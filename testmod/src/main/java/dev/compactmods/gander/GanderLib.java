package dev.compactmods.gander;

import com.mojang.logging.LogUtils;
import dev.compactmods.gander.network.OpenUIPacket;
import dev.compactmods.gander.network.SceneDataRequest;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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
		final PayloadRegistrar main = payloads.registrar(GanderLib.ID)
				.versioned("1.0.0");

		main.playToClient(OpenUIPacket.ID, OpenUIPacket.STREAM_CODEC, OpenUIPacket::handle);

		// Scene sync
		main.playToServer(SceneDataRequest.ID, SceneDataRequest.STREAM_CODEC, SceneDataRequest::handle);
		main.playToClient(SceneDataRequest.SceneData.ID, SceneDataRequest.SceneData.STREAM_CODEC, SceneDataRequest.SceneData::handle);
	}
}
