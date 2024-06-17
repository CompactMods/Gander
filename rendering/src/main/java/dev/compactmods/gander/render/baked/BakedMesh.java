package dev.compactmods.gander.render.baked;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public record BakedMesh(
    FloatBuffer vertices,
    FloatBuffer normals,
    FloatBuffer uvs,
    ByteBuffer indices)
{ }
