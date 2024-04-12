package com.simibubi.create.foundation.utility;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class Iterate {

	public static final boolean[] trueAndFalse = {true, false};
	public static final int[] zeroAndOne = {0, 1};
	public static final Direction[] directions = Direction.values();
	public static final Direction[] horizontalDirections = getHorizontals();
	public static final Axis[] axes = Axis.values();

	private static Direction[] getHorizontals() {
		return Direction.Plane.HORIZONTAL
				.stream()
				.toArray(Direction[]::new);
	}
}
