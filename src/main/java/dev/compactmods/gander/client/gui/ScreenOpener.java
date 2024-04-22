package dev.compactmods.gander.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import dev.compactmods.gander.utility.animation.LerpedFloat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenOpener {

	private static final Deque<Screen> backStack = new ArrayDeque<>();
	private static Screen backSteppedFrom = null;

	public static void open(Screen screen) {
		open(Minecraft.getInstance().screen, screen);
	}

	public static void open(@Nullable Screen current, Screen toOpen) {
		backSteppedFrom = null;
		if (current != null) {
			if (backStack.size() >= 15) // don't go deeper than 15 steps
				backStack.pollLast();

			backStack.push(current);
		} else
			backStack.clear();

		openScreen(toOpen);
	}

	private static void openScreen(Screen screen) {
		Minecraft.getInstance()
			.tell(() -> {
				Minecraft.getInstance()
					.setScreen(screen);
			});
	}

}
