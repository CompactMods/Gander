package dev.compactmods.gander;

import net.minecraft.world.item.ItemStack;

public class ItemHelper {

	public static boolean sameItem(ItemStack stack, ItemStack otherStack) {
		return !otherStack.isEmpty() && stack.is(otherStack.getItem());
	}

}
