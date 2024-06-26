package dev.compactmods.gander.render.mixin.accessor;

import net.neoforged.neoforge.client.model.obj.ObjMaterialLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(targets = "net/neoforged/neoforge/client/model/obj/ObjModel$ModelMesh")
public interface ObjModel$ModelMeshAccessor
{
    @Accessor
    ObjMaterialLibrary.Material getMat();
    @Accessor
    String getSmoothingGroup();
    @Accessor
    List<int[][]> getFaces();
}
