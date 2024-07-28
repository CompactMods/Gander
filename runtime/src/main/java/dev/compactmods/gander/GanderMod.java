package dev.compactmods.gander;

import dev.compactmods.gander.runtime.baked.model.block.BlockModelBaker;
import dev.compactmods.gander.runtime.baked.model.composite.CompositeModelBaker;
import dev.compactmods.gander.runtime.baked.model.obj.ObjModelBaker;
import dev.compactmods.gander.render.event.RegisterGeometryProvidersEvent;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.obj.ObjModel;

@Mod(GanderMod.ID)
public class GanderMod
{
    public static final String ID = "gander";

    public GanderMod(IEventBus modEventBus, Dist dist)
    {
        if (dist.isClient())
            modEventBus.addListener(RegisterGeometryProvidersEvent.class,
                GanderModClient::registerGeometryProviders);
    }

    final class GanderModClient
    {
        public static void registerGeometryProviders(
            RegisterGeometryProvidersEvent e)
        {
            e.registerUnbakedModelProvider(BlockModel.class, BlockModelBaker::bakeBlockModel);
            e.registerUnbakedGeometryProvider(CompositeModel.class, CompositeModelBaker::bakeCompositeModel);
            e.registerUnbakedGeometryProvider(ObjModel.class, ObjModelBaker::bakeObjModel);
        }
    }
}
