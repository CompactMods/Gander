package dev.compactmods.gander.render.translucency;

import com.mojang.blaze3d.pipeline.RenderTarget;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A translucency chain. Intended to replace {@link PostChain} uses with the
 * "transparency" shader.
 */
public final class TranslucencyChain implements AutoCloseable
{
	private final TranslucencyEffectInstance shader;
	private final RenderTarget screenTarget;
	private final TranslucentRenderTarget layeredTargets;
	private final RenderTarget intermediaryCopyTarget;
	// TODO: toposort?
	private final List<ResourceLocation> renderTargets;

	private final Matrix4f shaderOrthoMatrix;

	public TranslucencyChain(final ResourceLocation shaderPath, final RenderTarget screenTarget, final ResourceProvider resourceProvider) throws IOException
	{
		this.shader = new TranslucencyEffectInstance(resourceProvider, shaderPath.toString());
		this.screenTarget = screenTarget;
		this.layeredTargets = new TranslucentRenderTarget();
		this.intermediaryCopyTarget = new RenderTarget(true){};
		this.renderTargets = new ArrayList<>();
		this.shaderOrthoMatrix = new Matrix4f();
	}

	@Override
	public void close()
	{
		layeredTargets.destroyBuffers();
		shader.close();
	}

	public TranslucencyChain addLayer(final ResourceLocation name)
	{
		// TODO: better exception
		if (renderTargets.contains(name))
		{
			throw new IllegalArgumentException("Already registered");
		}

		renderTargets.add(name);

		return this;
	}

	public void resize(int width, int height) {
		this.shaderOrthoMatrix.setOrtho(0, (float)width, 0, (float)height, 0.1f, 1000.0f);

		layeredTargets.resize(width, height, renderTargets.size(), Minecraft.ON_OSX);
		intermediaryCopyTarget.resize(width, height, Minecraft.ON_OSX);
	}

	public void prepareLayer(ResourceLocation layer)
	{
		// Takes the depth and stencil data in the current layer and copies it to the next layer
		var target = getRenderTarget(layer);
		if (target.getLayer() == 0) return; // if we're layer 0, there's nothing to copy from

		var source = layeredTargets.getLayer(target.getLayer() - 1);

		// N.B. We're avoiding going through Blaze3D state management here

		// Blit to the intermediary...
		GL32.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, intermediaryCopyTarget.frameBufferId);
		GL32.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, layeredTargets.getFrameBufferId());
		GL32.glFramebufferTextureLayer(GlConst.GL_READ_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, layeredTargets.getColorTextureId(), 0, source.getLayer());
		GL32.glFramebufferTextureLayer(GlConst.GL_READ_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, layeredTargets.getDepthTextureId(), 0, source.getLayer());
		GL32.glBlitFramebuffer(
				0, 0, layeredTargets.getWidth(), layeredTargets.getHeight(),
				0, 0, intermediaryCopyTarget.width, intermediaryCopyTarget.height,
				GlConst.GL_DEPTH_BUFFER_BIT, // | GL11.GL_STENCIL_BUFFER_BIT,
				GlConst.GL_NEAREST);

		// And then to the target.
		GL32.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, layeredTargets.getFrameBufferId());
		GL32.glFramebufferTextureLayer(GlConst.GL_DRAW_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, layeredTargets.getColorTextureId(), 0, target.getLayer());
		GL32.glFramebufferTextureLayer(GlConst.GL_DRAW_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, layeredTargets.getDepthTextureId(), 0, target.getLayer());
		GL32.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, intermediaryCopyTarget.frameBufferId);
		GL32.glBlitFramebuffer(
				0, 0, intermediaryCopyTarget.width, intermediaryCopyTarget.height,
				0, 0, layeredTargets.getWidth(), layeredTargets.getHeight(),
				GlConst.GL_DEPTH_BUFFER_BIT, // | GL11.GL_STENCIL_BUFFER_BIT,
				GlConst.GL_NEAREST);

		GL32.glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
	}

	public void clear()
	{
		//layeredTargets.bindWrite
		for (int layer = 0; layer < layeredTargets.getLayerCount(); layer++)
		{
			var target = layeredTargets.getLayer(layer);
			target.clear(Minecraft.ON_OSX);
		}
	}

	public void process()
	{
		screenTarget.unbindWrite();
		RenderSystem.viewport(0, 0, layeredTargets.getWidth(), layeredTargets.getHeight());
		this.shader.setSampler("InputLayersColorSampler", layeredTargets::getColorTextureId, GL32.GL_TEXTURE_2D_ARRAY);
		this.shader.setSampler("InputLayersDepthSampler", layeredTargets::getDepthTextureId, GL32.GL_TEXTURE_2D_ARRAY);
		this.shader.safeGetUniform("InputLayerCount").set(layeredTargets.getLayerCount());
		this.shader.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
		this.shader.safeGetUniform("OutSize").set((float)layeredTargets.getWidth(), (float)layeredTargets.getHeight());

		final var mc = Minecraft.getInstance();
		this.shader.apply();

		this.screenTarget.clear(Minecraft.ON_OSX);
		this.screenTarget.bindWrite(false);

		RenderSystem.depthFunc(GlConst.GL_ALWAYS);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		bufferbuilder.vertex(0.0, 0.0, 500.0).endVertex();
		bufferbuilder.vertex(layeredTargets.getWidth(), 0.0, 500.0).endVertex();
		bufferbuilder.vertex(layeredTargets.getWidth(), layeredTargets.getHeight(), 500.0).endVertex();
		bufferbuilder.vertex(0.0, layeredTargets.getHeight(), 500.0).endVertex();
		BufferUploader.draw(bufferbuilder.end());
		RenderSystem.depthFunc(GlConst.GL_LEQUAL);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		this.shader.clear();
		this.screenTarget.unbindWrite();
		this.layeredTargets.unbindRead();
	}

	public TranslucentRenderTargetLayer getRenderTarget(ResourceLocation location)
	{
		var layer = renderTargets.indexOf(location);
		if (layer < 0 || layer >= layeredTargets.getLayerCount())
			throw new IllegalArgumentException("Unknown layer " + location);

		return layeredTargets.getLayer(layer);
	}
}
