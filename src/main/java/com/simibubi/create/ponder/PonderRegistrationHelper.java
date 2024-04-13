package com.simibubi.create.ponder;

import java.util.Arrays;
import java.util.function.Consumer;

import com.tterrag.registrate.util.entry.ItemProviderEntry;

import net.minecraft.resources.ResourceLocation;

public class PonderRegistrationHelper {

	protected String namespace;

	public PonderRegistrationHelper(String namespace) {
		this.namespace = namespace;
	}

	public PonderStoryBoardEntry addStoryBoard(ResourceLocation component, ResourceLocation schematicLocation, PonderStoryBoard storyBoard) {
		PonderStoryBoardEntry entry = this.createStoryBoardEntry(storyBoard, schematicLocation, component);
		PonderRegistry.addStoryBoard(entry);
		return entry;
	}

	public PonderStoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath, PonderStoryBoard storyBoard) {
		return addStoryBoard(component, asLocation(schematicPath), storyBoard);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component, ResourceLocation schematicLocation, PonderStoryBoard storyBoard) {
		return addStoryBoard(component.getId(), schematicLocation, storyBoard);
	}

	public PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component, String schematicPath, PonderStoryBoard storyBoard) {
		return addStoryBoard(component, asLocation(schematicPath), storyBoard);
	}

	public MultiSceneBuilder forComponents(ItemProviderEntry<?>... components) {
		return new MultiSceneBuilder(Arrays.asList(components));
	}

	public MultiSceneBuilder forComponents(Iterable<? extends ItemProviderEntry<?>> components) {
		return new MultiSceneBuilder(components);
	}

	public PonderStoryBoardEntry createStoryBoardEntry(PonderStoryBoard storyBoard, ResourceLocation schematicLocation, ResourceLocation component) {
		return new PonderStoryBoardEntry(storyBoard, namespace, schematicLocation, component);
	}

	public PonderStoryBoardEntry createStoryBoardEntry(PonderStoryBoard storyBoard, String schematicPath, ResourceLocation component) {
		return createStoryBoardEntry(storyBoard, asLocation(schematicPath), component);
	}

	public ResourceLocation asLocation(String path) {
		return new ResourceLocation(namespace, path);
	}

	public class MultiSceneBuilder {

		protected Iterable<? extends ItemProviderEntry<?>> components;

		protected MultiSceneBuilder(Iterable<? extends ItemProviderEntry<?>> components) {
			this.components = components;
		}

		public MultiSceneBuilder addStoryBoard(ResourceLocation schematicLocation, PonderStoryBoard storyBoard) {
			return addStoryBoard(schematicLocation, storyBoard, e -> {});
		}

		public MultiSceneBuilder addStoryBoard(ResourceLocation schematicLocation, PonderStoryBoard storyBoard, Consumer<PonderStoryBoardEntry> extras) {
			components.forEach(c -> extras.accept(PonderRegistrationHelper.this.addStoryBoard(c, schematicLocation, storyBoard)));
			return this;
		}

		public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard) {
			return addStoryBoard(asLocation(schematicPath), storyBoard);
		}

		public MultiSceneBuilder addStoryBoard(String schematicPath, PonderStoryBoard storyBoard, Consumer<PonderStoryBoardEntry> extras) {
			return addStoryBoard(asLocation(schematicPath), storyBoard, extras);
		}

	}

}
