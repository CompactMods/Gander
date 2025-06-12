package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.geometry.BakedLevelSection;
import dev.compactmods.gander.render.geometry.MultiPassGeometryUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;

public class BlockRenderer {

	public static void renderSectionBlocks(BakedLevelSection section,
                                           MultiPassGeometryUploader uploader,
                                           RenderType renderType, PoseStack poseStack,
                                           Vector3fc camera,
                                           Vector3fc renderOrigin,
                                           Matrix4f pProjectionMatrix) {
		renderSectionLayer(section, uploader, renderType, poseStack, camera, renderOrigin, pProjectionMatrix);
	}

	public static void renderSectionFluids(BakedLevelSection section,
                                           MultiPassGeometryUploader uploader,
                                           RenderType renderType, PoseStack poseStack,
                                           Vector3fc camera,
                                           Vector3fc renderOrigin,
                                           Matrix4f pProjectionMatrix) {
		renderSectionLayer(section, uploader, renderType, poseStack, camera, renderOrigin, pProjectionMatrix);
	}

	public static void renderSectionLayer(BakedLevelSection section,
                                          MultiPassGeometryUploader uploader,
                                          RenderType renderType,
                                          PoseStack poseStack,
                                          Vector3fc cameraPosition,
                                          Vector3fc renderOrigin,
                                          Matrix4f pProjectionMatrix
    ) {
		final var mc = Minecraft.getInstance();

		RenderSystem.assertOnRenderThread();
		renderType.setupRenderState();

        Profiler.get().popPush("gander_render_" + renderType.getName());

//        uploader.makeUploadTask(renderType, section.meshData())
//        uploader.makeUploadTask(renderType, section.gpuBuffers())

		Profiler.get().pop();

		// net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(pRenderType, this, pPoseStack, pProjectionMatrix, this.ticks, mc.gameRenderer.getMainCamera(), this.getFrustum());
		renderType.clearRenderState();
	}


}
