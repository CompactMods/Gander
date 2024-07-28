package dev.compactmods.gander.runtime.baked.model.archetype;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.render.event.RegisterGeometryProvidersEvent;
import dev.compactmods.gander.render.event.RegisterGeometryProvidersEvent.IUnbakedModelGeometryProvider;
import dev.compactmods.gander.runtime.mixin.accessor.BlockGeometryBakingContextAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.BlockModelAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.ModelBakeryAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

public final class ArchetypeBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBaker.class);

    private static final Map<Class<?>, IUnbakedModelGeometryProvider<?>> MODEL_GEOMETRY_PROVIDERS;
    private static final Map<Class<?>, IUnbakedModelGeometryProvider<?>> NEO_CUSTOM_GEOMETRY_PROVIDERS;

    static
    {
        // TODO: does this need to be concurrent?
        Map<Class<?>, IUnbakedModelGeometryProvider<?>> modelGeometryProviders = new HashMap<>();
        Map<Class<?>, IUnbakedModelGeometryProvider<?>> neoCustomGeometryProviders = new HashMap<>();

        ModLoader.postEvent(new RegisterGeometryProvidersEvent(
            modelGeometryProviders::put,
            neoCustomGeometryProviders::put));

        MODEL_GEOMETRY_PROVIDERS = Collections.unmodifiableMap(modelGeometryProviders);
        NEO_CUSTOM_GEOMETRY_PROVIDERS = Collections.unmodifiableMap(neoCustomGeometryProviders);
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    private static IUnbakedModelGeometryProvider<UnbakedModel> getProvider(UnbakedModel model)
    {
        if (model instanceof BlockModel blockModel
            && blockModel.customData.hasCustomGeometry())
        {
            var geom = blockModel.customData.getCustomGeometry();
            return (IUnbakedModelGeometryProvider<UnbakedModel>)NEO_CUSTOM_GEOMETRY_PROVIDERS.get(geom.getClass());
        }

        return (IUnbakedModelGeometryProvider<UnbakedModel>)MODEL_GEOMETRY_PROVIDERS.get(model.getClass());
    }

    private ArchetypeBaker() { }

    public static Stream<ArchetypeComponent> bakeArchetypeComponents(
        ModelResourceLocation originalName,
        UnbakedModel model)
    {
        var provider = getProvider(model);
        if (provider == null)
            throw new IllegalStateException("Unknown model kind: " + model);

        return provider.bake(originalName, model);
    }

    public static BiMap<ModelResourceLocation, UnbakedModel> getArchetypes(
        ModelResourceLocation model, ModelManagerAccessor manager, ModelBakery bakery)
    {
        var result = ImmutableBiMap.<ModelResourceLocation, UnbakedModel>builder();
        var visited = new HashSet<ResourceLocation>();
        var known = new HashSet<ResourceLocation>();
        var queue = new ArrayDeque<UnbakedModel>();

        var unbakedModel = ((ModelBakeryAccessor)bakery).getTopLevelModels().get(model);

        // If the model itself has geometry, it is its own archetype.
        if (hasGeometry(unbakedModel))
        {
            return ImmutableBiMap.of(model, unbakedModel);
        }

        // Otherwise, we need to search its parents
        queue.add(unbakedModel);
        while (!queue.isEmpty())
        {
            var first = queue.removeFirst();
            for (var dep : first.getDependencies())
            {
                var childModel = bakery.getModel(dep);
                // TODO: figure out if this is what we want to do here
                if (childModel == manager.getMissingModel()) continue;

                // TODO: check deps.isEmpty() ?
                // If it has geometry, it overrides any other parents
                if (hasGeometry(childModel))
                {
                    if (known.add(dep))
                        result.put(new ModelResourceLocation(dep, "gander_archetype"), childModel);
                }
                // Otherwise, check we haven't seen it before.
                else if (visited.add(dep))
                {
                    queue.addLast(childModel);
                }
            }
        }

        return result.build();
    }

    @Nullable
    public static ResourceLocation getRenderType(UnbakedModel model)
    {
        // TODO: is this safe?
        if (!(model instanceof BlockModel blockModel))
            return null;

        return blockModel.customData.getRenderTypeHint();
    }

    // TODO: support custom geometry here
    private static boolean hasGeometry(UnbakedModel model)
    {
        // If the child model has its own geometry, it overrides any
        // parents.
        if (model instanceof BlockModel blockModel)
        {
            var customDataAccessor = (BlockGeometryBakingContextAccessor)blockModel.customData;
            var modelAccessor = (BlockModelAccessor)blockModel;
            return customDataAccessor.getOwnCustomGeometry() != null
                || !modelAccessor.getOwnElements().isEmpty();
        }

        return false;
    }
}
