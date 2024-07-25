package dev.compactmods.gander.runtime.mixin.accessor;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(BlockModelShaper.class)
public interface BlockModelShaperAccessor
{
    @Accessor
    Map<BlockState, BakedModel> getModelByStateCache();
}
