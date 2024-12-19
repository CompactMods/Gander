package dev.compactmods.gander.runtime.mixin.accessor;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.parsing.packrat.Atom;

import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ModelManager.class)
public interface ModelManagerAccessor
{
    @Accessor
    Map<ModelResourceLocation, BakedModel> getBakedBlockStateModels();

    @Intrinsic
    @Accessor
    BakedModel getMissingModel();

    @Intrinsic
    @Accessor
    AtomicReference<ModelBakery> getModelBakery();

    @Intrinsic
    @Accessor
    BlockModelShaper getBlockModelShaper();
}
