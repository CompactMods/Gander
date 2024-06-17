package dev.compactmods.gander.render.baked.texture;

import net.minecraft.resources.ResourceLocation;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * A small helper type for storing baked atlases and their sprite indexes.
 */
record AtlasIndices(
    FloatBuffer buffer,
    List<ResourceLocation> indexes)
{ }
