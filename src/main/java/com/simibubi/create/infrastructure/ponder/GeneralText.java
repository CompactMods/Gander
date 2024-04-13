package com.simibubi.create.infrastructure.ponder;

import java.util.function.BiConsumer;

import com.simibubi.create.Create;
import com.simibubi.create.ponder.PonderTooltipHandler;
import com.simibubi.create.ponder.ui.PonderUI;

public class GeneralText {
	public static void provideLang(BiConsumer<String, String> consumer) {
		consume(consumer, PonderTooltipHandler.HOLD_TO_PONDER, "Hold [%1$s] to Ponder");
		consume(consumer, PonderTooltipHandler.SUBJECT, "Subject of this scene");
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
		consumer.accept(Create.ID + "." + key, enUS);
	}
}
