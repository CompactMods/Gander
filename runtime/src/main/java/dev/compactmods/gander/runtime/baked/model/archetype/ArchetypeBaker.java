package dev.compactmods.gander.runtime.baked.model.archetype;

import dev.compactmods.gander.render.event.RegisterGeometryProvidersEvent;
import dev.compactmods.gander.render.event.RegisterGeometryProvidersEvent.IUnbakedModelGeometryProvider;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ArchetypeBaker
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchetypeBaker.class);

    private static final Map<Class<?>, IUnbakedModelGeometryProvider<?>> MODEL_GEOMETRY_PROVIDERS;

    static
    {
        // TODO: does this need to be concurrent?
        Map<Class<?>, IUnbakedModelGeometryProvider<?>> modelGeometryProviders = new HashMap<>();

        ModLoader.postEvent(new RegisterGeometryProvidersEvent(modelGeometryProviders::put));

        MODEL_GEOMETRY_PROVIDERS = Collections.unmodifiableMap(modelGeometryProviders);
    }

    @SuppressWarnings({"unchecked"})
    public static IUnbakedModelGeometryProvider<UnbakedModel> getProvider(UnbakedModel model)
    {
        return (IUnbakedModelGeometryProvider<UnbakedModel>)MODEL_GEOMETRY_PROVIDERS.get(model.getClass());
    }

    private ArchetypeBaker() { }

    @Nullable
    public static ResourceLocation getRenderType(UnbakedModel model)
    {
        // TODO: actually read this
        return ResourceLocation.withDefaultNamespace("solid");
    }
}
