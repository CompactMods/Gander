package dev.compactmods.gander.ponder.level;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import dev.compactmods.gander.mixin.accessor.ParticleEngineAccessor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class PonderWorldParticles {

	private final Map<ParticleRenderType, Queue<Particle>> byType = Maps.newIdentityHashMap();
	private final Queue<Particle> queue = Queues.newArrayDeque();
	private final Map<ResourceLocation, ParticleProvider<?>> particleProviders;

	private final TextureManager textureManager;

	PonderLevel world;

	public PonderWorldParticles(PonderLevel world) {
		this.world = world;
		this.textureManager = Minecraft.getInstance().getTextureManager();
		this.particleProviders = ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).create$getProviders();
	}

	public void addParticle(Particle p) {
		this.queue.add(p);
	}

	public void tick() {
		this.byType.forEach((p_228347_1_, p_228347_2_) -> this.tickParticleList(p_228347_2_));

		Particle particle;
		if (queue.isEmpty())
			return;
		while ((particle = this.queue.poll()) != null)
			this.byType.computeIfAbsent(particle.getRenderType(), $ -> EvictingQueue.create(16384))
				.add(particle);
	}

	private void tickParticleList(Collection<Particle> p_187240_1_) {
		if (p_187240_1_.isEmpty())
			return;

		Iterator<Particle> iterator = p_187240_1_.iterator();
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			particle.tick();
			if (!particle.isAlive())
				iterator.remove();
		}
	}

	public void render(PoseStack p_107337_, MultiBufferSource.BufferSource buffers, LightTexture lightTexture, Camera camera, float partialTicks, @Nullable net.minecraft.client.renderer.culling.Frustum clippingHelper) {
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
		RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
		PoseStack posestack = RenderSystem.getModelViewStack();
		posestack.pushPose();
		posestack.mulPoseMatrix(p_107337_.last().pose());
		RenderSystem.applyModelViewMatrix();

		for(ParticleRenderType particlerendertype : byType.keySet()) { // Forge: allow custom IParticleRenderType's
			if (particlerendertype == ParticleRenderType.NO_RENDER) continue;
			Iterable<Particle> iterable = byType.get(particlerendertype);
			if (iterable != null) {
				RenderSystem.setShader(GameRenderer::getParticleShader);
				Tesselator tesselator = Tesselator.getInstance();
				BufferBuilder bufferbuilder = tesselator.getBuilder();
				particlerendertype.begin(bufferbuilder, textureManager);

				for(Particle particle : iterable) {
					if (clippingHelper != null && particle.shouldCull() && !clippingHelper.isVisible(particle.getBoundingBox().inflate(1.0))) continue;
					try {
						particle.render(bufferbuilder, camera, partialTicks);
					} catch (Throwable throwable) {
						CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
						CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
						crashreportcategory.setDetail("Particle", particle::toString);
						crashreportcategory.setDetail("Particle Type", particlerendertype::toString);
						throw new ReportedException(crashreport);
					}
				}

				particlerendertype.end(tesselator);
			}
		}

		posestack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		lightTexture.turnOffLightLayer();
	}

	public void clearEffects() {
		this.byType.clear();
	}

	public ParticleProvider<?> getProvider(ResourceLocation key) {
		return particleProviders.get(key);
	}
}
