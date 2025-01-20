package dev.compactmods.gander.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderScreenCommand {

    public static final SuggestionProvider<CommandSourceStack> ANY_STRUCTURE = (ctx, builder)
        -> SharedSuggestionProvider.suggestResource(ctx.getSource().getServer().getStructureManager().listTemplates(), builder);

    public static void addScreenSubtree(CommandBuildContext buildContext, LiteralArgumentBuilder<CommandSourceStack> root) {
        // --- SCENE: For Existing Templates ---
        var scene = Commands.literal("scene")
            .then(
                Commands.argument("scene", ResourceLocationArgument.id())
                    .suggests(ANY_STRUCTURE)
                    .executes(GanderScreenCommand::openTemplateScene)
            );

        // --- STRUCTURE: Generated Structures (Jigsaw) ---
        var structure = Commands.literal("structure")
            .then(Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                .executes(ctx -> openStructureSceneWithFloor(ctx, Blocks.AIR.defaultBlockState()))
                .then(Commands.argument("floor", BlockStateArgument.block(buildContext))
                    .executes(GanderScreenCommand::openStructureScene)
                )
            );

        // --- DEBUG: EVERY BLOCK EVER ---
        var debug = Commands.literal("debug")
            .executes(GanderScreenCommand::generateDebug);

        // --- NEARBY (A Player) ---
        var nearby = Commands.literal("nearby")
            .then(Commands.argument("distance", IntegerArgumentType.integer(3, 32))
                .executes(GanderScreenCommand::nearbyBlocks));

        final var renderToScreenRoot = Commands.literal("screen");
        renderToScreenRoot
            .then(scene)
            .then(structure)
            .then(debug)
            .then(nearby);

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

    private static int nearbyBlocks(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        var distance = IntegerArgumentType.getInteger(ctx, "distance");
        var source = ctx.getSource();

        StructureTemplate finalStructure = new StructureTemplate();
        finalStructure.fillFromWorld(source.getLevel(),
            BlockPos.containing(source.getPosition().subtract(distance, distance, distance)),
            new Vec3i(distance * 2, distance * 2, distance * 2),
            false, null);

        final var nearbyPlayers = source.getLevel()
                .getPlayers(player -> player.position()
                    .closerThan(source.getPosition(), 5, 5));

        for(var nearby : nearbyPlayers) {
            PacketDistributor.sendToPlayer(nearby, new OpenGanderUiForStructureRequest(Component.literal("Nearby: " + distance + " blocks"), finalStructure));
        }

        return 0;
    }
}
