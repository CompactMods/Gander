package dev.compactmods.gander.render.baked.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.core.Direction;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static dev.compactmods.gander.render.baked.model.RotationUtil.rotate;

/**
 * A small helper type containing model normals.
 */
record ModelNormals(
    Vector3fc down,
    Vector3fc up,
    Vector3fc north,
    Vector3fc south,
    Vector3fc west,
    Vector3fc east)
{
    public static ModelNormals compute(BlockElement element, Quaternionfc rotation)
    {
        var down = new Vector3f(Direction.DOWN.getNormal().getX(), Direction.DOWN.getNormal().getY(), Direction.DOWN.getNormal().getZ()); // -Y
        var up = new Vector3f(Direction.UP.getNormal().getX(), Direction.UP.getNormal().getY(), Direction.UP.getNormal().getZ()); // +Y
        var north = new Vector3f(Direction.NORTH.getNormal().getX(), Direction.NORTH.getNormal().getY(), Direction.NORTH.getNormal().getZ()); // -Z
        var south = new Vector3f(Direction.SOUTH.getNormal().getX(), Direction.SOUTH.getNormal().getY(), Direction.SOUTH.getNormal().getZ()); // +Z
        var west = new Vector3f(Direction.WEST.getNormal().getX(), Direction.WEST.getNormal().getY(), Direction.WEST.getNormal().getZ()); // -X
        var east = new Vector3f(Direction.EAST.getNormal().getX(), Direction.EAST.getNormal().getY(), Direction.EAST.getNormal().getZ()); // +X

        rotate(down, element.rotation, rotation);
        rotate(up, element.rotation, rotation);
        rotate(north, element.rotation, rotation);
        rotate(south, element.rotation, rotation);
        rotate(west, element.rotation, rotation);
        rotate(east, element.rotation, rotation);

        return new ModelNormals(
            down, up,
            north, south,
            west, east);
    }
}
