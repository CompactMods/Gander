package dev.compactmods.gander.render.baked.model.block;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import dev.compactmods.gander.render.baked.model.block.ModelUvs.ModelUv;
import dev.compactmods.gander.render.baked.model.block.ModelUvs.UvIndex;
import dev.compactmods.gander.render.baked.model.ModelVertices;
import dev.compactmods.gander.render.baked.model.ModelVertices.ModelVertex;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.compactmods.gander.render.baked.model.RotationUtil.rotate;

public final class BlockModelBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockModelBaker.class);

    private BlockModelBaker() { }

    // TODO: almost identical logic is used in ObjModelBaker, this should be deduplicated.
    public static Map<ModelResourceLocation, BakedMesh> bakeBlockModel(
        ModelResourceLocation originalName,
        BlockModel model)
    {
        var name = ArchetypeBaker.computeMeshName(originalName);
        var allFaces = model.getElements().stream()
            .map(BlockModelBaker::bakeElement)
            .reduce(ModelVertices::combine)
            .stream()
            .map(ModelVertices::faces)
            .flatMap(List::stream)
            .toList();

        if (allFaces.isEmpty())
        {
            return null;
        }

        var deduplicatedVertices = allFaces.stream()
            .flatMap(List::stream)
            .distinct()
            .toList();

        var textureReferences = new ArrayList<String>();
        var knownTextures = HashBiMap.<ResourceLocation, String>create();
        var vertexBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 3);
        var normalBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 3);
        var uvBuffer = FloatBuffer.allocate(deduplicatedVertices.size() * 2);
        for (var vertex : deduplicatedVertices)
        {
            vertexBuffer.put(vertex.position().x());
            vertexBuffer.put(vertex.position().y());
            vertexBuffer.put(vertex.position().z());
            normalBuffer.put(vertex.normal().x());
            normalBuffer.put(vertex.normal().y());
            normalBuffer.put(vertex.normal().z());
            uvBuffer.put(vertex.uv().x());
            uvBuffer.put(vertex.uv().y());

            textureReferences.add(vertex.textureReference().startsWith("#")
                ? vertex.textureReference().substring(1)
                : knownTextures.computeIfAbsent(
                    ResourceLocation.parse(vertex.textureReference()),
                    unused -> "gander_generated_" + knownTextures.size()));
        }

        // There are two triangles per face, so we need to double this.
        var indexBuffer = ByteBuffer.allocate(allFaces.stream().mapToInt(List::size).sum() * 2);
        var tempBuffer = new ModelVertex[4];
        for (var face : allFaces)
        {
            // Face vertices built by computeVertices are in this order:
            // 0---1
            // |   |
            // 2---3
            // Since we're in CCW normal order, this means this order:
            // 0-<-1 1
            // |  / /|
            // v ^ v ^
            // |/ /  |
            // 2 2->-3
            // So we want the vertices in this order:
            // 021
            // 312
            var vertices = face.toArray(tempBuffer);
            if (vertices.length != 4)
                LOGGER.error("Face contains more than 4 vertices. This needs to be implemented.");

            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[0]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[2]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[1]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[3]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[1]));
            indexBuffer.put((byte)deduplicatedVertices.indexOf(vertices[2]));
        }

        var materials = Stream.concat(textureReferences.stream(), model.textureMap.keySet().stream())
            .distinct()
            .map(it -> {
                // TODO: check if any parents contain textures?
                var mtlOrRef = model.textureMap.getOrDefault(it, knownTextures.containsValue(it)
                    ? Either.left(new net.minecraft.client.resources.model.Material(
                    TextureAtlas.LOCATION_BLOCKS,
                    knownTextures.inverse().get(it)))
                    : Either.right(it));
                return new MaterialParent(it,
                    mtlOrRef.map(
                        net.minecraft.client.resources.model.Material::atlasLocation,
                        ref -> TextureAtlas.LOCATION_BLOCKS),
                    mtlOrRef.map(
                        net.minecraft.client.resources.model.Material::texture,
                        ref -> null));
            })
            .toList();

        var materialIndexes = new int[deduplicatedVertices.size()];
        int i = 0;
        for (var reference : textureReferences)
        {
            var materialByName = materials.stream()
                .filter(x -> reference.equals(x.name()))
                .findFirst()
                .orElseThrow();

            materialIndexes[i++] = materials.indexOf(materialByName);
        }

        return Map.of(name,
            new BakedMesh(
                deduplicatedVertices.size(),
                vertexBuffer.flip(),
                normalBuffer.flip(),
                uvBuffer.flip(),
                indexBuffer.flip(),
                materials, materialIndexes));
    }

    private static ModelVertices bakeElement(BlockElement element)
    {
        var rotation = computeRotation(element.rotation);
        var normals = computeNormals(element, rotation);
        var uvs = computeUvs(element);
        return computeVertices(element, rotation, normals, uvs);
    }

    private static final Quaternionfc NO_ROTATION = new Quaternionf();
    private static Quaternionfc computeRotation(@Nullable BlockElementRotation rotation)
    {
        // If there was no rotation specified, we don't want to do any maths.
        if (rotation == null) return NO_ROTATION;

        var axis = new Vector3f();
        switch (rotation.axis()) {
            case X -> axis.set(1, 0, 0);
            case Y -> axis.set(0, 1, 0);
            case Z -> axis.set(0, 0, 1);
        };
        return new Quaternionf().rotationAxis(
            (float)Math.toRadians(rotation.angle()),
            axis);
    }

    private static ModelNormals computeNormals(BlockElement element, Quaternionfc rotation)
    {
        var down = new Vector3f(Direction.DOWN.getNormal().getX(), Direction.DOWN.getNormal().getY(), Direction.DOWN.getNormal().getZ()); // -Y
        var up = new Vector3f(Direction.UP.getNormal().getX(), Direction.UP.getNormal().getY(), Direction.UP.getNormal().getZ()); // +Y
        var north = new Vector3f(Direction.NORTH.getNormal().getX(), Direction.NORTH.getNormal().getY(), Direction.NORTH.getNormal().getZ()); // -Z
        var south = new Vector3f(Direction.SOUTH.getNormal().getX(), Direction.SOUTH.getNormal().getY(), Direction.SOUTH.getNormal().getZ()); // +Z
        var west = new Vector3f(Direction.WEST.getNormal().getX(), Direction.WEST.getNormal().getY(), Direction.WEST.getNormal().getZ()); // -X
        var east = new Vector3f(Direction.EAST.getNormal().getX(), Direction.EAST.getNormal().getY(), Direction.EAST.getNormal().getZ()); // +X

        rotate(down, element.rotation, rotation);
        rotate(up, element.rotation, rotation);
        rotate(north, element.rotation, rotation);
        rotate(south, element.rotation, rotation);
        rotate(west, element.rotation, rotation);
        rotate(east, element.rotation, rotation);

        return new ModelNormals(
            down.normalize(), up.normalize(),
            north.normalize(), south.normalize(),
            west.normalize(), east.normalize());
    }

    private static ModelUvs computeUvs(BlockElement element)
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

        // No, I don't know what they're doing here either.
        var topLeft = new Vector2f(face.uv().getU(0), face.uv().getV(0))
            .div(16);
        var topRight = new Vector2f(face.uv().getU(3), face.uv().getV(3))
            .div(16);
        var bottomLeft = new Vector2f(face.uv().getU(1), face.uv().getV(1))
            .div(16);
        var bottomRight = new Vector2f(face.uv().getU(2), face.uv().getV(2))
            .div(16);

        return new ModelUv(
            topLeft, topRight,
            bottomLeft, bottomRight);
    }

    private static ModelVertices computeVertices(
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
            element.from.min(element.to, backLowerLeft)
                .div(16);
            element.from.max(element.to, frontUpperRight)
                .div(16);

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

        var faces = ImmutableList.<List<ModelVertex>>builder();
        for (var direction : Direction.values())
        {
            var faceElement = element.faces.get(direction);
            if (faceElement == null) continue;

            var face = new ArrayList<ModelVertex>();
            forFace(face, direction,
                faceElement, uvs, normals,
                backLowerLeft, frontUpperRight, backLowerRight, frontLowerRight,
                frontLowerLeft, frontUpperLeft, backUpperLeft, backUpperRight);
            faces.add(Collections.unmodifiableList(face));
        }

        return new ModelVertices(faces.build());
    }

    private static void forFace(
        List<ModelVertex> result, Direction direction,
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
        List<ModelVertex> result,
        BlockElementFace face,
        Vector3fc position,
        Vector3fc normal,
        ModelUv uv,
        UvIndex index)
    {
        var data = face.faceData();
        result.add(new ModelVertex(
            position,
            normal,
            face.texture(),
            switch (index)
            {
                case TopLeft -> uv.topLeft();
                case TopRight -> uv.topRight();
                case BottomLeft -> uv.bottomLeft();
                case BottomRight -> uv.bottomRight();
            }));/*,
            face.tintIndex,
            data.color(),
            data.blockLight(), data.skyLight(),
            data.ambientOcclusion()));*/
    }
}
