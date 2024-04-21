package dev.compactmods.gander.ponder.element;

import dev.compactmods.gander.ponder.PonderScene;

public interface PonderElement {

	default void tick(PonderScene scene) {}

	default void reset(PonderScene scene) {}
}
