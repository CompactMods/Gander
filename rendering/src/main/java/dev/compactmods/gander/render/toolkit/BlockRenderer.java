package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.function.Function;

public class BlockRenderer {

	public static void renderSectionBlocks(BakedLevel bakedLevel, RenderTypeStore renderTypeStore, RenderType renderType, PoseStack poseStack,
                                           Vector3fc camera,
                                           Vector3fc renderOrigin,
                                           Matrix4f pProjectionMatrix) {
		renderSectionLayer(bakedLevel.blockRenderBuffers(), renderTypeStore::redirectedBlockRenderType, renderType, poseStack, camera, renderOrigin, pProjectionMatrix);
	}

	public static void renderSectionFluids(BakedLevel bakedLevel, RenderTypeStore renderTypeStore, RenderType renderType, PoseStack poseStack,
                                           Vector3fc camera,
                                           Vector3fc renderOrigin,
                                           Matrix4f pProjectionMatrix) {
		renderSectionLayer(bakedLevel.fluidRenderBuffers(), renderTypeStore::redirectedFluidRenderType, renderType, poseStack, camera, renderOrigin, pProjectionMatrix);
	}

	// TODO: we shouldn't leak internals...
	public static void renderSectionLayer(Map<RenderType, VertexBuffer> renderBuffers,
                                          Function<RenderType, RenderType> redirector,
                                          RenderType renderType,
                                          PoseStack poseStack,
                                          Vector3fc cameraPosition,
                                          Vector3fc renderOrigin,
                                          Matrix4f pProjectionMatrix
    ) {
		final var mc = Minecraft.getInstance();

		final var retargetedRenderType = redirector.apply(renderType);

		RenderSystem.assertOnRenderThread();
		retargetedRenderType.setupRenderState();

		mc.getProfiler().popPush(() -> "render_" + renderType);

		ShaderInstance shaderinstance = RenderSystem.getShader();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		final var vertexbuffer = renderBuffers.get(renderType);

        final Vector3f renderAt = new Vector3f();
        renderOrigin.sub(cameraPosition, renderAt);

		if (vertexbuffer != null) {
			if (uniform != null) {
				shaderinstance.apply();
				uniform.set(renderAt.x(), renderAt.y(), renderAt.z());
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
