package dev.compactmods.gander.render.baked.model;

import net.minecraft.resources.ResourceLocation;

public record MaterialInstance(
    String name,
    ResourceLocation overrideTexture)
{ }
