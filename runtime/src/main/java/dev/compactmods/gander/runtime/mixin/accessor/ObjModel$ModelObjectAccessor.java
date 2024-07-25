package dev.compactmods.gander.runtime.mixin.accessor;

import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ObjModel.ModelObject.class)
public interface ObjModel$ModelObjectAccessor
{
    @Accessor
    String getName();

    @Accessor
    List<ObjModel$ModelMeshAccessor> getMeshes();
}
