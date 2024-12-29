package dev.compactmods.gander.render.translucency;

import java.util.List;
import java.util.Set;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.translucency.shader.TranslucencyEffectInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/**
 * A translucency chain. Intended to replace {@link PostChain} uses with the
 * "transparency" shader.
 */
public final class TranslucencyChain implements AutoCloseable
{
	private final TranslucencyEffectInstance shader;
	private final RenderTarget screenTarget;
	private final List<ResourceLocation> renderTargets;

	private final TranslucentRenderTarget layeredTargets;
	private final RenderTarget intermediaryCopyTarget;
	private final Matrix4f shaderOrthoMatrix;

	public static TranslucencyChainBuilder builder()
	{
		return new TranslucencyChainBuilder();
	}

	TranslucencyChain(
		final RenderTarget screenTarget,
		final List<ResourceLocation> layers)
	{
		this.shader = new TranslucencyEffectInstance(layers.size());
		this.screenTarget = screenTarget;
		this.renderTargets = layers;

		this.layeredTargets = new TranslucentRenderTarget();
		this.intermediaryCopyTarget = new RenderTarget(true){};
		this.shaderOrthoMatrix = new Matrix4f();
	}

	@Override
	public void close()
	{
		layeredTargets.destroyBuffers();
		shader.close();
	}

	public void resize(int width, int height) {
		this.shaderOrthoMatrix.setOrtho(0, (float)width, 0, (float)height, 0.1f, 1000.0f);

		layeredTargets.resize(width, height, renderTargets.size(), Minecraft.ON_OSX);
		intermediaryCopyTarget.resize(width, height, Minecraft.ON_OSX);
	}

	public void prepareBackgroundColor(RenderTarget copyFrom)
	{ }

	public void prepareLayer(ResourceLocation layer)
	{
		// Takes the depth data in the current layer and copies it to the next layer
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
				GlConst.GL_DEPTH_BUFFER_BIT,
				GlConst.GL_NEAREST);

		// And then to the target.
		GL32.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, layeredTargets.getFrameBufferId());
		GL32.glFramebufferTextureLayer(GlConst.GL_DRAW_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, layeredTargets.getColorTextureId(), 0, target.getLayer());
		GL32.glFramebufferTextureLayer(GlConst.GL_DRAW_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, layeredTargets.getDepthTextureId(), 0, target.getLayer());
		GL32.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, intermediaryCopyTarget.frameBufferId);
		GL32.glBlitFramebuffer(
				0, 0, intermediaryCopyTarget.width, intermediaryCopyTarget.height,
				0, 0, layeredTargets.getWidth(), layeredTargets.getHeight(),
				GlConst.GL_DEPTH_BUFFER_BIT,
				GlConst.GL_NEAREST);

		GL32.glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
	}

	public void clear()
	{
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
		this.shader.setColorSampler(layeredTargets.getColorTextureId());
		this.shader.setDepthSampler(layeredTargets.getDepthTextureId());
		this.shader.setProjectionMatrix(this.shaderOrthoMatrix);
		this.shader.setOutputSize(layeredTargets.getWidth(), layeredTargets.getHeight());

		final var mc = Minecraft.getInstance();
		this.shader.apply();

		this.screenTarget.clear(Minecraft.ON_OSX);
		this.screenTarget.bindWrite(false);

		RenderSystem.depthFunc(GlConst.GL_ALWAYS);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		bufferbuilder.addVertex(0.0F, 0.0F, 500.0F);
		bufferbuilder.addVertex(layeredTargets.getWidth(), 0.0F, 500.0F);
		bufferbuilder.addVertex(layeredTargets.getWidth(), layeredTargets.getHeight(), 500.0F);
		bufferbuilder.addVertex(0.0F, layeredTargets.getHeight(), 500.0F);
		BufferUploader.draw(bufferbuilder.buildOrThrow());
		RenderSystem.depthFunc(GlConst.GL_LEQUAL);
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
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

    public Set<ResourceLocation> layers() {
        return Set.copyOf(renderTargets);
    }
}
