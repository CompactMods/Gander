package dev.compactmods.gander.core.camera;

import net.minecraft.client.Camera;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class SceneCamera extends Camera {
	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private float zoomFactor;

	private final Vector3f lookFrom;
	private final Vector3f lookTarget;
	private final Vector2f cameraRotation;
	public static final Vector2f DEFAULT_ROTATION = new Vector2f((float) Math.toRadians(-25), (float) Math.toRadians(-135));

	public SceneCamera() {
		this.cameraRotation = new Vector2f(DEFAULT_ROTATION);
		this.lookFrom = new Vector3f();
		this.lookTarget = new Vector3f();
		this.zoomFactor = 1;
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

	public void zoom(float factor) {
		this.zoomFactor += factor;
		this.recalcLook();
	}

	private void recalcLook() {
		lookFrom.set(0, 0, -1);
		lookFrom.rotateX(cameraRotation.x);
		lookFrom.rotateY(cameraRotation.y);
		lookFrom.mul(zoomFactor);

//		if (lookFrom.distance(newLookFrom) > 1 && bakedLevel != null)
//			bakedLevel.resortTranslucency(newLookFrom);

		var forward = new Vector3f(lookTarget).sub(lookFrom);
		forward.normalize();

		new Quaternionf().lookAlong(forward, UP, this.rotation());

		this.setPosition(new Vec3(lookFrom.x, lookFrom.y, lookFrom.z));
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
