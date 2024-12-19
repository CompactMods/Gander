package dev.compactmods.gander.level.block;

import dev.compactmods.gander.level.light.VirtualLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.lighting.LevelLightEngine;

import org.jetbrains.annotations.NotNull;

public class VirtualBlockAndTintGetter extends VirtualBlockGetter implements BlockAndTintGetter
{
    private final LevelLightEngine lightEngine;

    private final Biome PLAINS;

    public VirtualBlockAndTintGetter(
        LevelHeightAccessor heightAccessor,
        VirtualBlockAndFluidStorage blockAndFluidStorage,
        LevelLightEngine lightEngine)
    {

        super(heightAccessor, blockAndFluidStorage);
        this.lightEngine = lightEngine;
        this.PLAINS = Minecraft.getInstance().level.registryAccess()
            .lookupOrThrow(Registries.BIOME)
            .getValue(Biomes.PLAINS);
    }

    @Override
    public int getBrightness(LightLayer pLightType, BlockPos pBlockPos)
    {
        return BlockAndTintGetter.super.getBrightness(pLightType, pBlockPos);
    }

    @Override
    public int getRawBrightness(BlockPos pBlockPos, int pAmount)
    {
        return BlockAndTintGetter.super.getRawBrightness(pBlockPos, pAmount);
    }

    @Override
    public boolean canSeeSky(BlockPos pBlockPos)
    {
        return BlockAndTintGetter.super.canSeeSky(pBlockPos);
    }

    @Override
    public float getShade(Direction pDirection, boolean pShade)
    {
        return 1f;
    }

    @Override
    public @NotNull LevelLightEngine getLightEngine()
    {
        return this.lightEngine;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colors)
    {
        return colors.getColor(PLAINS, pos.getX(), pos.getZ());
    }

}
