package dev.compactmods.gander.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.baked.BakedLevel;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class ScreenBlockRenderer {

	public static void renderSectionBlocks(BakedLevel bakedLevel, RenderTypeStore renderTypeStore, RenderType renderType, PoseStack poseStack,
										  Vector3f cameraPosition, Matrix4f pProjectionMatrix) {
		renderSectionLayer(bakedLevel.blockRenderBuffers(), renderTypeStore, renderType, poseStack, cameraPosition, pProjectionMatrix);
	}

	public static void renderSectionFluids(BakedLevel bakedLevel, RenderTypeStore renderTypeStore, RenderType renderType, PoseStack poseStack,
			Vector3f cameraPosition, Matrix4f pProjectionMatrix) {
		renderSectionLayer(bakedLevel.fluidRenderBuffers(), renderTypeStore, renderType, poseStack, cameraPosition, pProjectionMatrix);
	}

	private static void renderSectionLayer(Map<RenderType, VertexBuffer> renderBuffers, RenderTypeStore renderTypeStore, RenderType renderType, PoseStack poseStack,
			Vector3f cameraPosition, Matrix4f pProjectionMatrix) {
		final var mc = Minecraft.getInstance();

		final var retargetedRenderType = renderTypeStore.redirectedRenderType(renderType);

		RenderSystem.assertOnRenderThread();
		retargetedRenderType.setupRenderState();

		mc.getProfiler().popPush(() -> "render_" + renderType);

		ShaderInstance shaderinstance = RenderSystem.getShader();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		final var vertexbuffer = renderBuffers.get(renderType);
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
