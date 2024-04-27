package dev.compactmods.gander.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ScreenBlockRenderer {

	public static void maybePrepareTranslucency(RenderType type) {
		Minecraft mc = Minecraft.getInstance();

		var transTarget = mc.levelRenderer.getTranslucentTarget();
		boolean isTranslucent = type == RenderType.translucent();
		var chain = Minecraft.getInstance().levelRenderer.transparencyChain;

		if (isTranslucent) {
			if (transTarget != null)
				transTarget.clear(Minecraft.ON_OSX);

			if (chain != null) {
				transTarget.copyDepthFrom(mc.getMainRenderTarget());
			}
		}
	}

	public static void render(BakedLevel bakedLevel, PoseStack poseStack, Vector3f cameraPosition, Matrix4f projectionMatrix, float partialTicks) {
		poseStack.pushPose();
		float scale = (1 / 16f);
		// poseStack.scale(scale, scale, scale);

		RenderTypeHelper.RENDER_TYPE_BUFFERS.keySet().forEach(renderType -> {
			maybePrepareTranslucency(renderType);

			renderSectionLayer(bakedLevel, renderType, poseStack, cameraPosition, projectionMatrix);
		});

		processTranslucency(partialTicks);

		poseStack.popPose();
	}

	public static void processTranslucency(float partialTicks) {
		var chain = Minecraft.getInstance().levelRenderer.transparencyChain;
		if (chain != null)
			chain.process(partialTicks);
	}

	public static void renderSectionLayer(BakedLevel bakedLevel, RenderType pRenderType, PoseStack pPoseStack, Vector3f cameraPosition, Matrix4f pProjectionMatrix) {
		final var mc = Minecraft.getInstance();

		RenderSystem.assertOnRenderThread();
		pRenderType.setupRenderState();

		mc.getProfiler().push("ganderBlockRenderer");
		mc.getProfiler().popPush(() -> "render_" + pRenderType);

		ShaderInstance shaderinstance = RenderSystem.getShader();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		final var vertexbuffer = bakedLevel.renderBuffers().get(pRenderType);
		if (vertexbuffer != null) {
			BlockPos blockpos = BlockPos.ZERO;
			if (uniform != null) {
				shaderinstance.apply();
				uniform.set(
						(float) ((double) blockpos.getX() - cameraPosition.x),
						(float) ((double) blockpos.getY() - cameraPosition.y),
						(float) ((double) blockpos.getZ() - cameraPosition.z)
				);
				uniform.upload();
			}

			vertexbuffer.bind();
			vertexbuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, shaderinstance);
		}

		if (uniform != null) {
			uniform.set(0.0F, 0.0F, 0.0F);
		}

		mc.getProfiler().pop();
		// net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(pRenderType, this, pPoseStack, pProjectionMatrix, this.ticks, mc.gameRenderer.getMainCamera(), this.getFrustum());
		pRenderType.clearRenderState();
	}
}
