package dev.compactmods.gander;

import dev.compactmods.gander.core.GanderCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class CommonEvents {

	private static Component TITLE = Component.empty();

	private static void registerCommands(RegisterCommandsEvent event) {
		var dispatcher = event.getDispatcher();
		var root = GanderCommand.make(event.getBuildContext());
		dispatcher.register(root);
	}

	private static void test(RegisterGuiLayersEvent event) {
		event.registerAboveAll(GanderLib.asResource("title"), (graphics, partialTick) -> {
			var font = Minecraft.getInstance().font;
			graphics.drawCenteredString(font, TITLE, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2, font.lineHeight, CommonColors.WHITE);
		});
	}

	public static void setTitle(Component title) {
		TITLE = title;
	}

	public static void register(IEventBus modBus) {
		NeoForge.EVENT_BUS.addListener(CommonEvents::registerCommands);

		modBus.addListener(CommonEvents::test);
	}
}
