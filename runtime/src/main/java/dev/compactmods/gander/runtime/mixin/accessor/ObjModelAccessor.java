package dev.compactmods.gander.runtime.mixin.accessor;

import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import net.neoforged.neoforge.client.model.obj.ObjModel.ModelGroup;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ObjModel.class)
public interface ObjModelAccessor
{
    @Accessor
    Map<String, ModelGroup> getParts();

    @Accessor
    List<Vector3f> getPositions();

    @Accessor
    List<Vec2> getTexCoords();

    @Accessor
    List<Vector3f> getNormals();

    @Accessor
    List<Vector4f> getColors();

    @Accessor
    boolean getFlipV();
}
