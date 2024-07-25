package dev.compactmods.gander.runtime.mixin.accessor;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.AtlasSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AtlasSet.StitchResult.class)
public interface StitchResultAccessor
{
    @Accessor
    SpriteLoader.Preparations getPreparations();
}
