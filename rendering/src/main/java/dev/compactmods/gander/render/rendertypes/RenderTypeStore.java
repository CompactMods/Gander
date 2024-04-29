package dev.compactmods.gander.render.rendertypes;

import net.minecraft.client.renderer.RenderType;

public interface RenderTypeStore {

	RenderType redirectedRenderType(RenderType desiredType);

	void prepareTranslucency();

	void clear();

	void dispose();

	void processTransclucency(float partialTicks);
}
