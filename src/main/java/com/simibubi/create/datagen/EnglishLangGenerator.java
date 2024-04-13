package com.simibubi.create.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.Create;

import com.simibubi.create.infrastructure.ponder.GeneralText;
import com.simibubi.create.infrastructure.ponder.PonderIndex;
import com.simibubi.create.ponder.PonderLocalization;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

import java.util.Map;
import java.util.function.BiConsumer;

public class EnglishLangGenerator extends LanguageProvider {
	public EnglishLangGenerator(DataGenerator gen) {
		super(gen.getPackOutput(), Create.ID, "en_us");
	}

	@Override
	protected void addTranslations() {
		provideDefaultLang("interface");
		provideDefaultLang("tooltips");
		providePonderLang(this::add);
	}

	private void provideDefaultLang(String fileName) {
		String path = "assets/create/lang/default/" + fileName + ".json";
		JsonElement jsonElement = FilesHelper.loadJsonResource(path);
		if (jsonElement == null) {
			throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
		}
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().getAsString();
			add(key, value);
		}
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		// Register these since FMLClientSetupEvent does not run during datagen
		PonderIndex.register();
		GeneralText.provideLang(consumer);
		PonderLocalization.provideLang(Create.ID, consumer);
	}
}
