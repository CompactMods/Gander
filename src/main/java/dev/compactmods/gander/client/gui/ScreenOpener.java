package dev.compactmods.gander.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenOpener {

	public static void open(Screen screen) {
		Minecraft.getInstance()
			.tell(() -> {
				Minecraft.getInstance()
					.setScreen(screen);
			});
	}

}
