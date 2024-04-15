package dev.compactmods.gander.utility.math;

import net.minecraft.core.Vec3i;

import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

public interface Translate<Self extends Translate<Self>> {
	float CENTER = 0.5f;

	Self translate(double x, double y, double z);

	default Self translate(double v) {
		return translate(v, v, v);
	}

	default Self translate(Vec3i vec) {
		return translate(vec.getX(), vec.getY(), vec.getZ());
	}

	default Self translate(Vector3f vec) {
		return translate(vec.x(), vec.y(), vec.z());
	}

	default Self translate(Vec3 vec) {
		return translate(vec.x, vec.y, vec.z);
	}

	default Self translateBack(double x, double y, double z) {
		return translate(-x, -y, -z);
	}

	default Self translateBack(Vec3 vec) {
		return translateBack(vec.x, vec.y, vec.z);
	}

	default Self center() {
		return translate(CENTER);
	}

	default Self uncenter() {
		return translate(-CENTER);
	}
}
