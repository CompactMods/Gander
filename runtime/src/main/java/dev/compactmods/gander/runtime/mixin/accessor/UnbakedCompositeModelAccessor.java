package dev.compactmods.gander.runtime.mixin.accessor;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.UnbakedCompositeModel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UnbakedCompositeModel.class)
public interface UnbakedCompositeModelAccessor
{
    @Accessor
    ImmutableMap<String, ResourceLocation> getChildren();
}
