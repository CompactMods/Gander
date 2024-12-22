package dev.compactmods.gander.core.utility;

import net.minecraft.core.Direction.Axis;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

public class VecHelper {

	public static Vec3 fromJoml(Vector3f vec) {
		return new Vec3(vec.x, vec.y, vec.z);
	}

	public static Vec3 rotate(Vec3 vec, Vec3 rotationVec) {
		return rotate(vec, rotationVec.x, rotationVec.y, rotationVec.z);
	}

	public static Vec3 rotate(Vec3 vec, double xRot, double yRot, double zRot) {
		return rotate(rotate(rotate(vec, xRot, Axis.X), yRot, Axis.Y), zRot, Axis.Z);
	}

	public static Vec3 rotate(Vec3 vec, double deg, Axis axis) {
		if (deg == 0)
			return vec;
		if (vec == Vec3.ZERO)
			return vec;

		float angle = (float) (deg / 180f * Math.PI);
		double sin = Mth.sin(angle);
		double cos = Mth.cos(angle);
		double x = vec.x;
		double y = vec.y;
		double z = vec.z;

		if (axis == Axis.X)
			return new Vec3(x, y * cos - z * sin, z * cos + y * sin);
		if (axis == Axis.Y)
			return new Vec3(x * cos + z * sin, y, z * cos - x * sin);
		if (axis == Axis.Z)
			return new Vec3(x * cos - y * sin, y * cos + x * sin, z);
		return vec;
	}

	public static Vec3 mirror(Vec3 vec, Mirror mirror) {
		if (mirror == null || mirror == Mirror.NONE)
			return vec;
		if (vec == Vec3.ZERO)
			return vec;

		double x = vec.x;
		double y = vec.y;
		double z = vec.z;

		if (mirror == Mirror.LEFT_RIGHT)
			return new Vec3(x, y, -z);
		if (mirror == Mirror.FRONT_BACK)
			return new Vec3(-x, y, z);
		return vec;
	}

	public static void write(Vec3 vec, FriendlyByteBuf buffer) {
		buffer.writeDouble(vec.x);
		buffer.writeDouble(vec.y);
		buffer.writeDouble(vec.z);
	}

	public static Vec3 read(FriendlyByteBuf buffer) {
		return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	public static Vec3 lerp(float p, Vec3 from, Vec3 to) {
		return from.add(to.subtract(from).scale(p));
	}
}
