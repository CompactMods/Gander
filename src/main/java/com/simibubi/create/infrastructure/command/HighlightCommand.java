package com.simibubi.create.infrastructure.command;

import java.util.Collection;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.AllPackets;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
}
