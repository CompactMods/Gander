package dev.compactmods.gander;

import net.minecraft.client.Camera;

import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SceneCamera extends Camera {

	public SceneCamera lookAt(Vector3f from, Vector3f to, Vector3f up) {
		var forward = new Vector3f(to).sub(from);
		forward.normalize();

		new Quaternionf().lookAlong(forward, up, this.rotation());

		this.setPosition(new Vec3(from.x, from.y, from.z));

		return this;
	}
}
