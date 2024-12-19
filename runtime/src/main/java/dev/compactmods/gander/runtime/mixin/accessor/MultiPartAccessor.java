package dev.compactmods.gander.runtime.mixin.accessor;

import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(MultiPart.class)
public interface MultiPartAccessor
{
    //@Accessor
    //StateDefinition<Block, BlockState> getDefinition();

    //@Intrinsic
    //@Accessor
    //List<Selector> getSelectors();
}
