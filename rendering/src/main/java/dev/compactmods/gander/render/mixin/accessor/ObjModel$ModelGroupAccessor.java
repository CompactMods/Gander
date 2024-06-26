package dev.compactmods.gander.render.mixin.accessor;

import net.neoforged.neoforge.client.model.obj.ObjModel;
import net.neoforged.neoforge.client.model.obj.ObjModel.ModelObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ObjModel.ModelGroup.class)
public interface ObjModel$ModelGroupAccessor
{
    @Accessor
    Map<String, ModelObject> getParts();
}
