package com.simibubi.create.foundation.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.utility.Components;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class KineticStats implements TooltipModifier {
	protected final Block block;

	public KineticStats(Block block) {
		this.block = block;
	}

	@Nullable
	public static KineticStats create(Item item) {
		return null;
	}

	@Override
	public void modify(ItemTooltipEvent context) {
		List<Component> kineticStats = getKineticStats(block, context.getEntity());
		if (!kineticStats.isEmpty()) {
			List<Component> tooltip = context.getToolTip();
			tooltip.add(Components.immutableEmpty());
			tooltip.addAll(kineticStats);
		}
	}

	public static List<Component> getKineticStats(Block block, Player player) {
        return new ArrayList<>();
	}
}
