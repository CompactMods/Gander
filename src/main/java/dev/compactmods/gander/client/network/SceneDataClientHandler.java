package dev.compactmods.gander.client.network;

import dev.compactmods.gander.ponder.Scene;
import dev.compactmods.gander.ponder.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SceneDataClientHandler {


	public static void loadScene(StructureTemplate data) {
		final var mc = Minecraft.getInstance();
		if (mc.screen instanceof PonderUI ui) {
			var scene = new Scene(data);
			ui.setScene(scene);
		}
	}
}
