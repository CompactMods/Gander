package dev.compactmods.gander.ponder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

public class PonderLocalization {
	static final Map<ResourceLocation, String> SHARED = new HashMap<>();
	static final Map<ResourceLocation, Map<String, String>> SPECIFIC = new HashMap<>();

	//

	public static void registerShared(ResourceLocation key, String enUS) {
		SHARED.put(key, enUS);
	}

	public static void registerSpecific(ResourceLocation sceneId, String key, String enUS) {
		SPECIFIC.computeIfAbsent(sceneId, $ -> new HashMap<>())
				.put(key, enUS);
	}

	//

	public static final String LANG_PREFIX = "ponder.";

	protected static String langKeyForShared(ResourceLocation k) {
		return k.getNamespace() + "." + LANG_PREFIX + "shared." + k.getPath();
	}

	protected static String langKeyForSpecific(ResourceLocation sceneId, String k) {
		return sceneId.getNamespace() + "." + LANG_PREFIX + sceneId.getPath() + "." + k;
	}

	//

	public static String getShared(ResourceLocation key) {
		return I18n.get(langKeyForShared(key));
	}

	public static String getSpecific(ResourceLocation sceneId, String k) {
		return I18n.get(langKeyForSpecific(sceneId, k));
	}

	public static void provideLang(String namespace, BiConsumer<String, String> consumer) {
		SHARED.forEach((k, v) -> {
			if (k.getNamespace().equals(namespace)) {
				consumer.accept(langKeyForShared(k), v);
			}
		});

		SPECIFIC.entrySet()
			.stream()
			.filter(entry -> entry.getKey().getNamespace().equals(namespace))
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> {
				entry.getValue()
					.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(subEntry -> consumer.accept(
						langKeyForSpecific(entry.getKey(), subEntry.getKey()), subEntry.getValue()));
			});
	}

	@Deprecated(forRemoval = true)
	public static void record(String namespace, JsonObject object) {
		provideLang(namespace, object::addProperty);
	}

}
