package dev.compactmods.gander.ponder.instruction;

import dev.compactmods.gander.ponder.PonderScene;

import dev.compactmods.gander.ponder.instruction.contract.PonderInstruction;
import net.minecraft.world.phys.Vec3;

public class MovePoiInstruction extends PonderInstruction {

	private final Vec3 poi;

	public MovePoiInstruction(Vec3 poi) {
		this.poi = poi;
	}

	@Override
	public boolean isComplete() {
		return true;
	}

	@Override
	public void tick(PonderScene scene) {
		scene.setPointOfInterest(poi);
	}

}
