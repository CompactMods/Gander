package dev.compactmods.gander.render.baked.model.material;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public record MaterialParent(
    String name,
    ResourceLocation atlas,
    ResourceLocation defaultValue)
{
    public static final MaterialParent MISSING
        = new MaterialParent(
            "missingno",
            TextureAtlas.LOCATION_BLOCKS,
            MissingTextureAtlasSprite.getLocation());
}
