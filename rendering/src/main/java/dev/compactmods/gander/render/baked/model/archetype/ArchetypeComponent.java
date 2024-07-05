package dev.compactmods.gander.render.baked.model.archetype;

import dev.compactmods.gander.render.baked.BakedMesh;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ArchetypeComponent(
    ModelResourceLocation name,
    BakedMesh bakedMesh,
    @Nullable
    ResourceLocation renderType, boolean isVisible)
{ }