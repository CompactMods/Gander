package com.simibubi.create.ponder;

import net.minecraft.resources.ResourceLocation;

public class PonderStoryBoardEntry {

	private final PonderStoryBoard board;
	private final String namespace;
	private final ResourceLocation schematicLocation;
	private final ResourceLocation component;

	public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, ResourceLocation schematicLocation, ResourceLocation component) {
		this.board = board;
		this.namespace = namespace;
		this.schematicLocation = schematicLocation;
		this.component = component;
	}

	public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, String schematicPath, ResourceLocation component) {
		this(board, namespace, new ResourceLocation(namespace, schematicPath), component);
	}

	public PonderStoryBoard getBoard() {
		return board;
	}

	public String getNamespace() {
		return namespace;
	}

	public ResourceLocation getSchematicLocation() {
		return schematicLocation;
	}

	public ResourceLocation getComponent() {
		return component;
	}
	// Builder end

}
