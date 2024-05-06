package dev.compactmods.gander.client.network;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.client.gui.GanderUI;
import dev.compactmods.gander.level.tick.TickingLevels;
import dev.compactmods.gander.render.baked.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.joml.Vector3f;

public class SceneDataClientHandler {

	public static void loadScene(Component source, StructureTemplate data) {
		final var mc = Minecraft.getInstance();
		if (mc.screen instanceof GanderUI ui) {
			var virtualLevel = new VirtualLevel(mc.level.registryAccess());
			var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
			virtualLevel.setBounds(bounds);
			data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(), Block.UPDATE_CLIENTS);

			TickingLevels.registerTicker(virtualLevel);
			var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
			ui.setSceneSource(source);
			ui.setScene(bakedLevel);
			ui.onClosed(() -> TickingLevels.unregisterTicker(virtualLevel));
		}
	}
}
