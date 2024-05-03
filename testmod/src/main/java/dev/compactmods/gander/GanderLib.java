package dev.compactmods.gander;

import java.util.Random;

import com.mojang.logging.LogUtils;

import dev.compactmods.gander.network.SceneDataRequest;
import dev.compactmods.gander.network.OpenUIPacket;
import dev.compactmods.gander.network.SceneDataResponse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import org.slf4j.Logger;

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
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}

	private static void onPacketRegistration(final RegisterPayloadHandlersEvent payloads) {
		final var main = payloads.registrar("1");

		main.playToClient(OpenUIPacket.ID, OpenUIPacket.STREAM_CODEC, OpenUIPacket.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToServer(SceneDataRequest.ID, SceneDataRequest.STREAM_CODEC, SceneDataRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);
		main.playToClient(SceneDataResponse.ID, SceneDataResponse.STREAM_CODEC, SceneDataResponse.HANDLER)
				.executesOn(HandlerThread.MAIN);
	}
}
