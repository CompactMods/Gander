package com.simibubi.create;

import static com.simibubi.create.AllTags.forgeItemTag;
import static com.simibubi.create.Create.REGISTRATE;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class AllItems {
	public static final ItemEntry<Item> BRASS_HAND = ingredient("brass_hand");

	public static final ItemEntry<Item> BRASS_INGOT = taggedIngredient("brass_ingot", forgeItemTag("ingots/brass"));

	private static ItemEntry<Item> ingredient(String name) {
		return REGISTRATE.item(name, Item::new)
				.register();
	}

	@SafeVarargs
	private static ItemEntry<Item> taggedIngredient(String name, TagKey<Item>... tags) {
		return REGISTRATE.item(name, Item::new)
			.tag(tags)
			.register();
	}

	public static void register() {
	}

}
