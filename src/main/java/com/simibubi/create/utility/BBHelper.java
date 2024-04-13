package com.simibubi.create.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class BBHelper {

	public static BoundingBox encapsulate(BoundingBox bb, BlockPos pos) {
		return new BoundingBox(Math.min(bb.minX(), pos.getX()), Math.min(bb.minY(), pos.getY()),
			Math.min(bb.minZ(), pos.getZ()), Math.max(bb.maxX(), pos.getX()), Math.max(bb.maxY(), pos.getY()),
			Math.max(bb.maxZ(), pos.getZ()));
	}

}
