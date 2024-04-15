package dev.compactmods.gander;

import java.util.Random;

import com.mojang.logging.LogUtils;

import dev.compactmods.gander.datagen.CreateDatagen;

import dev.compactmods.gander.ponder.core.OpenPonderPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

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
		modEventBus.addListener(EventPriority.LOWEST, CreateDatagen::gatherData);
		modEventBus.addListener(GanderLib::onPacketRegistration);

		if (FMLEnvironment.dist.isClient())
			CreateClient.onCtorClient(modEventBus);
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}

	private static void onPacketRegistration(final RegisterPayloadHandlerEvent payloads) {
		final IPayloadRegistrar main = payloads.registrar(GanderLib.ID)
				.versioned("1.0.0");

		main.play(OpenPonderPacket.ID, OpenPonderPacket::new, builder ->
				builder.client(OpenPonderPacket.HANDLER));
	}
}
