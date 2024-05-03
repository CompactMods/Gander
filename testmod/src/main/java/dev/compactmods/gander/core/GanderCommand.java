package dev.compactmods.gander.core;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.compactmods.gander.network.OpenUIPacket;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderCommand {
	public static final SuggestionProvider<CommandSourceStack> SCENES = (ctx, builder)
			-> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getStructureManager().listTemplates(), builder);

	public static LiteralArgumentBuilder<CommandSourceStack> make() {
		return Commands.literal("gander")
				.requires(cs -> cs.hasPermission(0))
				.then(Commands.argument("scene", ResourceLocationArgument.id())
						.suggests(SCENES)
						.executes(ctx -> openScene(ResourceLocationArgument.getId(ctx, "scene"), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("targets", EntityArgument.players())
								.requires(cs -> cs.hasPermission(2))
								.executes(ctx -> openScene(ResourceLocationArgument.getId(ctx, "scene"), EntityArgument.getPlayers(ctx, "targets")))
						)
				);

	}

	private static int openScene(ResourceLocation sceneId, ServerPlayer player) {
		return openScene(sceneId, ImmutableList.of(player));
	}

	private static int openScene(ResourceLocation sceneId, Collection<? extends ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (player instanceof FakePlayer)
				continue;

			PacketDistributor.sendToPlayer(player, new OpenUIPacket(sceneId));
		}

		return Command.SINGLE_SUCCESS;
	}
}
