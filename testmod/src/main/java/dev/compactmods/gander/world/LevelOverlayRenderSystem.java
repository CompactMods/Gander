package dev.compactmods.gander.world;

import dev.compactmods.gander.GanderTestMod;

import org.joml.Vector3f;

import dev.compactmods.gander.CommonEvents;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LevelOverlayRenderSystem
{
	public static void createAndAddRenderer(Component source, StructureTemplate data, Vector3f renderLocation) {
		CommonEvents.setTitle(source);

        var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
		var virtualLevel = new VirtualLevel(Minecraft.getInstance().level.registryAccess(), true, (newLevel) -> {
            var bakedLevel = LevelBakery.bakeVertices(newLevel, bounds, new Vector3f());
            var newRenderer = LevelInLevelRenderer.create(bakedLevel, newLevel, renderLocation);

            //TODO: FIgure out how to override the existing LIL renderer
            //GanderTestMod.addLevelInLevelRenderer(newRenderer);
        });

		virtualLevel.setBounds(bounds);
		data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_CLIENTS);

		var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
		final var newRenderer = LevelInLevelRenderer.create(bakedLevel, virtualLevel, renderLocation);

        GanderTestMod.addLevelInLevelRenderer(newRenderer);
	}
}
