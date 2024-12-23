package dev.compactmods.gander.runtime.additions;

import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import dev.compactmods.gander.runtime.baked.ArchetypeBakery;
import dev.compactmods.gander.runtime.baked.AtlasIndex.IndexResult;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Additional reload state attached to {@link ModelManager}.
 *
 * @param archetypeBakingResult The archetype baking result.
 * @param atlasIndices The baked atlas indices.
 * @param blockStateCache The block state cache.
 */
public record ModelManager$ReloadState$Gander(
    ArchetypeBakery.BakingResult archetypeBakingResult,
    Map<ResourceLocation, IndexResult> atlasIndices,
    Map<BlockState, DisplayableMeshGroup> blockStateCache)
{ }
