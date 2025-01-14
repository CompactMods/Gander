package dev.compactmods.gander.render.rendertypes;

import dev.compactmods.gander.render.translucency.TranslucentRenderTargetLayer;
import net.minecraft.client.renderer.RenderType;

public interface GanderCompositeRenderType {

	//region Actual code
	RenderType targetingTranslucentRenderTarget(TranslucentRenderTargetLayer newTarget, TranslucentRenderTargetLayer mainTarget);

	static GanderCompositeRenderType of(RenderType type) {
		return ((GanderCompositeRenderType) type);
	}

	RenderType.CompositeState state(); // USA
}
