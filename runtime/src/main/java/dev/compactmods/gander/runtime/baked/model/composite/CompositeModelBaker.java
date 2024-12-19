package dev.compactmods.gander.runtime.baked.model.composite;

import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import dev.compactmods.gander.runtime.mixin.accessor.UnbakedCompositeModelAccessor;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.UnbakedCompositeModel;

import java.util.function.BiFunction;
import java.util.stream.Stream;

public final class CompositeModelBaker
{
    private CompositeModelBaker() { }

    public static Stream<ArchetypeComponent> bakeCompositeModel(
        ModelResourceLocation name,
        UnbakedCompositeModel composite,
        BiFunction<ModelResourceLocation, ResourceLocation, Stream<ArchetypeComponent>> bakeComponents)
    {
        var accessor = (UnbakedCompositeModelAccessor)composite;

        return accessor.getChildren().entrySet()
            .stream()
            .flatMap(it -> bakeComponents.apply(name, it.getValue()));
    }
}
