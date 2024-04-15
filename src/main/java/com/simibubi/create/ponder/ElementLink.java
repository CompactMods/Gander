package com.simibubi.create.ponder;

import java.util.UUID;

import com.simibubi.create.ponder.element.PonderElement;

public class ElementLink<T extends PonderElement> {

	private final Class<T> elementClass;
	private final UUID id;

	public ElementLink(Class<T> elementClass) {
		this(elementClass, UUID.randomUUID());
	}

	public ElementLink(Class<T> elementClass, UUID id) {
		this.elementClass = elementClass;
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	public T cast(PonderElement e) {
		return elementClass.cast(e);
	}

}
