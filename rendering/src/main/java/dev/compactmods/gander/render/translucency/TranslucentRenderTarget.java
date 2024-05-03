package dev.compactmods.gander.render.translucency;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Roughly the same as {@link RenderTarget}, but optimised for the multiple
 * layer usage we're doing.
 */
public class TranslucentRenderTarget
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TranslucentRenderTarget.class);

	private static int MAX_SUPPORTED_TEXTURE_LAYERS = -1;
	private static int maxSupportedTextureLayers()
	{
		if (MAX_SUPPORTED_TEXTURE_LAYERS == -1) {
			RenderSystem.assertOnRenderThreadOrInit();
			MAX_SUPPORTED_TEXTURE_LAYERS = GlStateManager._getInteger(GL30.GL_MAX_ARRAY_TEXTURE_LAYERS);
		}

		return MAX_SUPPORTED_TEXTURE_LAYERS;
	}

	private final List<TranslucentRenderTargetLayer> layers;

	private int width;
	private int height;
	private int layerCount;
	private int frameBufferId;
	private int colorTextureId;
	private int depthTextureId;

	public TranslucentRenderTarget()
	{
		layers = new ArrayList<>();
	}

	public int getWidth() { return this.width; }
	public int getHeight() { return this.height; }
	public int getLayerCount() { return this.layerCount; }
	public int getFrameBufferId() { return this.frameBufferId; }
	public int getColorTextureId() { return colorTextureId; }
	public int getDepthTextureId() { return depthTextureId; }

	private void createBuffers(final int width, final int height, final int layerCount, final boolean clearError)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		var maxSize = RenderSystem.maxSupportedTextureSize();
		var maxLayers = maxSupportedTextureLayers();
		RenderSystem.maxSupportedTextureSize();
		if (width <= 0 || width > maxSize || height <= 0 || height > maxSize || layerCount <= 0 || layerCount > maxLayers)
			throw new IllegalArgumentException("Window " + width + "x" + height + "x" + layerCount + " size out of bounds (max. size: " + maxSize + ", max. layers: " + maxLayers + ")");

		this.width = width;
		this.height = height;
		this.layerCount = layerCount;
		this.frameBufferId = GlStateManager.glGenFramebuffers();
		this.colorTextureId = generateColorTexture(width, height, layerCount);
		this.depthTextureId = generateDepthTexture(width, height, layerCount);

		for (int i = 0; i < layerCount; i++)
		{
			layers.add(new TranslucentRenderTargetLayer(this, i));
		}

		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this.frameBufferId);

		GL32.glFramebufferTexture(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, colorTextureId, 0);
		GL32.glFramebufferTexture(GlConst.GL_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, depthTextureId, 0);

		checkStatus();
	}

	private void checkStatus() {
		RenderSystem.assertOnRenderThreadOrInit();
		int i = GlStateManager.glCheckFramebufferStatus(36160);
		if (i != 36053) {
			if (i == 36054) {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
			} else if (i == 36055) {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
			} else if (i == 36059) {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
			} else if (i == 36060) {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
			} else if (i == 36061) {
				throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
			} else if (i == 1285) {
				throw new RuntimeException("GL_OUT_OF_MEMORY");
			} else {
				throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
			}
		}
	}


	public TranslucentRenderTargetLayer getLayer(int layer)
	{
		return layers.get(layer);
	}

	public void resize(final int width, final int height, final int layerCount, final boolean clearError)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		if (frameBufferId >= 0)
		{
			destroyBuffers();
		}

		createBuffers(width, height, layerCount, clearError);
	}

	public void destroyBuffers()
	{
		RenderSystem.assertOnRenderThreadOrInit();

		for (var layer : layers)
		{
			layer.unbind();
		}

		unbindWrite();
		layers.clear();

		if (depthTextureId > 0)
		{
			TextureUtil.releaseTextureId(depthTextureId);
			this.depthTextureId = -1;
		}

		if (colorTextureId > 0)
		{
			TextureUtil.releaseTextureId(colorTextureId);
			this.colorTextureId = -1;
		}

		if (frameBufferId > 0)
		{
			GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
			GlStateManager._glDeleteFramebuffers(this.frameBufferId);
			this.frameBufferId = -1;
		}
	}

	void clear(int layer, boolean clearError)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		bindWrite(layer, true);
		GlStateManager._clearColor(0, 0, 0, 0);
		GlStateManager._clearDepth(1.0);

		GlStateManager._clear(GlConst.GL_COLOR_BUFFER_BIT | GlConst.GL_DEPTH_BUFFER_BIT, clearError);
		unbindWrite();
	}

	void bindRead(int layer)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GL30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, colorTextureId);
	}

	public void unbindRead()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GlStateManager._bindTexture(0);
	}

	void bindWrite(int layer, boolean setViewport)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this.frameBufferId);
		GL32.glFramebufferTextureLayer(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, colorTextureId, 0, layer);
		GL32.glFramebufferTextureLayer(GlConst.GL_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, depthTextureId, 0, layer);
		if (setViewport)
			GlStateManager._viewport(0, 0, width, height);
	}

	public void unbindWrite()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
	}

	private static int generateDepthTexture(final int width, final int height, final int layerCount)
	{
		// TODO: depth-stencil

		// First, we generate a 2D_ARRAY texture (which is basically just a 3D texture in a trenchcoat)
		final var texture = TextureUtil.generateTextureId();
		GL30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, texture);
		GL30.glTexImage3D(
				/* target */ GL30.GL_TEXTURE_2D_ARRAY,
				/* mipmap level */ 0,
				/* internalformat */ GlConst.GL_DEPTH_COMPONENT,
				/* w, h, d */ width, height, layerCount,
				/* border */ 0,
				/* format */ GlConst.GL_DEPTH_COMPONENT,
				/* type */ GlConst.GL_FLOAT,
				/* data */ (IntBuffer)null);

		// Then, we set up its default filtering to be something Minecraft likes
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_COMPARE_MODE, 0);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);

		return texture;
	}

	private static int generateColorTexture(final int width, final int height, final int layerCount)
	{
		// First, we generate a 2D_ARRAY texture (which is basically just a 3D texture in a trenchcoat)
		final var texture = TextureUtil.generateTextureId();
		GL30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, texture);
		GL30.glTexImage3D(
				/* target */ GL30.GL_TEXTURE_2D_ARRAY,
				/* mipmap level */ 0,
				/* internalformat */ GlConst.GL_RGBA8,
				/* w, h, d */ width, height, layerCount,
				/* border */ 0,
				/* format */ GlConst.GL_RGBA,
				/* type */ GlConst.GL_UNSIGNED_BYTE,
				/* data */ (IntBuffer)null);

		// Then, we set up its default filtering to be something Minecraft likes
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_COMPARE_MODE, GL30.GL_NONE);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_WRAP_S, GlConst.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL30.GL_TEXTURE_2D_ARRAY, GlConst.GL_TEXTURE_WRAP_T, GlConst.GL_CLAMP_TO_EDGE);

		return texture;
	}
}
