package dev.compactmods.gander.render.baked.model;

import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.joml.Matrix4fc;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Defines a baked mesh and how it is intended to be rendered.
 */
public record DisplayableMesh(
    ModelResourceLocation name,
    BakedMesh mesh,
    RenderType renderType,
    Matrix4fc transform,
    int weight,
    Function<DisplayableMesh, Set<MaterialInstance>> materialInstanceSupplier)
{
    public Set<MaterialInstance> materialInstances()
    {
        return materialInstanceSupplier().apply(this);
    }
}
