package dev.compactmods.gander.render.pipeline.context;

import net.minecraft.world.level.Level;

public interface LevelRenderingContext<T extends Level> {
    Level level();
}
