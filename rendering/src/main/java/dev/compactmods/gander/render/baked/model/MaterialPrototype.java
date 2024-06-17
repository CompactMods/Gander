package dev.compactmods.gander.render.baked.model;

import net.minecraft.resources.ResourceLocation;

public record MaterialPrototype(
    String name,
    ResourceLocation atlas,
    ResourceLocation defaultValue)
{ }
