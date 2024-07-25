package dev.compactmods.gander.runtime.baked.model.archetype;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.render.baked.model.block.BlockModelBaker;
import dev.compactmods.gander.render.baked.model.composite.CompositeModelBaker;
import dev.compactmods.gander.render.baked.model.obj.ObjModelBaker;
import dev.compactmods.gander.runtime.mixin.accessor.BlockGeometryBakingContextAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.BlockModelAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.ModelBakeryAccessor;
import dev.compactmods.gander.runtime.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

public final class ArchetypeBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBaker.class);

    private ArchetypeBaker() { }

    // TODO: this should support mods registering their own geometry types
    public static Stream<ArchetypeComponent> bakeArchetypeComponents(
        ModelResourceLocation originalName,
        UnbakedModel model)
    {
        return switch (model)
        {
            case BlockModel block ->
                // Since we know we're already a "root" model, this is safe to call
                block.customData.hasCustomGeometry()
                    ? dispatchBakeCustomGeometry(
                        originalName,
                        block,
                        Objects.requireNonNull(
                            block.customData.getCustomGeometry()))
                    : BlockModelBaker.bakeBlockModel(originalName, block);
            default -> throw new IllegalStateException("Unexpected value: " + model);
        };
    }

    // TODO: this should support mods registering their own geometry types
    private static Stream<ArchetypeComponent> dispatchBakeCustomGeometry(
        ModelResourceLocation originalName,
        BlockModel model,
        IUnbakedGeometry<?> geometry)
    {
        return switch (geometry)
        {
            case CompositeModel composite ->
                CompositeModelBaker.bakeCompositeModel(originalName, model, composite);
            case ObjModel obj ->
                ObjModelBaker.bakeObjModel(originalName, model, obj);
            default ->
            {
                LOGGER.error("Unsupported custom geometry type {}", geometry.getClass());
                yield Stream.of();
            }
        };
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
