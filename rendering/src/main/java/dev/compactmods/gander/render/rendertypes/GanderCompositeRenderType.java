package dev.compactmods.gander.render.rendertypes;

import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.renderer.RenderType;

public interface GanderCompositeRenderType {

	//region Actual code
	RenderType targetingRenderTarget(RenderTarget newTarget, RenderTarget mainTarget);

	final class SecretHandshake {

	}

	SecretHandshake throughTheTunnel = new SecretHandshake();

	static GanderCompositeRenderType of(RenderType type) {
		return ((GanderCompositeRenderType) type);
	}

	RenderType.CompositeState state(); // USA
}
