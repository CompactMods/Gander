package dev.compactmods.gander.client.gui;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenOpener {

	public static void open(Screen screen) {
		open(() -> screen);
	}

	public static void open(Supplier<Screen> screen) {
		Minecraft.getInstance()
				.tell(() -> {
					Minecraft.getInstance()
							.setScreen(screen.get());
				});
	}

}
