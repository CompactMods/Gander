package dev.compactmods.gander.utility.math;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

public class PoseTransformStack implements Translate<PoseTransformStack>, Rotate<PoseTransformStack> {

	private final PoseStack stack;

	private PoseTransformStack(PoseStack stack) {
		this.stack = stack;
	}

	public static PoseTransformStack of(PoseStack stack) {
		return new PoseTransformStack(stack);
	}

	public PoseTransformStack rotate(Quaternionf quaternion) {
		stack.mulPose(quaternion);
		return this;
	}

	public PoseTransformStack scale(float factorX, float factorY, float factorZ) {
		stack.scale(factorX, factorY, factorZ);
		return this;
	}

	public PoseTransformStack pushPose() {
		stack.pushPose();
		return this;
	}

	public PoseTransformStack popPose() {
		stack.popPose();
		return this;
	}

	public PoseTransformStack translate(double x, double y, double z) {
		stack.translate(x, y, z);
		return this;
	}
}
