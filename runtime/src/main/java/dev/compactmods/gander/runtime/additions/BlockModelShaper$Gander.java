package dev.compactmods.gander.runtime.additions;

import dev.compactmods.gander.render.baked.model.DisplayableMeshGroup;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public interface BlockModelShaper$Gander
{
    void gander$replaceCache(Map<BlockState, DisplayableMeshGroup> modelCache);

    DisplayableMeshGroup gander$getDisplayableMesh(BlockState state);
}
