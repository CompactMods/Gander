package dev.compactmods.gander.render.translucency;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Roughly the same as {@link RenderTarget}, but optimised for the multiple
 * layer usage we're doing.
 */
public class TranslucentRenderTargetLayer
{
	private TranslucentRenderTarget renderTarget;
	private int layer;

	TranslucentRenderTargetLayer(final TranslucentRenderTarget translucentRenderTarget, final int layer)
	{
		this.renderTarget = translucentRenderTarget;
		this.layer = layer;
	}

	public int getLayer() { return this.layer; }

	void unbind()
	{
		this.renderTarget = null;
		this.layer = -1;
	}

	public void bindRead()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		renderTarget.bindRead(layer);
	}

	public void unbindRead()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		renderTarget.unbindRead();
	}

	public void bindWrite(boolean setViewport)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		renderTarget.bindWrite(layer, setViewport);
	}

	public void unbindWrite()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		renderTarget.unbindWrite();
	}

	public void clear()
	{
		renderTarget.clear(layer);
	}
}
