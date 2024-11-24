package dev.compactmods.gander;

import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.compactmods.gander.network.OpenGanderUiForDeferredStructureRequest;
import dev.compactmods.gander.network.OpenGanderUiForStructureRequest;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod("gander")
public class GanderLib {

	public static final String ID = "gander";

	public static final Logger LOGGER = LogUtils.getLogger();
	public static final String NET_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			asResource("main"),
			() -> NET_VERSION,
			NET_VERSION::equals,
			NET_VERSION::equals
	);

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = new Random();

	public GanderLib(/*IEventBus modEventBus*/) {
		onPacketRegistration();
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}

	private void onPacketRegistration() {
		CHANNEL.registerMessage(0, OpenGanderUiForDeferredStructureRequest.class, OpenGanderUiForDeferredStructureRequest::encode, OpenGanderUiForDeferredStructureRequest::new, OpenGanderUiForDeferredStructureRequest::handle);
		CHANNEL.registerMessage(1, OpenGanderUiForStructureRequest.class, OpenGanderUiForStructureRequest::encode, OpenGanderUiForStructureRequest::new, OpenGanderUiForStructureRequest::handle);
		CHANNEL.registerMessage(2, StructureSceneDataRequest.class, StructureSceneDataRequest::encode, StructureSceneDataRequest::new, StructureSceneDataRequest::handle);
	}
}
