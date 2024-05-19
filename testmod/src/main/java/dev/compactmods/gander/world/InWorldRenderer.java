package dev.compactmods.gander.world;

import dev.compactmods.gander.VirtualLevelRenderers;
import dev.compactmods.gander.client.gui.GanderUI;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.baked.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Vector3f;

public class InWorldRenderer
{
	public static void forStructure(ResourceLocation sceneID) {
		PacketDistributor.sendToServer(new StructureSceneDataRequest(sceneID, true));
	}

	public static void forStructureData(Component source, StructureTemplate data) {
		var virtualLevel = new VirtualLevel(Minecraft.getInstance().level.registryAccess());
		var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
		virtualLevel.setBounds(bounds);
		data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_CLIENTS);

		var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
		VirtualLevelRenderers.registerLevelToRender(bakedLevel, virtualLevel);
	}
}
