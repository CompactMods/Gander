package dev.compactmods.gander.render.baked.model.block;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2fc;

/**
 * A small helper type containing model UVs.
 * At least one of these is not null, but there's no good way to encode that.
 */
public record ModelUvs(
    @Nullable ModelUv down,
    @Nullable ModelUv up,
    @Nullable ModelUv north,
    @Nullable ModelUv south,
    @Nullable ModelUv west,
    @Nullable ModelUv east)
{
    public enum UvIndex
    {
        TopLeft,
        TopRight,
        BottomLeft,
        BottomRight
    }

    /**
     * The modelUv for a given face.
     */
    public record ModelUv(
        Vector2fc topLeft,
        Vector2fc topRight,
        Vector2fc bottomLeft,
        Vector2fc bottomRight) { }
}