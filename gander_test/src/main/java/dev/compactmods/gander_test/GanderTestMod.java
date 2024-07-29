package dev.compactmods.gander_test;

import java.util.Random;

import com.mojang.logging.LogUtils;

//import dev.compactmods.gander.network.GanderDebugRenderPacket;
import dev.compactmods.gander_test.network.GanderDebugRenderPacket;
import dev.compactmods.gander_test.network.StructureSceneDataRequest;
import dev.compactmods.gander_test.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander_test.network.OpenGanderUiForStructureRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import org.slf4j.Logger;

@Mod(GanderTestMod.ID)
public class GanderTestMod
{

	public static final String ID = "gander_test";

	public static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = new Random();

	public GanderTestMod(IEventBus modEventBus, Dist dist) {
		modEventBus.addListener(GanderTestMod::onPacketRegistration);

		//if (dist.isClient())
		//	NeoForge.EVENT_BUS.addListener(GanderDebugRenderPacket::render);
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}

	private static void onPacketRegistration(final RegisterPayloadHandlersEvent payloads) {
		final var main = payloads.registrar("1");

		main.playToClient(OpenGanderUiForDeferredStructureRequest.ID, OpenGanderUiForDeferredStructureRequest.STREAM_CODEC, OpenGanderUiForDeferredStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToServer(StructureSceneDataRequest.ID, StructureSceneDataRequest.STREAM_CODEC, StructureSceneDataRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToClient(OpenGanderUiForStructureRequest.ID, OpenGanderUiForStructureRequest.STREAM_CODEC, OpenGanderUiForStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToClient(GanderDebugRenderPacket.ID, GanderDebugRenderPacket.STREAM_CODEC, GanderDebugRenderPacket::handle)
			.executesOn(HandlerThread.MAIN);
	}
}
