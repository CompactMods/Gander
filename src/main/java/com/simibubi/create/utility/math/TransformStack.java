package com.simibubi.create.utility.math;

public interface TransformStack<Self extends TransformStack<Self>> {

	Self pushPose();

	Self popPose();
}
