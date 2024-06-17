package dev.compactmods.gander.render.baked.model;

import com.google.common.collect.ImmutableList;
import dev.compactmods.gander.render.baked.model.ModelUvs.ModelUv;
import dev.compactmods.gander.render.baked.model.ModelUvs.UvIndex;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

import static dev.compactmods.gander.render.baked.model.RotationUtil.rotate;

/**
 * A small helper type containing model vertices.
 * Graphics libraries don't support multi-indexing, so vertices need to be
 * duplicated for each normal/uv combination. These can then be deduplicated,
 * in the case of inefficient models being uploaded.
 */
record ModelVertices(
    List<SequencedSet<ModelVertex>> faces)
{
    /**
     * A given model vertex.
     */
     public record ModelVertex(
         Vector3fc position,
         Vector3fc normal,
         ModelUv modelUv, UvIndex uvIndex)/*,
         int tintIndex,
         int colorArgb,
         int blockLight, int skyLight,
         boolean ambientOcclusion)*/
    {
        public Vector2fc uv()
        {
            return switch (uvIndex)
            {
                case TopLeft -> modelUv().topLeft();
                case TopRight -> modelUv().topRight();
                case BottomLeft -> modelUv().bottomLeft();
                case BottomRight -> modelUv().bottomRight();
            };
        }
    }

    public static ModelVertices combine(
        ModelVertices left,
        ModelVertices right)
    {
        var result = ImmutableList.<SequencedSet<ModelVertex>>builderWithExpectedSize(
            left.faces().size() + right.faces().size());

        result.addAll(left.faces());
        result.addAll(right.faces());

        return new ModelVertices(result.build());
    }

    public static ModelVertices compute(
        BlockElement element,
        Quaternionfc rotation,
        ModelNormals normals,
        ModelUvs uvs)
    {
        // All eight vertices of the cube we're baking
        var backLowerLeft = new Vector3f(); // (0) (0, 0, 0)
        var frontUpperRight = new Vector3f(); // (1) (+1, +1, +1)

        var backLowerRight = new Vector3f(); // (2) (+1, 0, 0)
        var frontLowerRight = new Vector3f(); // (3) (+1, 0, +1)
        var frontLowerLeft = new Vector3f(); // (4) (0, 0, +1)

        var frontUpperLeft = new Vector3f(); // (5) (0, +1, +1)
        var backUpperLeft = new Vector3f(); // (6) (0, +1, 0)
        var backUpperRight = new Vector3f(); // (7) (+1, +1, 0)

        // Set all eight coordinates to the correct position
        {
            element.from.min(element.to, backLowerLeft);
            element.from.max(element.to, frontUpperRight);

            backLowerRight.set(frontUpperRight.x, backLowerLeft.y, backLowerLeft.z);
            backUpperLeft.set(backLowerLeft.x, frontUpperRight.y, backLowerLeft.z);
            frontLowerLeft.set(backLowerLeft.x, backLowerLeft.y, frontUpperRight.z);

            frontUpperLeft.set(backLowerLeft.x, frontUpperRight.y, frontUpperRight.z);
            frontLowerRight.set(frontUpperRight.x, backLowerLeft.y, frontUpperRight.z);
            backUpperRight.set(frontUpperRight.x, frontUpperRight.y, backLowerLeft.z);
        }

        // Rotate them based on the rotation in the model.
        {
            rotate(backLowerLeft, element.rotation, rotation);
            rotate(frontUpperRight, element.rotation, rotation);

            rotate(backLowerRight, element.rotation, rotation);
            rotate(backUpperLeft, element.rotation, rotation);
            rotate(frontLowerLeft, element.rotation, rotation);

            rotate(frontUpperLeft, element.rotation, rotation);
            rotate(frontLowerRight, element.rotation, rotation);
            rotate(backUpperRight, element.rotation, rotation);
        }

        var result = ImmutableList.<SequencedSet<ModelVertex>>builder();
        for (var direction : Direction.values())
        {
            var faceElement = element.faces.get(direction);
            if (faceElement == null) continue;

            var face = new LinkedHashSet<ModelVertex>();
            forFace(face, direction,
                faceElement, uvs, normals,
                backLowerLeft, frontUpperRight, backLowerRight, frontLowerRight,
                frontLowerLeft, frontUpperLeft, backUpperLeft, backUpperRight);
            result.add(Collections.unmodifiableSequencedSet(face));
        }

        return new ModelVertices(result.build());
    }

    private static void forFace(
        LinkedHashSet<ModelVertex> result, Direction direction,
        BlockElementFace face, ModelUvs uvs, ModelNormals normals,
        Vector3fc backLowerLeft, Vector3fc frontUpperRight, Vector3fc backLowerRight, Vector3fc frontLowerRight,
        Vector3fc frontLowerLeft, Vector3fc frontUpperLeft, Vector3fc backUpperLeft, Vector3fc backUpperRight)
    {
        // Right handed coordinates, -Z is *away* from the camera
        switch (direction)
        {
            case DOWN:
                // front is up, left and right are normal
                forUv(result, face, frontLowerLeft, normals.down(), uvs.down(), UvIndex.TopLeft);
                forUv(result, face, frontLowerRight, normals.down(), uvs.down(), UvIndex.TopRight);
                forUv(result, face, backLowerLeft, normals.down(), uvs.down(), UvIndex.BottomLeft);
                forUv(result, face, backLowerRight, normals.down(), uvs.down(), UvIndex.BottomRight);
                break;
            case UP:
                // back is up, left and right are normal
                forUv(result, face, backUpperLeft, normals.up(), uvs.up(), UvIndex.TopLeft);
                forUv(result, face, backUpperRight, normals.up(), uvs.up(), UvIndex.TopRight);
                forUv(result, face, frontUpperLeft, normals.up(), uvs.up(), UvIndex.BottomLeft);
                forUv(result, face, frontUpperRight, normals.up(), uvs.up(), UvIndex.BottomRight);
                break;
            case NORTH:
                // up is up, left and right are swapped
                forUv(result, face, backUpperRight, normals.north(), uvs.north(), UvIndex.TopLeft);
                forUv(result, face, backUpperLeft, normals.north(), uvs.north(), UvIndex.TopRight);
                forUv(result, face, backLowerRight, normals.north(), uvs.north(), UvIndex.BottomLeft);
                forUv(result, face, backLowerLeft, normals.north(), uvs.north(), UvIndex.BottomRight);
                break;
            case SOUTH:
                // up is up, left and right are normal
                forUv(result, face, frontUpperLeft, normals.south(), uvs.south(), UvIndex.TopLeft);
                forUv(result, face, frontUpperRight, normals.south(), uvs.south(), UvIndex.TopRight);
                forUv(result, face, frontLowerLeft, normals.south(), uvs.south(), UvIndex.BottomLeft);
                forUv(result, face, frontLowerRight, normals.south(), uvs.south(), UvIndex.BottomRight);
                break;
            case WEST:
                // up is up, back is left and front is right
                forUv(result, face, backUpperLeft, normals.west(), uvs.west(), UvIndex.TopLeft);
                forUv(result, face, frontUpperLeft, normals.west(), uvs.west(), UvIndex.TopRight);
                forUv(result, face, backLowerLeft, normals.west(), uvs.west(), UvIndex.BottomLeft);
                forUv(result, face, frontLowerLeft, normals.west(), uvs.west(), UvIndex.BottomRight);
                break;
            case EAST:
                // up is up, front is left and back is right
                forUv(result, face, frontUpperRight, normals.east(), uvs.east(), UvIndex.TopLeft);
                forUv(result, face, backUpperRight, normals.east(), uvs.east(), UvIndex.TopRight);
                forUv(result, face, frontLowerRight, normals.east(), uvs.east(), UvIndex.BottomLeft);
                forUv(result, face, backLowerRight, normals.east(), uvs.east(), UvIndex.BottomRight);
                break;
        }
    }

    private static void forUv(
        LinkedHashSet<ModelVertex> result,
        BlockElementFace face,
        Vector3fc position,
        Vector3fc normal,
        ModelUv uv,
        UvIndex index)
    {
        var data = face.getFaceData();
        result.add(new ModelVertex(
            position,
            normal,
            uv, index));/*,
            face.tintIndex,
            data.color(),
            data.blockLight(), data.skyLight(),
            data.ambientOcclusion()));*/
    }
}
