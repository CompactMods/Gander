package dev.compactmods.gander.render.baked.model.obj;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import dev.compactmods.gander.render.baked.model.BakedMesh;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import dev.compactmods.gander.render.baked.model.ModelVertices;
import dev.compactmods.gander.render.baked.model.ModelVertices.ModelVertex;
import dev.compactmods.gander.render.mixin.accessor.ObjModel$ModelGroupAccessor;
import dev.compactmods.gander.render.mixin.accessor.ObjModel$ModelMeshAccessor;
import dev.compactmods.gander.render.mixin.accessor.ObjModel$ModelObjectAccessor;
import dev.compactmods.gander.render.mixin.accessor.ObjModelAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class ObjModelBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjModelBaker.class);

    private ObjModelBaker() { }

    public static Stream<ArchetypeComponent> bakeObjModel(
        ModelResourceLocation originalName,
        UnbakedModel model)
    {
        var accessor = (ObjModelAccessor)obj;

        return accessor.getParts()
            .entrySet()
            .stream()
            .flatMap(it -> {
                var name = it.getValue().name();
                if (name.isBlank()) name = it.getKey();

                return bakePart(
                    ArchetypeBaker.computeMeshName(originalName, "obj_part/" + name),
                    it.getValue(),
                    obj,
                    model);
            });
    }

    private static Stream<ArchetypeComponent> bakePart(
        ModelResourceLocation partName,
        ObjModel.ModelObject part,
        ObjModel model,
        BlockModel originalModel)
    {
        var accessor = (ObjModel$ModelObjectAccessor)part;

        var meshes = accessor.getMeshes();
        var result = IntStream.range(0, meshes.size())
            .mapToObj(it -> {
                var name = ArchetypeBaker.computeMeshName(
                    partName,
                    String.valueOf(it));
                var mesh = bakeMesh(name, meshes.get(it), accessor, model, originalModel);
                if (mesh != null)
                    return mesh;

                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Failed to bake part {} mesh index {}", partName, it);

                return null;
            })
            .filter(Objects::nonNull);

        if (part instanceof ObjModel.ModelGroup group)
            result = Stream.concat(result, bakeGroup(partName, group, model, originalModel));

        return result;
    }

    private static Stream<ArchetypeComponent> bakeGroup(
        ModelResourceLocation partName,
        ObjModel.ModelGroup group,
        ObjModel model,
        BlockModel originalModel)
    {
        var accessor = (ObjModel$ModelGroupAccessor)group;

        return accessor.getParts()
            .entrySet()
            .stream()
            .flatMap(it -> {
                var name = it.getValue().name();
                if (name.isBlank()) name = it.getKey();

                return bakePart(
                    ArchetypeBaker.computeMeshName(partName, name),
                    it.getValue(),
                    model,
                    originalModel);
            });
    }

    // TODO: almost identical logic is used in BlockModelBaker, this should be deduplicated.
    private static ArchetypeComponent bakeMesh(
        ModelResourceLocation meshName,
        ObjModel$ModelMeshAccessor mesh,
        ObjModel$ModelObjectAccessor object,
        ObjModel model,
        BlockModel originalModel)
    {
        var accessor = (ObjModelAccessor)model;

        var allFaces = mesh.getFaces()
            .stream()
            .map(face -> bakeFace(accessor, mesh, face))
            .map(ModelVertices::faces)
            .flatMap(Collection::stream)
            .toList();

        if (allFaces.isEmpty())
        {
            return null;
        }

        var deduplicatedVertices = allFaces.stream()
            .flatMap(Collection::stream)
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

        // Our faces are pre-triangulated, so in theory this should be enough
        var indexBuffer = ByteBuffer.allocate(allFaces.stream()
            .mapToInt(List::size)
            .sum());
        for (var face : allFaces)
        {
            // We're assuming CCW order, so if this is wrong then :oops:
            for (var vertex : face)
            {
                indexBuffer.put((byte)deduplicatedVertices.indexOf(vertex));
            }
        }

        var materials = Stream.concat(textureReferences.stream(), originalModel.textureMap.keySet().stream())
            .distinct()
            .map(it -> {
                // TODO: check if any parents contain textures?
                var mtlOrRef = originalModel.textureMap.getOrDefault(it, knownTextures.containsValue(it)
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

        var renderType = ArchetypeBaker.getRenderType(originalModel);

        return new ArchetypeComponent(
            meshName,
            new BakedMesh(
                deduplicatedVertices.size(),
                vertexBuffer.flip(),
                normalBuffer.flip(),
                uvBuffer.flip(),
                indexBuffer.flip(),
                materials, materialIndexes),
            renderType,
            originalModel.customData.isComponentVisible(object.getName(), true));
    }

    private static ModelVertices bakeFace(
        ObjModelAccessor model,
        ObjModel$ModelMeshAccessor mesh,
        int[][] indices)
    {
        // This code assumes that the vertices of the given face fall on a single
        // plane.

        var face = new ArrayList<ModelVertex>();
        // If any vertex in this face is missing a normal, we will need to
        // recalculate its normals. Following the assumption above, we assume
        // all normals will be equivalent, and so we ignore any manually
        // specified ones.
        // TODO: is this a valid assumption?
        var recalculateNormals = Arrays.stream(indices)
            .anyMatch(x -> x.length != 3);
        // Only allocate the recalculated normal if we need to.
        var recalculatedNormal = recalculateNormals
            ? new Vector3f(0, 0, 0)
            : null;

        for (var vertex : indices)
        {
            var position = model.getPositions().get(vertex[0]);
            var uv = model.getTexCoords().get(vertex[1]);
            var normal = recalculateNormals
                ? recalculatedNormal
                : model.getNormals().get(vertex[2]);

            face.add(new ModelVertex(
                position,
                normal,
                mesh.getMat().diffuseColorMap,
                new Vector2f(uv.x, model.getFlipV() ? 1 - uv.y : uv.y)));
        }

        if (recalculateNormals)
        {
            for (int i = 0; i < face.size(); i++)
            {
                var vertex = face.get(i);
                var nextVertex = face.get((i + 1) % face.size());

                recalculatedNormal.x += (vertex.position().y() - nextVertex.position().y()) * (vertex.position().z() + nextVertex.position().z());
                recalculatedNormal.y += (vertex.position().z() - nextVertex.position().z()) * (vertex.position().x() + nextVertex.position().x());
                recalculatedNormal.z += (vertex.position().x() - nextVertex.position().x()) * (vertex.position().y() + nextVertex.position().y());
            }

            recalculatedNormal.normalize();
        }

        return new ModelVertices(triangulate(face));
    }

    // TODO: implement an ear-cutting method for non-convex faces.
    private static List<List<ModelVertex>> triangulate(
        List<ModelVertex> originalFace)
    {
        if (isConvex(originalFace))
        {
            return triangulateFan(originalFace);
        }

        throw new IllegalStateException("Concave Faces Not Yet Implemented");
    }

    private static List<List<ModelVertex>> triangulateFan(
        List<ModelVertex> face)
    {
        var result = ImmutableList.<List<ModelVertex>>builder();
        for (int i = 1; i < face.size() - 1; i++)
        {
            var set = new ArrayList<ModelVertex>();
            set.add(face.getFirst());
            set.add(face.get(i));
            set.add(face.get(i + 1));

            result.add(Collections.unmodifiableList(set));
        }

        return result.build();
    }

    private static boolean isConvex(List<ModelVertex> vertices)
    {
        // If it's not a polygon, it's not convex.
        if (vertices.size() < 3)
            return false;

        // All triangles are convex.
        if (vertices.size() == 3)
            return true;

        var left = new Vector3f();
        var up = new Vector3f();

        var sign = 0;
        for (int i = 0; i < vertices.size(); i++)
        {
            var previous = vertices.get((vertices.size() + i - 1) % vertices.size());
            var current = vertices.get(i);
            var next = vertices.get((i + 1) % vertices.size());

            left.set(next.position())
                .sub(previous.position())
                .normalize();

            up.set(current.position())
                .sub(previous.position())
                .normalize();

            var cross = left.cross(up);

            // we assume the normal is accurate for the entire surface
            var newSign = Math.signum(cross.dot(current.normal()));

            // If the dot product is 0, it means that no "turn" occured
            if (newSign == 0)
                continue;
            // If the current sign is zero, it means we just started.
            else if (sign == 0)
                sign = (int)newSign;
            // If they mismatch, it means the direction changed and we're not convex
            else if (sign != (int)newSign)
                return false;
        }

        return true;
    }
}
