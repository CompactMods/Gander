package com.simibubi.create.infrastructure.command;

import java.util.Collection;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;

import com.simibubi.create.CreateClient;
import com.simibubi.create.networking.SimplePacketBase;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class HighlightCommand {

	public static ArgumentBuilder<CommandSourceStack, ?> register() {
		return Commands.literal("highlight")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> {
									Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
									BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");

									for (ServerPlayer p : players) {
										AllPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> p), new HighlightPacket(pos));
									}

									return players.size();
								}))
						// .requires(AllCommands.sourceIsPlayer)
						.executes(ctx -> {
							BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");

							AllPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) ctx.getSource()
									.getEntity()), new HighlightPacket(pos));

							return Command.SINGLE_SUCCESS;
						}));

	}

	public static class HighlightPacket extends SimplePacketBase {

		private final BlockPos pos;

		public HighlightPacket(BlockPos pos) {
			this.pos = pos;
		}

		public HighlightPacket(FriendlyByteBuf buffer) {
			this.pos = buffer.readBlockPos();
		}

		@Override
		public void write(FriendlyByteBuf buffer) {
			buffer.writeBlockPos(pos);
		}

		@Override
		public boolean handle(NetworkEvent.Context context) {
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				performHighlight(pos);
			}));
			return true;
		}

		@OnlyIn(Dist.CLIENT)
		public static void performHighlight(BlockPos pos) {
			if (Minecraft.getInstance().level == null || !Minecraft.getInstance().level.isLoaded(pos))
				return;

			CreateClient.OUTLINER.showAABB("highlightCommand", Shapes.block()
							.bounds()
							.move(pos), 200)
					.lineWidth(1 / 32f)
					.colored(0xEeEeEe);
		}

	}

}
