package dev.compactmods.gander.runtime.mixin.accessor;

import com.mojang.math.Transformation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Transformation.class)
public interface TransformationAccessor
{
    @Accessor("matrix")
    @Intrinsic
    Matrix4f gander$matrix();
}
