package dev.compactmods.gander.render.mixin.accessor;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.neoforged.neoforge.client.model.CompositeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompositeModel.class)
public interface CompositeModelAccessor
{
    @Accessor
    ImmutableMap<String, BlockModel> getChildren();
}
