package dev.compactmods.gander.ponder.element;

import dev.compactmods.gander.ponder.Scene;

public interface PonderElement {

	default void tick(Scene scene) {}

	default void reset(Scene scene) {}
}
