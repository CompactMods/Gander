package dev.compactmods.gander.render.baked.model;

import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.joml.Matrix4fc;

import java.util.Set;

/**
 * Defines a baked mesh and how it is intended to be rendered.
 */
public record DisplayableMesh(
    ModelResourceLocation name,
    BakedMesh mesh,
    RenderType renderType,
    Matrix4fc transform,
    int weight,
    Set<MaterialInstance> materialInstances)
{ }
