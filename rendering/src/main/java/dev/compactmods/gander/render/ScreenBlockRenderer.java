package dev.compactmods.gander.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ScreenBlockRenderer {

	public static void prepareTranslucency(PostChain translucencyChain) {
		var target = translucencyChain.getRenderTarget("translucent");
		target.clear(Minecraft.ON_OSX);
		target.copyDepthFrom(translucencyChain.getRenderTarget(RenderTypeHelper.MAIN_TARGET));
	}

	public static void renderSectionLayer(BakedLevel bakedLevel, PostChain translucencyChain, RenderType renderType, PoseStack poseStack,
										  Vector3f cameraPosition, Matrix4f pProjectionMatrix) {

		final var mc = Minecraft.getInstance();

		final var retargetedRenderType = RenderTypeHelper.redirectedRenderType(renderType, translucencyChain);

		RenderSystem.assertOnRenderThread();
		retargetedRenderType.setupRenderState();

		mc.getProfiler().push("ganderBlockRenderer");
		mc.getProfiler().popPush(() -> "render_" + renderType);

		ShaderInstance shaderinstance = RenderSystem.getShader();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		final var vertexbuffer = bakedLevel.renderBuffers().get(renderType);
		if (vertexbuffer != null) {
			if (uniform != null) {
				shaderinstance.apply();
				uniform.set(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
				uniform.upload();
			}

			vertexbuffer.bind();
			vertexbuffer.drawWithShader(poseStack.last().pose(), pProjectionMatrix, shaderinstance);
		}

		if (uniform != null) {
			uniform.set(0.0F, 0.0F, 0.0F);
		}

		mc.getProfiler().pop();
		// net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(pRenderType, this, pPoseStack, pProjectionMatrix, this.ticks, mc.gameRenderer.getMainCamera(), this.getFrustum());
		retargetedRenderType.clearRenderState();
	}
}
