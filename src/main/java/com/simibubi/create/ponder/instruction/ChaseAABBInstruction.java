package com.simibubi.create.ponder.instruction;

import com.simibubi.create.ponder.PonderPalette;
import com.simibubi.create.ponder.PonderScene;

import net.minecraft.world.phys.AABB;

public class ChaseAABBInstruction extends TickingInstruction {

	private AABB bb;
	private Object slot;
	private PonderPalette color;

	public ChaseAABBInstruction(PonderPalette color, Object slot, AABB bb, int ticks) {
		super(false, ticks);
		this.color = color;
		this.slot = slot;
		this.bb = bb;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		scene.getOutliner()
			.chaseAABB(slot, bb)
			.lineWidth(0.5f / 16f)
			.colored(color.getColor());
	}

}
