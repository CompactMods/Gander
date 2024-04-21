package dev.compactmods.gander.ponder.instruction.contract;

import java.util.function.Consumer;

import dev.compactmods.gander.ponder.PonderScene;

public abstract class PonderInstruction {

	public boolean isBlocking() {
		return false;
	}

	public void reset(PonderScene scene) {}

	public abstract boolean isComplete();

	public void onScheduled(PonderScene scene) {}

	public abstract void tick(PonderScene scene);

	public static PonderInstruction simple(Consumer<PonderScene> callback) {
		return new Simple(callback);
	}

	private static class Simple extends PonderInstruction {

		private final Consumer<PonderScene> callback;

		public Simple(Consumer<PonderScene> callback) {
			this.callback = callback;
		}

		@Override
		public boolean isComplete() {
			return true;
		}

		@Override
		public void tick(PonderScene scene) {
			callback.accept(scene);
		}

	}

}
