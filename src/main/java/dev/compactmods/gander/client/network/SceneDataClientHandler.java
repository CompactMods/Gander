package dev.compactmods.gander.client.network;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.client.gui.GanderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SceneDataClientHandler {

	public static void loadScene(StructureTemplate data) {
		final var mc = Minecraft.getInstance();
		if (mc.screen instanceof GanderUI ui) {
			var virtualLevel = new VirtualLevel(mc.level.registryAccess());
			data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(), Block.UPDATE_CLIENTS);
			virtualLevel.setBounds(data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO));
			ui.setScene(virtualLevel);
		}
	}
}
