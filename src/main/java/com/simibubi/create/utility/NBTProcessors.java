package com.simibubi.create.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class NBTProcessors {

	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> processors = new HashMap<>();

	public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
		processors.put(type, processor);
	}

	static {
		addProcessor(BlockEntityType.SIGN, data -> {
			for (int i = 0; i < 4; ++i) {
				if (textComponentHasClickEvent(data.getString("Text" + (i + 1))))
					return null;
			}
			return data;
		});
		addProcessor(BlockEntityType.LECTERN, data -> {
			if (!data.contains("Book", Tag.TAG_COMPOUND))
				return data;
			CompoundTag book = data.getCompound("Book");

			if (!book.contains("tag", Tag.TAG_COMPOUND))
				return data;
			CompoundTag tag = book.getCompound("tag");

			if (!tag.contains("pages", Tag.TAG_LIST))
				return data;
			ListTag pages = tag.getList("pages", Tag.TAG_STRING);

			for (Tag inbt : pages) {
				if (textComponentHasClickEvent(inbt.getAsString()))
					return null;
			}
			return data;
		});
	}

	public static ItemStack withUnsafeNBTDiscarded(ItemStack stack) {
		if (stack.getTag() == null)
			return stack;
		ItemStack copy = stack.copy();
		stack.getTag()
			.getAllKeys()
			.stream()
			.filter(NBTProcessors::isUnsafeItemNBTKey)
			.forEach(copy::removeTagKey);
		return copy;
	}

	public static boolean isUnsafeItemNBTKey(String name) {
		if (name.equals(EnchantedBookItem.TAG_STORED_ENCHANTMENTS))
			return false;
		if (name.equals("Enchantments"))
			return false;
		if (name.contains("Potion"))
			return false;
		if (name.contains("Damage"))
			return false;
        return !name.equals("display");
    }

	public static boolean textComponentHasClickEvent(String json) {
		Component component = Component.Serializer.fromJson(json.isEmpty() ? "\"\"" : json);
		return component != null && component.getStyle() != null && component.getStyle()
			.getClickEvent() != null;
	}

	private NBTProcessors() {}

}
