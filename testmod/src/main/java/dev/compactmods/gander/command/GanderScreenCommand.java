package dev.compactmods.gander.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.compactmods.gander.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander.network.OpenGanderUiForStructureRequest;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderScreenCommand {

    public static final SuggestionProvider<CommandSourceStack> ANY_STRUCTURE = (ctx, builder)
        -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getStructureManager().listTemplates(), builder);

    public static void addScreenSubtree(CommandBuildContext buildContext, LiteralArgumentBuilder<CommandSourceStack> root) {
        var scene = Commands.literal("scene")
            .then(
                Commands.argument("scene", ResourceLocationArgument.id())
                    .suggests(ANY_STRUCTURE)
                    .executes(GanderScreenCommand::openTemplateScene)
            );

        var structure = Commands.literal("structure")
            .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                .executes(ctx -> openStructureSceneWithFloor(ctx, Blocks.AIR.defaultBlockState()))
                .then(Commands.argument("floor", BlockStateArgument.block(buildContext))
                    .executes(GanderScreenCommand::openStructureScene)
                )
            );

        var debug = Commands.literal("debug")
            .executes(GanderScreenCommand::generateDebug);

        final var renderToScreenRoot = Commands.literal("screen");

        renderToScreenRoot
            .then(scene)
            .then(structure)
            .then(debug);

        root.then(renderToScreenRoot);
    }

    private static int openTemplateScene(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sceneId = ResourceLocationArgument.getId(ctx, "scene");
        var player = ctx.getSource().getPlayerOrException();

        if (player instanceof FakePlayer)
            return -1;

        PacketDistributor.sendToPlayer(player, new OpenGanderUiForDeferredStructureRequest(sceneId));
        return Command.SINGLE_SUCCESS;
    }

    private static int openStructureScene(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var floor = BlockStateArgument.getBlock(ctx, "floor").getState();
        return openStructureSceneWithFloor(ctx, floor);
    }

    private static int openStructureSceneWithFloor(CommandContext<CommandSourceStack> ctx, BlockState floor) throws CommandSyntaxException {
        var structure = ResourceKeyArgument.getStructure(ctx, "structure");
        var player = ctx.getSource().getPlayerOrException();

        if (player instanceof FakePlayer)
            return -1;

        var seed = RandomSource.create().nextLong();

        final var finalStructure = GanderCommandHelper.generateStructureWithFloor(ctx, floor, structure, seed);
        if (finalStructure == null) return -1;

        final var title = Component.literal("Generated: %s, Seed: %d".formatted(structure.key().location(), seed));

        player.displayClientMessage(Component.literal("Seed: %d".formatted(seed))
            .withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Copy Seed to Clipboard")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, seed + ""))
            ), false);

        PacketDistributor.sendToPlayer(player, new OpenGanderUiForStructureRequest(title, finalStructure));
        return Command.SINGLE_SUCCESS;
    }

    private static int generateDebug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();

        if (player instanceof FakePlayer)
            return -1;

        var structure = GanderCommandHelper.buildDebugStructure(ctx);

        PacketDistributor.sendToPlayer(player, new OpenGanderUiForStructureRequest(Component.literal("Generated: minecraft:debug"), structure));
        return Command.SINGLE_SUCCESS;
    }
}
