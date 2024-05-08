package dev.compactmods.gander.level.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;

public class VirtualEntitySystem {


	private final VirtualEntityGetter entityGetter;

	public VirtualEntitySystem() {
		this.entityGetter = new VirtualEntityGetter();
	}

	public EntityGetter entityGetter() {
		return entityGetter;
	}

	public Entity getEntity(int pId) {
		return null;
	}
}
