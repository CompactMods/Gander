package dev.compactmods.gander.render.baked.model.material;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.ModelRebaker;
import dev.compactmods.gander.render.baked.model.DisplayableMesh;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MaterialBaker
{
    // TODO: add logging
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialBaker.class);

    private MaterialBaker() { }

    public static Set<MaterialInstance> getMaterialInstances(
        ModelBakery bakery, ModelResourceLocation key, UnbakedModel model)
    {
        switch (model)
        {
            case BlockModel block ->
            {
                return getBlockModelMaterials(key, block);
            }
            case MultiPart multiPart ->
            {
                return multiPart.getMultiVariants()
                    .stream()
                    .map(MultiVariant::getVariants)
                    .flatMap(List::stream)
                    .map(Variant::getModelLocation)
                    .map(bakery::getModel)
                    .map(it -> getMaterialInstances(bakery, key, it))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            }
            case MultiVariant multiVariant ->
            {
                return multiVariant.getVariants()
                    .stream()
                    .map(Variant::getModelLocation)
                    .map(bakery::getModel)
                    .map(it -> getMaterialInstances(bakery, key, it))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            }
            default -> throw new IllegalStateException("Unexpected value: " + model);
        }
    }

    private static Set<MaterialInstance> getBlockModelMaterials(ModelResourceLocation name, final BlockModel model)
    {
        var result = new HashSet<MaterialInstance>();
        Stream<Entry<String, Either<Material, String>>> materials = Stream.of();
        for (var mdl = model; mdl != null; mdl = mdl.parent)
        {
            materials = Stream.concat(materials, mdl.textureMap.entrySet().stream());
        }

        var fullMaterialMap = materials.collect(Collectors.toSet());

        var materialParents = ModelRebaker.getArchetypeMeshes(name)
            .stream()
            .map(DisplayableMesh::mesh)
            .map(BakedMesh::materials)
            .flatMap(List::stream)
            .collect(Collectors.toSet());

        var parentsByName = new HashMap<String, MaterialParent>();
        materialParents.forEach(material -> {
            parentsByName.put(material.name(), material);
        });

        fullMaterialMap.stream()
            .map(it -> Pair.of(it.getKey(), it.getValue().left().orElse(null)))
            .filter(it -> it.getSecond() != null)
            .forEach(material -> {
                parentsByName.putIfAbsent(material.getFirst(),
                    new MaterialParent(material.getFirst(),
                        material.getSecond().atlasLocation(),
                        material.getSecond().texture()));
            });

        var usedMaterials = fullMaterialMap.stream()
            .map(it -> it.getValue()
                .map(left -> Map.entry(it.getKey(),
                        checkMaterials(it.getKey(), parentsByName.getOrDefault(it.getKey(), MaterialParent.MISSING), left)),
                    ref -> Map.entry(it.getKey(), parentsByName.getOrDefault(ref, MaterialParent.MISSING))))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (var usedMaterial : usedMaterials)
        {
            var parent = parentsByName.get(usedMaterial.getKey());
            var resolved = usedMaterial.getValue();
            var overridenTexture = resolved.defaultValue();

            result.add(new MaterialInstance(parent, overridenTexture));
        }

        return result;
    }

    private static MaterialParent checkMaterials(
        String name, MaterialParent material, Material vanilla)
    {
        if (material == null)
        {
            return new MaterialParent(name, vanilla.atlasLocation(), vanilla.texture());
        }

        // We only care if the atlas matches, because if it doesn't that's a problem.
        Preconditions.checkArgument(vanilla.atlasLocation().equals(material.atlas()));
        return material;
    }
}
