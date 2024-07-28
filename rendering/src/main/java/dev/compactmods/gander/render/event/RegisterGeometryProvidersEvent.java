package dev.compactmods.gander.render.event;

import dev.compactmods.gander.render.baked.model.archetype.ArchetypeComponent;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * An event fired during initialization to register custom geometry providers.
 */
public final class RegisterGeometryProvidersEvent extends Event implements IModBusEvent
{
    private final BiConsumer<Class<?>, IUnbakedModelGeometryProvider<?>> _registerUnbakedModel;
    private final BiConsumer<Class<?>, IUnbakedModelGeometryProvider<BlockModel>> _registerCustomGeometry;

    @ApiStatus.Internal
    public RegisterGeometryProvidersEvent(
        BiConsumer<Class<?>, IUnbakedModelGeometryProvider<?>> registerUnbakedModel,
        BiConsumer<Class<?>, IUnbakedModelGeometryProvider<BlockModel>> registerCustomGeometry)
    {
        _registerUnbakedModel = registerUnbakedModel;
        _registerCustomGeometry = registerCustomGeometry;
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

    /**
     * Registers a provider for the given NeoForge custom model geometry type.
     *
     * @param clazz The {@link Class} of the model type to be registered.
     * @param geometryProvider The {@link IGeometryProvider} to register.
     * @param <T> The type of the model to register a geometry provider for.
     */
    public <T extends IUnbakedGeometry<T>> void registerUnbakedGeometryProvider(
        Class<T> clazz,
        ICustomGeometryProvider<T> geometryProvider)
    {
        _registerCustomGeometry.accept(clazz, geometryProvider);
    }

    @FunctionalInterface
    public interface IUnbakedModelGeometryProvider<T extends UnbakedModel>
    {
        Stream<ArchetypeComponent> bake(
            ModelResourceLocation originalName,
            T originalModel);
    }

    @FunctionalInterface
    public interface ICustomGeometryProvider<T extends IUnbakedGeometry<T>>
        extends IUnbakedModelGeometryProvider<BlockModel>
    {
        @SuppressWarnings("unchecked")
        default Stream<ArchetypeComponent> bake(
            ModelResourceLocation originalName,
            BlockModel originalModel)
        {
            return bake(
                originalName,
                originalModel,
                (T)originalModel.customData.getCustomGeometry());
        }

        Stream<ArchetypeComponent> bake(
            ModelResourceLocation originalName,
            BlockModel originalModel,
            T geometry);
    }
}
