package dev.compactmods.gander.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.ponder.core.GeneralText;
import dev.compactmods.gander.ponder.core.PonderIndex;
import dev.compactmods.gander.ponder.PonderLocalization;

import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.Map;
import java.util.function.BiConsumer;

public class EnglishLangGenerator extends LanguageProvider {
	public EnglishLangGenerator(DataGenerator gen) {
		super(gen.getPackOutput(), GanderLib.ID, "en_us");
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
		PonderLocalization.provideLang(GanderLib.ID, consumer);
	}
}
