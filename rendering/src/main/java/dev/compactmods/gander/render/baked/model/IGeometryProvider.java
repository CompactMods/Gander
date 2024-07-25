package dev.compactmods.gander.render.baked.model;

import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;

import java.util.stream.Stream;

/**
 * Declares an interface used to provide triangle-based geometry for instanced
 * rendering.
 */
@FunctionalInterface
public interface IGeometryProvider
{
    /**
     * Bakes the given model into its archetypes.
     *
     * @param originalName The original name of the model, as registered in
     * the {@link ModelManager}.
     * @param originalModel The original, unbaked model, as it appears in the
     * {@link ModelManager}.
     * @return A {@link Stream} of
     * {@link ArchetypeComponent ArchetypeComponents} which can be used during
     * rendering of this model.
     */
    Stream<ArchetypeComponent> bake(
        ModelResourceLocation originalName,
        UnbakedModel originalModel);
}
