package com.simibubi.create.utility;

import java.util.Locale;

import com.simibubi.create.Create;

import net.minecraft.network.chat.MutableComponent;

public class Lang {

	/**
	 * legacy-ish. Use Lang.translate and other builder methods where possible
	 *
	 * @param key
	 * @param args
	 * @return
	 */
	public static MutableComponent translateDirect(String key, Object... args) {
		return Components.translatable(Create.ID + "." + key, resolveBuilders(args));
	}

	public static String asId(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	public static LangBuilder builder() {
		return new LangBuilder(Create.ID);
	}

	public static LangBuilder builder(String namespace) {
		return new LangBuilder(namespace);
	}

	public static LangBuilder number(double d) {
		return builder().text(LangNumberFormat.format(d));
	}

	public static LangBuilder translate(String langKey, Object... args) {
		return builder().translate(langKey, args);
	}

	public static LangBuilder text(String text) {
		return builder().text(text);
	}

	public static Object[] resolveBuilders(Object[] args) {
		for (int i = 0; i < args.length; i++)
			if (args[i]instanceof LangBuilder cb)
				args[i] = cb.component();
		return args;
	}

}
