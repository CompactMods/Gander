package dev.compactmods.gander.render.baked.model;

import net.minecraft.client.renderer.block.model.BlockElementRotation;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class RotationUtil
{
    private RotationUtil() { }

    public static void rotate(
        Vector3f coordinate,
        BlockElementRotation info,
        Quaternionfc rotation)
    {
        if (info == null) return;

        coordinate.sub(info.origin());
        coordinate.rotate(rotation);

        // Vanilla does some wacky stuff if the rescale parameter is passed.
        // This mimics that logic.
        if (info.rescale())
        {
            // There's two different scale factors based on the angle.
            // Presumably, they only support 22.5 and 45 degree angles.
            var scale = Math.abs(info.angle()) == 22.5f
                ? (1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F)
                : (1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F);

            switch (info.axis())
            {
                case X ->
                {
                    coordinate.mul(1, 1 + scale, 1 + scale);
                }
                case Y ->
                {
                    coordinate.mul(1 + scale, 1, 1 + scale);
                }
                case Z ->
                {
                    coordinate.mul(1 + scale, 1 + scale, 1);
                }
            }
        }

        coordinate.add(info.origin());
    }
}
