package dev.compactmods.gander;

import net.minecraft.client.Camera;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SceneCamera extends Camera {

	private Vector3f lookFrom;
	private final Vector3f lookTarget;

	private final Vector2f cameraRotation;
	public static final Vector2f DEFAULT_ROTATION = new Vector2f((float) Math.toRadians(-25), (float) Math.toRadians(-135));

	public SceneCamera() {
		this.cameraRotation = new Vector2f(DEFAULT_ROTATION);
		this.lookFrom = new Vector3f();
		this.lookTarget = new Vector3f();
		this.recalcLook();
	}

	public Vector3f getLookFrom() {
		return new Vector3f(lookFrom);
	}

	public void resetLook() {
		this.cameraRotation.set(DEFAULT_ROTATION);
		this.recalcLook();
	}

	public void lookUp(float amount) {
		if (this.cameraRotation.x < -amount) {
			this.cameraRotation.x += amount;
			this.recalcLook();
		}
	}

	public void lookDown(float amount) {
		if (this.cameraRotation.x > -(Math.PI / 2) + (amount * 2)) {
			this.cameraRotation.x -= amount;
			this.recalcLook();
		}
	}

	public void lookLeft(float amount) {
		this.cameraRotation.y += amount;
		this.recalcLook();
	}

	public void lookRight(float amount) {
		this.cameraRotation.y -= amount;
		this.recalcLook();
	}

	private void recalcLook() {
		var newLookFrom = new Vector3f(0, 0, 1);
		newLookFrom.rotateX(cameraRotation.x);
		newLookFrom.rotateY(cameraRotation.y);

//		if (lookFrom.distance(newLookFrom) > 1 && bakedLevel != null)
//			bakedLevel.resortTranslucency(newLookFrom);

		this.lookFrom = newLookFrom;
		lookAt(this.lookFrom, lookTarget, new Vector3f(0, 1, 0));
	}

	public SceneCamera lookAt(Vector3f from, Vector3f to, Vector3f up) {
		var forward = new Vector3f(to).sub(from);
		forward.normalize();

		new Quaternionf().lookAlong(forward, up, this.rotation());

		this.setPosition(new Vec3(from.x, from.y, from.z));

		return this;
	}

	public void lookDirection(Direction direction) {
		switch (direction) {
			case UP:
				this.cameraRotation.set(0, Math.PI);
				break;

			case DOWN:
				this.cameraRotation.set(Math.PI / 2, Math.PI);
				break;

			case NORTH:
				this.cameraRotation.set(0, Math.PI);
				break;

			case SOUTH:
				this.cameraRotation.set(Math.PI / 2, -Math.PI);
				break;

			case WEST:
				break;

			case EAST:
				break;
		}

		this.recalcLook();
	}
}
