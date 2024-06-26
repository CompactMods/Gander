package dev.compactmods.gander.render.baked.model;

import com.google.common.collect.ImmutableList;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.List;

/**
 * A small helper type containing model vertices.
 * Graphics libraries don't support multi-indexing, so vertices need to be
 * duplicated for each normal/uv combination. These can then be deduplicated,
 * in the case of inefficient models being uploaded.
 */
public record ModelVertices(
    List<List<ModelVertex>> faces)
{
    /**
     * A given model vertex.
     */
     public record ModelVertex(
         Vector3fc position,
         Vector3fc normal,
         String textureReference,
         Vector2fc uv)/*,
         int tintIndex,
         int colorArgb,
         int blockLight, int skyLight,
         boolean ambientOcclusion)*/
    { }

    public static ModelVertices combine(
        ModelVertices left,
        ModelVertices right)
    {
        var combinedFaces = ImmutableList.<List<ModelVertex>>builderWithExpectedSize(
            left.faces().size() + right.faces().size());

        combinedFaces.addAll(left.faces());
        combinedFaces.addAll(right.faces());

        return new ModelVertices(
            combinedFaces.build());
    }
}
