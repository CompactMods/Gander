package dev.compactmods.gander.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import com.mojang.brigadier.context.CommandContext;

import dev.compactmods.gander.core.math.WorldMath;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class GanderTestCommand {

    public static void addTestSubtree(CommandBuildContext buildContext, LiteralArgumentBuilder<CommandSourceStack> root) {
        // --- PLATFORM: Generate a test platform with a command block for viewing
        var platform = Commands.literal("platform");
        var size = Commands.argument("radius", IntegerArgumentType.integer(5, 15))
            .executes(GanderTestCommand::makeSizedPlatform);

        size.then(Commands.argument("block", BlockStateArgument.block(buildContext))
            .executes(GanderTestCommand::makeSizedPlatformWithBorderBlock));
        platform.then(size);

        final var testCmdRoot = Commands.literal("test");
        testCmdRoot
            .then(platform);

        root.then(testCmdRoot);
    }

    private static int makeSizedPlatformWithBorderBlock(CommandContext<CommandSourceStack> ctx) {
        final var src = ctx.getSource();
        final var level = src.getLevel();

        final var radius = IntegerArgumentType.getInteger(ctx, "radius");
        final var ringBlock = BlockStateArgument.getBlock(ctx, "block").getState();

        final var center = BlockPos.containing(src.getPosition()).below();
        final var minCorner = center.offset(-radius, 0, -radius);
        final var maxCorner = center.offset(radius, 0, radius);

        generateTestPlatform(minCorner, maxCorner, level, center, radius, ringBlock);

        return 0;
    }

    private static int makeSizedPlatform(CommandContext<CommandSourceStack> ctx) {
        final var src = ctx.getSource();
        final var level = src.getLevel();

        final var center = BlockPos.containing(src.getPosition()).below();
        final var radius = IntegerArgumentType.getInteger(ctx, "radius");
        final var minCorner = center.offset(-radius, 0, -radius);
        final var maxCorner = center.offset(radius, 0, radius);

        generateTestPlatform(minCorner, maxCorner, level, center, radius, Blocks.WHITE_CONCRETE.defaultBlockState());

        return 0;
    }

    private static void generateTestPlatform(BlockPos minCorner, BlockPos maxCorner, ServerLevel level, BlockPos center, int radius, BlockState ringBlock) {
        BlockPos.betweenClosed(minCorner, maxCorner)
            .forEach(pos -> level.setBlock(pos, Blocks.BLACK_STAINED_GLASS.defaultBlockState(), Block.UPDATE_ALL));

        WorldMath.blockPosRing(center, radius)
            .map(BlockPos::immutable)
            .forEach(pos -> level.setBlock(pos, ringBlock, Block.UPDATE_ALL));

        level.setBlock(center.above(), Blocks.OAK_BUTTON.defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.FLOOR), Block.UPDATE_ALL);

        level.setBlock(center, Blocks.COMMAND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        StringBuilder command = new StringBuilder();
        command.append("gander screen area ")
            .append(minCorner.getX()).append(" ")
            .append(minCorner.getY()).append(" ")
            .append(minCorner.getZ()).append(" ")
            .append(maxCorner.getX()).append(" ")
            .append(maxCorner.getY() + radius).append(" ")
            .append(maxCorner.getZ());

        if (level.getBlockEntity(center) instanceof CommandBlockEntity cbe) {
            cbe.getCommandBlock().setCommand(command.toString());
        }
    }
}
