package dev.compactmods.gander.render.event;

import dev.compactmods.gander.render.baked.model.IGeometryProvider;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

/**
 * An event fired during initialization to register custom geometry providers.
 */
public final class RegisterGeometryProvidersEvent extends Event
{
    /**
     * Registers a provider for the given "vanilla" unbaked model type.
     *
     * @param clazz The {@link Class} of the model type to be registered.
     * @param geometryProvider The {@link IGeometryProvider} to register.
     * @param <T> The type of the model to register a geometry provider for.
     */
    public <T extends UnbakedModel> void registerUnbakedModelProvider(
        Class<T> clazz,
        IGeometryProvider geometryProvider)
    {

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
        IGeometryProvider geometryProvider)
    {

    }
}
