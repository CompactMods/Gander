package dev.compactmods.gander.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexBuffer;

import dev.compactmods.gander.render.baked.BakedLevel;
import dev.compactmods.gander.render.baked.LevelBakery;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ScreenBlockRenderer {

	public static void render(Level level, BoundingBox blockBoundaries, Camera camera, MultiBufferSource.BufferSource buffer, PoseStack pose, float partialTicks) {
		pose.pushPose();

		Minecraft mc = Minecraft.getInstance();

		for (RenderType type : RenderType.chunkBufferLayers()) {
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
//
//			var baked = LevelBakery.bakeVertices(level, blockBoundaries, camera.getPosition());
//			baked.render();

			if (isTranslucent && chain != null)
				chain.process(partialTicks);
		}


		pose.popPose();
	}

	public static void renderBakedLevel(BakedLevel bakedLevel, PoseStack poseStack, Vector3f cameraPosition, Matrix4f projectionMatrix) {
		poseStack.pushPose();
		float scale = (1 / 16f);
		// poseStack.scale(scale, scale, scale);

		RenderTypeHelper.RENDER_TYPE_BUFFERS.keySet().forEach(renderType -> {
			renderSectionLayer(bakedLevel, renderType, poseStack, cameraPosition, projectionMatrix);
		});
		poseStack.popPose();
	}

	private static void renderSectionLayer(BakedLevel bakedLevel, RenderType pRenderType, PoseStack pPoseStack, Vector3f cameraPosition, Matrix4f pProjectionMatrix) {
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
