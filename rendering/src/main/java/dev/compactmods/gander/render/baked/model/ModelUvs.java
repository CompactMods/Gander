package dev.compactmods.gander.render.baked.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2fc;

/**
 * A small helper type containing model UVs.
 * At least one of these is not null, but there's no good way to encode that.
 */
record ModelUvs(
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
        Vector2fc bottomRight,
        String textureReference) { }

    public static ModelUvs compute(BlockElement element)
    {
        return new ModelUvs(
            getUv(element.faces.get(Direction.DOWN)),
            getUv(element.faces.get(Direction.UP)),
            getUv(element.faces.get(Direction.NORTH)),
            getUv(element.faces.get(Direction.SOUTH)),
            getUv(element.faces.get(Direction.WEST)),
            getUv(element.faces.get(Direction.EAST)));
    }

    private static ModelUv getUv(BlockElementFace face)
    {
        if (face == null) return null;

        // TODO: determine if these indices are CW/CCW or zigzag

        // No, I don't know what they're doing here either.
        var topLeft = new Vector2f(face.uv.getU(0), face.uv.getV(0));
        var topRight = new Vector2f(face.uv.getU(3), face.uv.getV(3));
        var bottomLeft = new Vector2f(face.uv.getU(1), face.uv.getV(1));
        var bottomRight = new Vector2f(face.uv.getU(2), face.uv.getV(2));

        return new ModelUv(
            topLeft, topRight,
            bottomLeft, bottomRight,
            face.texture);
    }
}