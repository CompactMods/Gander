package com.simibubi.create.datagen;

import net.minecraftforge.data.event.GatherDataEvent;

public class CreateDatagen {
	public static void gatherData(GatherDataEvent event) {
		final var generator = event.getGenerator();
		if(event.includeClient()) {
			generator.addProvider(true, new EnglishLangGenerator(generator));
		}
	}
}
