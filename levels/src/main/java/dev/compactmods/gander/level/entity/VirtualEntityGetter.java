package dev.compactmods.gander.level.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class VirtualEntityGetter implements EntityGetter {

	public VirtualEntityGetter() {
	}

	@Override
	public List<Entity> getEntities(@Nullable Entity pEntity, AABB pArea, Predicate<? super Entity> pPredicate) {
		return List.of();
	}

	@Override
	public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate) {
		return List.of();
	}

	@Override
	public List<? extends Player> players() {
		return List.of();
	}

	/**
	 * This is on {@link Level} instead of {@link EntityGetter} for some reason. Mojang why.
	 *
	 * @param pId Entity id.
	 * @return
	 */
	public Entity getEntity(int pId) {
		return null;
	}
}
