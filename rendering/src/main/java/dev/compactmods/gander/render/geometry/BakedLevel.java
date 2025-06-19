package dev.compactmods.gander.render.geometry;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import java.util.Map;

public record BakedLevel(Level originalLevel,
                         AABB blockBoundaries,
                         Map<Long, BakedLevelSection> sections) {

}
