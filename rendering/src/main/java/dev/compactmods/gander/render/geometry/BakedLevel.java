package dev.compactmods.gander.render.geometry;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;

import org.joml.Vector3f;

import java.util.Map;

public record BakedLevel(Level originalLevel,
                         AABB blockBoundaries,
                         Map<SectionPos, BakedLevelSection> sections) {


    public void resortTranslucency(Vector3f vector3f) {
        sections.values().forEach(s -> s.resortTranslucency(vector3f));
    }
}
