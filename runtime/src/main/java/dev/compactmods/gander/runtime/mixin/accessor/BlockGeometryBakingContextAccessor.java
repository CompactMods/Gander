package dev.compactmods.gander.runtime.mixin.accessor;

import net.neoforged.neoforge.client.model.geometry.BlockGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockGeometryBakingContext.class)
public interface BlockGeometryBakingContextAccessor
{
    @Accessor("customGeometry")
    IUnbakedGeometry<?> getOwnCustomGeometry();
}
