package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderPalette;
import dev.compactmods.gander.ponder.PonderScene;

import dev.compactmods.gander.ponder.instruction.contract.TickingInstruction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HighlightValueBoxInstruction extends TickingInstruction {

	private final Vec3 vec;
	private final Vec3 expands;

	public HighlightValueBoxInstruction(Vec3 vec, Vec3 expands, int duration) {
		super(false, duration);
		this.vec = vec;
		this.expands = expands;
	}

	@Override
	public void tick(PonderScene scene) {
		super.tick(scene);
		AABB point = new AABB(vec, vec);
		AABB expanded = point.inflate(expands.x, expands.y, expands.z);
		scene.getOutliner()
			.chaseAABB(vec, remainingTicks + 1 >= totalTicks ? point : expanded)
			.lineWidth(1 / 15f)
			.colored(PonderPalette.WHITE.getColor());
	}

}
