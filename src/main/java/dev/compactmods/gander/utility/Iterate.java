package dev.compactmods.gander.utility;

import net.minecraft.core.Direction;

public class Iterate {

	public static final Direction[] directions = Direction.values();
	public static final Direction[] horizontalDirections = getHorizontals();

	private static Direction[] getHorizontals() {
		return Direction.Plane.HORIZONTAL
				.stream()
				.toArray(Direction[]::new);
	}
}
