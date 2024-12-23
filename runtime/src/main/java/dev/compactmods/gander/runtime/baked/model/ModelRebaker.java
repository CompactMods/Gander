package dev.compactmods.gander.runtime.baked.model;

import dev.compactmods.gander.runtime.mixin.accessor.ModelManagerAccessor;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Rebakes meshes to not use an interleaved format for their data. Leaves the
 * original data as-is for mods which rely on the interleaved data for their own
 * purposes.
 */
public final class ModelRebaker
{
    public void rebakeModels(ModelManagerAccessor manager, ProfilerFiller reloadProfiler)
    {
        try
        {
            /*
            reloadProfiler.popPush("material_instances");
            var modelMaterials = new HashMap<DisplayableMesh, Set<MaterialInstance>>();
            for (var pair : bakedModelMeshes.entrySet())
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Baking material instances of model {}", pair.getKey());

                var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels()
                    .get(pair.getKey());

                pair.getValue().allMeshes()
                    .flatMap(it -> getMaterialInstances(
                        bakery,
                        pair.getKey(),
                        unbakedModel,
                        it,
                        false))
                    .forEach(it -> modelMaterials.put(it.getKey(), it.getValue()));
            }

            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Baked {} different material instances", modelMaterials.size());

            reloadProfiler.popPush("material_instances_cache");
            modelMaterialInstances = Collections.unmodifiableMap(modelMaterials);*/
        }
        catch (Exception e)
        {
            //LOGGER.error("Failed to rebake models", e);
        }
    }

    /*

    // TODO: support custom UnbakedModel types?
    private Stream<Map.Entry<DisplayableMesh, Set<MaterialInstance>>> getMaterialInstances(
        ModelBakery bakery, ModelResourceLocation key,
        UnbakedModel model,
        DisplayableMesh component,
        boolean uvLocked)
    {
        switch (model)
        {
            case BlockModel block ->
            {
                return Stream.of(getBlockModelMaterials(key, block, component, uvLocked));
            }
            case MultiPart multiPart ->
            {
                return multiPart.getMultiVariants()
                    .stream()
                    .map(MultiVariant::getVariants)
                    .flatMap(List::stream)
                    .flatMap(it -> getMaterialInstances(bakery, key, bakery.getModel(it.getModelLocation()), component, uvLocked || it.isUvLocked()));
            }
            case MultiVariant multiVariant ->
            {
                return multiVariant.getVariants()
                    .stream()
                    .flatMap(it -> getMaterialInstances(bakery, key, bakery.getModel(it.getModelLocation()), component, uvLocked || it.isUvLocked()));
            }
            default -> throw new IllegalStateException("Unexpected value: " + model);
        }
    }

    private Map.Entry<DisplayableMesh, Set<MaterialInstance>> getBlockModelMaterials(
        ModelResourceLocation name,
        final BlockModel model,
        final DisplayableMesh component,
        boolean isUvLocked)
    {
        Stream<Entry<String, Either<Material, String>>> materials = Stream.of();
        for (var mdl = model; mdl != null; mdl = mdl.parent)
        {
            materials = Stream.concat(materials, mdl.textureMap.entrySet().stream());
        }

        // All materials used by this model and its parents
        var fullMaterialMap = new HashSet<Map.Entry<String, Either<Material, String>>>();
        materials
            .sorted(Map.Entry.<String, Either<Material, String>>comparingByKey()
                .thenComparing((left, right) -> {
                    var leftMatOrRef = left.getValue();
                    var rightMatOrRef = right.getValue();

                    var result = leftMatOrRef.mapBoth(
                        leftMaterial -> rightMatOrRef.mapBoth(
                            rightMaterial -> 0,
                            rightRef -> -1
                        ),
                        leftRef -> rightMatOrRef.mapBoth(
                            rightMaterial -> 1,
                            rightRef -> 0
                        ));

                    return Either.unwrap(Either.unwrap(result));
                }))
            .forEach(fullMaterialMap::add);

        // All of the material parents pulled from the parent meshes
        // TODO: is .put here sane? What if two parts of an archetype use a name
        // in different ways?
        var parentsByName = new HashMap<String, MaterialParent>();
        getArchetypes(name)
            .allMeshes()
            .map(DisplayableMesh::mesh)
            .map(BakedMesh::materials)
            .flatMap(List::stream)
            .forEach(material -> {
                parentsByName.put(material.name(), material);
            });

        // All of the overriden material instances in this mesh
        // Any missing parents will be created, and the instance will be populated
        // based on this new parent.
        var overridenByName = new HashMap<String, MaterialInstance>();
        fullMaterialMap.stream()
            .map(it -> Pair.of(it.getKey(), it.getValue().left().orElse(null)))
            .filter(it -> it.getSecond() != null)
            .forEach(material -> {
                var missingFromParent = new MaterialParent(material.getFirst(),
                    material.getSecond().atlasLocation(),
                    material.getSecond().texture());
                var parent = parentsByName.putIfAbsent(
                    material.getFirst(),
                    missingFromParent);
                if (parent != null)
                {
                    // If the parent already exists, we override it with
                    // whatever texture we were supplied
                    overridenByName.put(
                        material.getFirst(),
                        new MaterialInstance(
                            material.getFirst(),
                            parent,
                            material.getSecond().texture(),
                            isUvLocked));
                }
                else
                {
                    // If it doesn't, we create a new material instance inheriting
                    // from the parent we just made, using the same name.
                    overridenByName.put(
                        missingFromParent.name(),
                        new MaterialInstance(
                            missingFromParent.name(),
                            missingFromParent,
                            null,
                            isUvLocked));
                }
            });

        // Now we have enough information to build the final set of material instances
        // from the full material map for this model
        var allMaterials = fullMaterialMap.stream()
            .map(it -> it.getValue()
                // If it's a direct material, we look for its instance in the
                // overriden material map where we made it
                .map(material -> Map.entry(it.getKey(),
                        overridenByName.get(it.getKey())),
                    // If it's a reference, we look for an overriden material
                    // by the original name, or create a new material instance
                    // referring to the parent we're referring to
                    ref -> Map.entry(it.getKey(),
                        overridenByName.getOrDefault(
                            it.getKey(),
                            new MaterialInstance(
                                it.getKey(),
                                parentsByName.getOrDefault(ref,
                                    MaterialParent.MISSING),
                                null,
                                isUvLocked)))))
            .filter(Objects::nonNull)
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        return Map.entry(component, allMaterials);
    }

    @SuppressWarnings("deprecation")
    private static RenderType getRenderType(ResourceLocation hint, BlockState state)
    {
        // If we have no hint, we have to fall back to the Vanilla map
        if (hint == null)
            return ItemBlockRenderTypes.getChunkRenderType(state);

        // If we do, pull it from the same source Neo does
        var group = NamedRenderTypeManager.get(hint);
        // If it doesn't exist... :ohno:
        if (group.isEmpty())
            return RenderType.solid();

        return group.block();
    }

     */
}
