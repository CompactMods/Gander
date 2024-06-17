package dev.compactmods.gander.render.baked.model;

import net.minecraft.client.renderer.block.model.BlockElementRotation;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

final class RotationUtil
{
    private RotationUtil() { }

    public static void rotate(Vector3f coordinate, BlockElementRotation info, Quaternionfc rotation)
    {
        if (info == null) return;

        coordinate.sub(info.origin());
        coordinate.rotate(rotation);

        if (info.rescale())
        {
            // what even is this???
            var scale = Math.abs(info.angle()) == 22.5f
                ? (1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F)
                : (1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F);

            coordinate.mul(scale + 1);
        }

        coordinate.add(info.origin());
    }
}
