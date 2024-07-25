package dev.compactmods.gander.render.baked.model;

import dev.compactmods.gander.render.baked.model.material.MaterialParent;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

public record BakedMesh(
    int vertexCount,
    FloatBuffer vertices,
    FloatBuffer normals,
    FloatBuffer uvs,
    ByteBuffer indices,
    List<MaterialParent> materials,
    int[] materialIndexes)
{ }
