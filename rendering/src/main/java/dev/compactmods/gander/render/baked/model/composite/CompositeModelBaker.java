package dev.compactmods.gander.render.baked.model.composite;

import dev.compactmods.gander.render.baked.BakedMesh;
import dev.compactmods.gander.render.baked.model.archetype.ArchetypeBaker;
import dev.compactmods.gander.render.mixin.accessor.CompositeModelAccessor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.CompositeModel;

import java.util.Map;
import java.util.stream.Collectors;

public final class CompositeModelBaker
{
    private CompositeModelBaker() { }

    public static Map<ModelResourceLocation, BakedMesh> bakeCompositeModel(
        ModelResourceLocation originalName,
        UnbakedModel model,
        CompositeModel composite)
    {
        var accessor = (CompositeModelAccessor)composite;

        return accessor.getChildren().entrySet()
            .stream()
            .map(it -> {
                var name = it.getValue().name;
                if (name.isBlank()) name = it.getKey();

                return ArchetypeBaker.bakeArchetypes(
                    ArchetypeBaker.computeMeshName(originalName, "composite_child/" + name),
                    it.getValue());
            })
            .flatMap(it -> it.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
