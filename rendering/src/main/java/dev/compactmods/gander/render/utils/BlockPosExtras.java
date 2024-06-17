package dev.compactmods.gander.render.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.SectionPos;

import java.util.stream.Stream;

public final class BlockPosExtras
{
    private BlockPosExtras() { }

    public static Stream<BlockPos> sectionFace(
        SectionPos pos,
        Direction face)
    {
        return switch (face)
        {
            case DOWN -> // XZ plane, Y = min
                BlockPos.betweenClosedStream(
                    pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(),
                    pos.maxBlockX(), pos.minBlockY(), pos.maxBlockZ());
            case UP -> // XZ plane, Y = max
                BlockPos.betweenClosedStream(
                    pos.minBlockX(), pos.maxBlockY(), pos.minBlockZ(),
                    pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ());
            case NORTH -> // XY plane, Z = min
                BlockPos.betweenClosedStream(
                    pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(),
                    pos.maxBlockX(), pos.maxBlockY(), pos.minBlockZ());
            case SOUTH -> // XY plane, Z = max
                BlockPos.betweenClosedStream(
                    pos.minBlockX(), pos.minBlockY(), pos.maxBlockZ(),
                    pos.maxBlockX(), pos.maxBlockY(), pos.maxBlockZ());
            case WEST -> // YZ plane, X = min
                BlockPos.betweenClosedStream(
                    pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(),
                    pos.minBlockX(), pos.maxBlockY(), pos.maxBlockZ());
            case EAST -> // YZ plane, X = max
                BlockPos.betweenClosedStream(
                    pos.maxBlockX(), pos.minBlockY(), pos.minBlockZ(),
                    pos.maxBlockX(), pos.maxBlockY(), pos.minBlockZ());
        };
    }
}
