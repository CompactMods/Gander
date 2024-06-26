package dev.compactmods.gander.render.mixin.accessor;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelManager.class)
public interface ModelManagerAccessor
{
    @Accessor
    Map<ModelResourceLocation, BakedModel> getBakedRegistry();

    @Intrinsic
    @Accessor
    BakedModel getMissingModel();

    @Intrinsic
    @Accessor
    ModelBakery getModelBakery();
}
