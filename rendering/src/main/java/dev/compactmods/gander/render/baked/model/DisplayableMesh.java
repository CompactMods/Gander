package dev.compactmods.gander.render.baked.model;

import com.google.common.collect.Multimap;
import com.mojang.math.Transformation;
import dev.compactmods.gander.render.baked.model.material.MaterialInstance;
import dev.compactmods.gander.render.baked.model.material.MaterialParent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;

import java.util.function.Supplier;

/**
 * Defines a baked mesh and how it is intended to be rendered.
 */
public record DisplayableMesh(
    ModelResourceLocation name,
    BakedMesh mesh,
    RenderType renderType,
    // TODO: is it worth using a Translation here, or should we extract the
    //  components directly?
    Transformation transform,
    int weight,
    Supplier<Multimap<MaterialParent, MaterialInstance>> materialInstanceSupplier)
{
    public Multimap<MaterialParent, MaterialInstance> materialInstances()
    {
        return materialInstanceSupplier().get();
    }
}
