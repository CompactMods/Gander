package dev.compactmods.gander.render.baked.model.block;

import org.joml.Vector3fc;

/**
 * A small helper type containing model normals.
 */
public record ModelNormals(
    Vector3fc down,
    Vector3fc up,
    Vector3fc north,
    Vector3fc south,
    Vector3fc west,
    Vector3fc east)
{

}
