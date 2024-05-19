package dev.compactmods.gander.world;

import dev.compactmods.gander.GanderTestMod;

import org.joml.Vector3f;

import dev.compactmods.gander.CommonEvents;
import dev.compactmods.gander.examples.LevelInLevelRenderer;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;

public class InWorldRenderer
{
	public static void forStructure(ResourceLocation sceneID, Vector3f renderLocation) {
		PacketDistributor.sendToServer(new StructureSceneDataRequest(sceneID, true, renderLocation));
	}

	public static void forStructureData(Component source, StructureTemplate data, Vector3f renderLocation) {
		CommonEvents.setTitle(source);

		var virtualLevel = new VirtualLevel(Minecraft.getInstance().level.registryAccess(), true);
		var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
		virtualLevel.setBounds(bounds);
		data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_CLIENTS);

		var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
		final var newRenderer = LevelInLevelRenderer.create(bakedLevel, virtualLevel, renderLocation);

        GanderTestMod.addLevelInLevelRenderer(newRenderer);
	}
}
