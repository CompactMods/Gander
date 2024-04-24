package dev.compactmods.gander.utility;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class AngleHelper {

	public static float horizontalAngle(Direction facing) {
		if (facing.getAxis().isVertical())
			return 0;
		float angle = facing.toYRot();
		if (facing.getAxis() == Axis.X)
			angle = -angle;
		return angle;
	}
}
