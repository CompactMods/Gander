package dev.compactmods.gander;

import dev.compactmods.gander.gui.UIRenderHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class CreateClient {

	public static final ClientResourceReloadListener RESOURCE_RELOAD_LISTENER = new ClientResourceReloadListener();

	public static void onCtorClient(IEventBus modEventBus) {
		modEventBus.addListener(CreateClient::clientInit);
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		DebugScenes.registerAll();
		UIRenderHelper.init();
	}
}
