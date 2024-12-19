package dev.compactmods.gander.render.event;

import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * An event fired during initialization to register custom geometry providers.
 */
public final class RegisterGeometryProvidersEvent extends Event implements IModBusEvent
{
    private final BiConsumer<Class<?>, IUnbakedModelGeometryProvider<?>> _registerUnbakedModel;

    @ApiStatus.Internal
    public RegisterGeometryProvidersEvent(
        BiConsumer<Class<?>, IUnbakedModelGeometryProvider<?>> registerUnbakedModel)
    {
        _registerUnbakedModel = registerUnbakedModel;
    }

    /**
     * Registers a provider for the given "vanilla" unbaked model type.
     *
     * @param clazz The {@link Class} of the model type to be registered.
     * @param geometryProvider The {@link IUnbakedModelGeometryProvider} to register.
     * @param <T> The type of the model to register a geometry provider for.
     */
    public <T extends UnbakedModel> void registerUnbakedModelProvider(
        Class<T> clazz,
        IUnbakedModelGeometryProvider<T> geometryProvider)
    {
        _registerUnbakedModel.accept(clazz, geometryProvider);
    }

    @FunctionalInterface
    public interface IUnbakedModelGeometryProvider<T extends UnbakedModel>
    {
        Stream<ArchetypeComponent> bake(
            ModelResourceLocation originalName,
            T originalModel,
            BiFunction<ModelResourceLocation, ResourceLocation, Stream<ArchetypeComponent>> bakeComponent);
    }
}
