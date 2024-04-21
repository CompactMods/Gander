package dev.compactmods.gander.ponder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.ItemLike;

public class PonderStoryBoardEntry {

	private final PonderStoryBoard board;
	private final ResourceLocation schematicLocation;
	private final ResourceLocation id;

	protected PonderStoryBoardEntry(PonderStoryBoard board, ResourceLocation schematicLocation, ResourceLocation id) {
		this.board = board;
		this.schematicLocation = schematicLocation;
		this.id = id;
	}

	public static Builder builder(PonderStoryBoard board) {
		return new Builder(board);
	}

	public PonderStoryBoard getBoard() {
		return board;
	}

	public ResourceLocation getSchematicLocation() {
		return schematicLocation;
	}

	public ResourceLocation id() {
		return id;
	}
	// Builder end

	public static class Builder {
		private final PonderStoryBoard board;
		private ResourceLocation schematicLocation;
		private ResourceLocation component;

		public Builder(PonderStoryBoard board) {
			this.board = board;
			this.schematicLocation = new ResourceLocation("minecraft", "empty");
		}

		public Builder schematicLocation(ResourceLocation schematicLocation) {
			this.schematicLocation = schematicLocation;
			return this;
		}

		public Builder schematicLocation(String schematicPath) {
			this.schematicLocation = new ResourceLocation(schematicLocation.getNamespace(), schematicPath);
			return this;
		}

		public Builder schematicLocation(String namespace, String path) {
			this.schematicLocation = new ResourceLocation(namespace, path);
			return this;
		}

		public Builder component(ItemLike component) {
			this.component = BuiltInRegistries.ITEM.getKey(component.asItem());
			return this;
		}

		public Builder component(ResourceLocation component) {
			this.component = component;
			return this;
		}

		public PonderStoryBoardEntry build() {
			return new PonderStoryBoardEntry(board, schematicLocation, component);
		}
	}
}
