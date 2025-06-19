package dev.compactmods.gander.client.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.level.VirtualLevels;
import net.minecraft.world.phys.AABB;

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
		client.execute(() -> client.setScreen(screen.get()));
	}

	public static void openGanderUI(Consumer<GanderUI> postSetup) {
		var client = Minecraft.getInstance();

		client.execute(() -> {
			var ui = new GanderUI();
			client.setScreen(ui);
			client.execute(() -> postSetup.accept(ui));
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
            var bounds = AABB.of(data.getBoundingBox(new StructurePlaceSettings(), BlockPos.ZERO));

            var virtualLevel = VirtualLevels.containingStructure(data, Minecraft.getInstance().level.registryAccess());
            virtualLevel.addBlockUpdateListener((level) -> {
                level.refreshBlockEntityModels();

                var bakedLevel = LevelBakery.bakeVertices(level, bounds, new Vector3f());
                ui.setScene(bakedLevel);
            });

			var bakedLevel = LevelBakery.bakeVertices(virtualLevel, bounds, new Vector3f());
			ui.setSceneSource(source);
			ui.setScene(bakedLevel);
		});
	}
}
