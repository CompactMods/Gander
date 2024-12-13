package dev.compactmods.gander.client;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.network.StructureSceneDataRequest;

import org.joml.Vector3f;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.baked.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ClientPacketHandler {
	public static void handleOpenUiPacket(ResourceLocation sceneID) {
		var client = Minecraft.getInstance();
		client.tell(() -> {
			client.setScreen(new GanderUI());
			GanderTestMod.CHANNEL.sendToServer(new StructureSceneDataRequest(sceneID));
		});
	}

	public static void structureDataReceived(Component source, StructureTemplate data) {
		var client = Minecraft.getInstance();
		client.tell(() -> {
			if (client.screen instanceof GanderUI ui) {
				sceneSetup(source, data, ui);
				return;
			}

			GanderUI ui = new GanderUI();
			client.setScreen(ui);
			sceneSetup(source, data, ui);
		});
	}

	private static void sceneSetup(Component source, StructureTemplate data, GanderUI ui) {
		var virtualLevel = new VirtualLevel(Minecraft.getInstance().level.registryAccess());
		var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
		virtualLevel.setBounds(bounds);
		data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_CLIENTS);

		var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
		ui.setSceneSource(source);
		ui.setScene(bakedLevel);
	}
}
