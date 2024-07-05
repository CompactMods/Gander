package dev.compactmods.gander.render.baked.model;

import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.BakedMesh;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Defines a baked mesh and how it is intended to be rendered.
 */
public record DisplayableMesh(
    ModelResourceLocation name,
    BakedMesh mesh,
    @Nullable
    ResourceLocation renderType,
    // TODO: decompose this back into quaternion?
    Transformation transform,
    int weight)
{ }
