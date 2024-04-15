package dev.compactmods.gander.ponder.core;

import java.util.function.BiConsumer;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.ponder.ui.PonderUI;

public class GeneralText {
	public static void provideLang(BiConsumer<String, String> consumer) {
		consume(consumer, PonderUI.PONDERING, "Pondering about...");
		consume(consumer, PonderUI.IDENTIFY_MODE, "Identify mode active.\nUnpause with [%1$s]");

		consume(consumer, PonderUI.CLOSE, "Close");
		consume(consumer, PonderUI.IDENTIFY, "Identify");
		consume(consumer, PonderUI.NEXT, "Next Scene");
		consume(consumer, PonderUI.NEXT_UP, "Up Next:");
		consume(consumer, PonderUI.PREVIOUS, "Previous Scene");
		consume(consumer, PonderUI.REPLAY, "Replay");
		consume(consumer, PonderUI.THINK_BACK, "Think Back");
		consume(consumer, PonderUI.SLOW_TEXT, "Comfy Reading");
	}

	private static void consume(BiConsumer<String, String> consumer, String key, String enUS) {
		consumer.accept(GanderLib.ID + "." + key, enUS);
	}
}
