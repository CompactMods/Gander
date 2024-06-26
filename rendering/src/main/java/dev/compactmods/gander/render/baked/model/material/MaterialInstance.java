package dev.compactmods.gander.render.baked.model.material;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record MaterialInstance(
    MaterialParent material,
    @Nullable
    ResourceLocation overrideTexture)
{
    public ResourceLocation getEffectiveTexture()
    {
        return overrideTexture != null
            ? overrideTexture
            : material.defaultValue();
    }
}
