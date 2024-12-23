package dev.compactmods.gander;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.compactmods.gander.examples.LevelInLevelRenderer;
import dev.compactmods.gander.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander.network.OpenGanderUiForStructureRequest;
import dev.compactmods.gander.network.RenderInWorldForDeferredStructureRequest;
import dev.compactmods.gander.network.RenderInWorldForStructureRequest;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@Mod("gander")
public class GanderTestMod {

	public static final String ID = "gander";

	public static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<UUID, LevelInLevelRenderer> LIL_RENDERERS = new ConcurrentHashMap<>();

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = new Random();

	public GanderTestMod(IEventBus modEventBus) {
		modEventBus.addListener(GanderTestMod::onPacketRegistration);
		CommonEvents.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent renderStage) -> LIL_RENDERERS.values()
            .forEach(lil -> lil.onRenderStage(renderStage)));

        if(FMLEnvironment.dist.isClient())
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> LIL_RENDERERS.values()
                .forEach(lil -> lil.onClientTick(event)));
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	private static void onPacketRegistration(final RegisterPayloadHandlersEvent payloads) {
		final var main = payloads.registrar("1");

		main.playToServer(StructureSceneDataRequest.ID, StructureSceneDataRequest.STREAM_CODEC, StructureSceneDataRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToClient(OpenGanderUiForDeferredStructureRequest.ID, OpenGanderUiForDeferredStructureRequest.STREAM_CODEC, OpenGanderUiForDeferredStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);
		main.playToClient(OpenGanderUiForStructureRequest.ID, OpenGanderUiForStructureRequest.STREAM_CODEC, OpenGanderUiForStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToClient(RenderInWorldForDeferredStructureRequest.ID, RenderInWorldForDeferredStructureRequest.STREAM_CODEC, RenderInWorldForDeferredStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);
		main.playToClient(RenderInWorldForStructureRequest.ID, RenderInWorldForStructureRequest.STREAM_CODEC, RenderInWorldForStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);
	}

    public static void addLevelInLevelRenderer(LevelInLevelRenderer lilRenderer) {
        LIL_RENDERERS.putIfAbsent(lilRenderer.id(), lilRenderer);
    }

    public static void removeLevelInLevelRenderer(UUID id) {
        LIL_RENDERERS.remove(id);
    }
}
