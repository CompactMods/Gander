package dev.compactmods.gander.level.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.world.level.gameevent.GameEvent;

import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public interface GanderEmptyLevelAccessor extends LevelAccessor {

    default void levelEvent(@Nullable Entity entity, int i, BlockPos blockPos, int i1) {

    }

    default void gameEvent(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context) {

    }
}
