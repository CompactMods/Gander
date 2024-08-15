package dev.compactmods.gander.render.baked.model.material;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record MaterialInstance(
    String name,
    MaterialParent parent,
    @Nullable
    ResourceLocation overrideTexture)
{
    public static final MaterialInstance MISSING
        = new MaterialInstance(
            MaterialParent.MISSING.name(),
            MaterialParent.MISSING,
            null);

    public ResourceLocation getEffectiveTexture()
    {
        return overrideTexture != null
            ? overrideTexture
            : parent.defaultValue();
    }
}
