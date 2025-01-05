package dev.compactmods.gander.client.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.phys.Vec3;

import org.apache.commons.lang3.function.Consumers;
import org.joml.Vector3f;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class ScreenOpener {
	public static void open(Supplier<Screen> screen) {
		var client = Minecraft.getInstance();
		client.tell(() -> client.setScreen(screen.get()));
	}

	public static void openGanderUI(Consumer<GanderUI> postSetup) {
		var client = Minecraft.getInstance();

		client.tell(() -> {
			var ui = new GanderUI();
			client.setScreen(ui);
			client.tell(() -> postSetup.accept(ui));
		});
	}

	public static void openGanderUI() {
		openGanderUI(Consumers.nop());
	}

	public static void forStructure(ResourceLocation sceneID) {
		open(() -> new GanderUI(new StructureSceneDataRequest(sceneID, false)));
	}

	public static void forStructureData(Component source, StructureTemplate data) {
		openGanderUI(ui -> {
            var bounds = data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO);
            var virtualLevel = new VirtualLevel(Minecraft.getInstance().level.registryAccess(), true, (newLevel) -> {
                var bakedLevel = LevelBakery.bakeVertices(newLevel, bounds, new Vector3f());
                ui.setScene(bakedLevel);
            });

			virtualLevel.setBounds(bounds);
			data.placeInWorld(virtualLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(), Block.UPDATE_CLIENTS);

			var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
			ui.setSceneSource(source);
			ui.setScene(bakedLevel);
		});
	}
}
