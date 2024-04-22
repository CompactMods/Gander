package dev.compactmods.gander;

import dev.compactmods.gander.client.gui.UIRenderHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

public class CreateClient {

	public static void onCtorClient(IEventBus modEventBus) {
		modEventBus.addListener(CreateClient::clientInit);
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		UIRenderHelper.init();
	}
}
