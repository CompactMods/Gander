package dev.compactmods.gander.ponder;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class SceneBuilder {

	final StructureTemplate template;

	private SceneBuilder(StructureTemplate template) {
		this.template = template;
	}

	public static SceneBuilder empty() {
		return new SceneBuilder(new StructureTemplate());
	}

	public static SceneBuilder forTemplate(ResourceLocation templateID) {
		final var templateManager = ServerLifecycleHooks.getCurrentServer().getStructureManager();
		return templateManager.get(templateID)
				.map(SceneBuilder::forTemplate)
				.orElse(SceneBuilder.empty());
	}

	public static SceneBuilder forTemplate(StructureTemplate template) {
		return new SceneBuilder(template);
	}

	public Scene build() {
		return new Scene(template);
	}
}
