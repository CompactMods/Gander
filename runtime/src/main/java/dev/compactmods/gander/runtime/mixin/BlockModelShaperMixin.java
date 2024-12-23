package dev.compactmods.gander.runtime.mixin;

import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import dev.compactmods.gander.runtime.additions.BlockModelShaper$Gander;
import net.minecraft.client.renderer.block.BlockModelShaper;

import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(BlockModelShaper.class)
public class BlockModelShaperMixin implements BlockModelShaper$Gander
{
    @Unique
    private Map<BlockState, DisplayableMeshGroup> gander$meshGroupByStateCache;

    @Override
    public void gander$replaceCache(Map<BlockState, DisplayableMeshGroup> modelCache)
    {
        gander$meshGroupByStateCache = modelCache;
    }

    @Override
    public DisplayableMeshGroup gander$getDisplayableMesh(BlockState state)
    {
        var group = gander$meshGroupByStateCache.get(state);

        if (group == null)
            // TODO: this should use the missing model
            group = null;

        return group;
    }
}
