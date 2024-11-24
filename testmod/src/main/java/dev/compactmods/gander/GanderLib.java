package dev.compactmods.gander;

import java.util.Random;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("gander")
public class GanderLib {

	public static final String ID = "gander";

	public static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Use the {@link Random} of a local {@link Level} or {@link Entity} or create one
	 */
	@Deprecated
	public static final Random RANDOM = new Random();

	public GanderLib(/*IEventBus modEventBus*/) {
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		// modEventBus.addListener(GanderLib::onPacketRegistration);
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}

	// TODO: down port this
	/*private static void onPacketRegistration(final RegisterPayloadHandlersEvent payloads) {
		final var main = payloads.registrar("1");

		main.playToClient(OpenGanderUiForDeferredStructureRequest.ID, OpenGanderUiForDeferredStructureRequest.STREAM_CODEC, OpenGanderUiForDeferredStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToServer(StructureSceneDataRequest.ID, StructureSceneDataRequest.STREAM_CODEC, StructureSceneDataRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);

		main.playToClient(OpenGanderUiForStructureRequest.ID, OpenGanderUiForStructureRequest.STREAM_CODEC, OpenGanderUiForStructureRequest.HANDLER)
				.executesOn(HandlerThread.MAIN);
	}*/
}
