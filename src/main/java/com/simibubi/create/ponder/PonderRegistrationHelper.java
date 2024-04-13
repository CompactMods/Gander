package com.simibubi.create.ponder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public class PonderRegistrationHelper {

	protected String namespace;

	public PonderRegistrationHelper(String namespace) {
		this.namespace = namespace;
	}

	public PonderStoryBoardEntry addStoryBoard(ResourceLocation component, ResourceLocation schematicLocation, PonderStoryBoard storyBoard) {
		PonderStoryBoardEntry entry = PonderStoryBoardEntry.builder(storyBoard)
				.schematicLocation(schematicLocation)
				.component(component)
				.build();

		PonderRegistry.addStoryBoard(entry);
		return entry;
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component, ResourceLocation schematicLocation, PonderStoryBoard storyBoard) {
		return addStoryBoard(component.getId(), schematicLocation, storyBoard);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component, String schematicPath, PonderStoryBoard storyBoard) {
		return addStoryBoard(component, asLocation(schematicPath), storyBoard);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemLike component, String schematicPath, PonderStoryBoard sb) {
		PonderStoryBoardEntry entry = PonderStoryBoardEntry.builder(sb)
				.schematicLocation(namespace, schematicPath)
				.component(component)
				.build();

		PonderRegistry.addStoryBoard(entry);
		return entry;
	}

	public ResourceLocation asLocation(String path) {
		return new ResourceLocation(namespace, path);
	}

}
