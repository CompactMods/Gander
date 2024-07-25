package dev.compactmods.gander.render.baked.model.composite;

import dev.compactmods.gander.render.baked.model.archetype.Archetypes;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.render.mixin.accessor.CompositeModelAccessor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.CompositeModel;

import java.util.stream.Stream;

public final class CompositeModelBaker
{
    private CompositeModelBaker() { }

    public static Stream<ArchetypeComponent> bakeCompositeModel(
        ModelResourceLocation originalName,
        UnbakedModel model)
    {
        var accessor = (CompositeModelAccessor)composite;

        return accessor.getChildren().entrySet()
            .stream()
            .flatMap(it -> {
                var name = it.getValue().name;
                if (name.isBlank()) name = it.getKey();

                return ArchetypeBaker.bakeArchetypeComponents(
                    Archetypes.computeMeshName(originalName, "composite_child/" + name),
                    it.getValue());
            });
    }
}
