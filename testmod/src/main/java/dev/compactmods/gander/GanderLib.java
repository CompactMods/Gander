package dev.compactmods.gander;

import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.compactmods.gander.client.event.LevelRenderEventHandler;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@Mod("gander")
public class GanderLib {

	public static final String ID = "gander";

	public static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = new Random();

	public GanderLib(IEventBus modEventBus) {
		modEventBus.addListener(GanderLib::onPacketRegistration);
		CommonEvents.register(modEventBus);

		// now we wait 20 years for Neogradle to get its shit together.....
		final var gameBus = NeoForge.EVENT_BUS;
		gameBus.addListener(LevelRenderEventHandler::onRenderStage);
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
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
}
