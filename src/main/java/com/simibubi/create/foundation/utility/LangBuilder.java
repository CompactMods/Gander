package com.simibubi.create.foundation.utility;

import java.util.List;

import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class LangBuilder {

	String namespace;
	MutableComponent component;

	public LangBuilder(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Appends a localised component<br>
	 * To add an independently formatted localised component, use add() and a nested
	 * builder
	 *
	 * @param langKey
	 * @param args
	 * @return
	 */
	public LangBuilder translate(String langKey, Object... args) {
		return add(Components.translatable(namespace + "." + langKey, Lang.resolveBuilders(args)));
	}

	/**
	 * Appends a text component
	 *
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(String literalText) {
		return add(Components.literal(literalText));
	}

	/**
	 * Appends a colored text component
	 *
	 * @param format
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(ChatFormatting format, String literalText) {
		return add(Components.literal(literalText).withStyle(format));
	}

	/**
	 * Appends a colored text component
	 *
	 * @param color
	 * @param literalText
	 * @return
	 */
	public LangBuilder text(int color, String literalText) {
		return add(Components.literal(literalText).withStyle(s -> s.withColor(color)));
	}

	/**
	 * Appends the contents of another builder
	 *
	 * @param otherBuilder
	 * @return
	 */
	public LangBuilder add(LangBuilder otherBuilder) {
		return add(otherBuilder.component());
	}

	/**
	 * Appends a component
	 *
	 * @param customComponent
	 * @return
	 */
	public LangBuilder add(MutableComponent customComponent) {
		component = component == null ? customComponent : component.append(customComponent);
		return this;
	}

	//

	/**
	 * Applies the format to all added components
	 *
	 * @param format
	 * @return
	 */
	public LangBuilder style(ChatFormatting format) {
		assertComponent();
		component = component.withStyle(format);
		return this;
	}

	/**
	 * Applies the color to all added components
	 *
	 * @param color
	 * @return
	 */
	public LangBuilder color(int color) {
		assertComponent();
		component = component.withStyle(s -> s.withColor(color));
		return this;
	}

	public MutableComponent component() {
		assertComponent();
		return component;
	}

	public String string() {
		return component().getString();
	}

	public String json() {
		return Component.Serializer.toJson(component());
	}

	public void sendChat(Player player) {
		player.displayClientMessage(component(), false);
	}

	private void assertComponent() {
		if (component == null)
			throw new IllegalStateException("No components were added to builder");
	}

}
