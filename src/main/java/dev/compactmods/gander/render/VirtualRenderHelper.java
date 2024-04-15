package dev.compactmods.gander.render;

import java.nio.ByteBuffer;

import dev.compactmods.gander.ponder.level.EmptyBlockAndTintGetter;

import net.minecraft.client.Minecraft;

import net.neoforged.neoforge.client.model.data.ModelData;

import net.neoforged.neoforge.client.model.data.ModelProperty;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class VirtualRenderHelper {
	public static final ModelProperty<Boolean> VIRTUAL_PROPERTY = new ModelProperty<>();
	public static final ModelData VIRTUAL_DATA = ModelData.builder().with(VIRTUAL_PROPERTY, true).build();
	private static final ThreadLocal<ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(ThreadLocalObjects::new);

	public static SuperByteBuffer bufferBlock(BlockState state) {
		return bufferModel(Minecraft.getInstance().getBlockRenderer().getBlockModel(state), state);
	}

	public static SuperByteBuffer bufferModel(BakedModel model, BlockState state) {
		return bufferModel(model, state, null);
	}

	public static SuperByteBuffer bufferModel(BakedModel model, BlockState state, @Nullable PoseStack poseStack) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		ModelBlockRenderer renderer = dispatcher.getModelRenderer();
		ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();

		if (poseStack == null) {
			poseStack = objects.identityPoseStack;
		}
		RandomSource random = objects.random;

		ShadeSeparatingVertexConsumer shadeSeparatingWrapper = objects.shadeSeparatingWrapper;
		BufferBuilder shadedBuilder = objects.shadedBuilder;
		BufferBuilder unshadedBuilder = objects.unshadedBuilder;

		shadedBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		unshadedBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
		shadeSeparatingWrapper.prepare(shadedBuilder, unshadedBuilder);

		ModelData modelData = model.getModelData(EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO, state, VIRTUAL_DATA);
		poseStack.pushPose();
		renderer.tesselateBlock(EmptyBlockAndTintGetter.INSTANCE, model, state, BlockPos.ZERO, poseStack, shadeSeparatingWrapper, false, random, 42L, OverlayTexture.NO_OVERLAY, modelData, null);
		poseStack.popPose();

		shadeSeparatingWrapper.clear();
		return endAndCombine(shadedBuilder, unshadedBuilder);
	}

	public static void transferBlockVertexData(ByteBuffer vertexBuffer, int vertexCount, int stride, MutableTemplateMesh mutableMesh, int dstIndex) {
		for (int i = 0; i < vertexCount; i++) {
			mutableMesh.x(i, vertexBuffer.getFloat(i * stride));
			mutableMesh.y(i, vertexBuffer.getFloat(i * stride + 4));
			mutableMesh.z(i, vertexBuffer.getFloat(i * stride + 8));
			mutableMesh.color(i, vertexBuffer.getInt(i * stride + 12));
			mutableMesh.u(i, vertexBuffer.getFloat(i * stride + 16));
			mutableMesh.v(i, vertexBuffer.getFloat(i * stride + 20));
			mutableMesh.overlay(i, OverlayTexture.NO_OVERLAY);
			mutableMesh.light(i, vertexBuffer.getInt(i * stride + 24));
			mutableMesh.normal(i, vertexBuffer.getInt(i * stride + 28));
		}
	}

	public static SuperByteBuffer endAndCombine(BufferBuilder shadedBuilder, BufferBuilder unshadedBuilder) {
		RenderedBuffer shadedData = shadedBuilder.end();
		int totalVertexCount = shadedData.drawState().vertexCount();
		int unshadedStartVertex = totalVertexCount;
		RenderedBuffer unshadedData = unshadedBuilder.endOrDiscardIfEmpty();
		if (unshadedData != null) {
			if (shadedData.drawState().format() != unshadedData.drawState().format()) {
				throw new IllegalStateException("Buffer formats are not equal!");
			}
			totalVertexCount += unshadedData.drawState().vertexCount();
		}

		MutableTemplateMesh mutableMesh = new MutableTemplateMesh(totalVertexCount);
		transferBlockVertexData(shadedData.vertexBuffer(), shadedData.drawState().vertexCount(), shadedData.drawState().format().getVertexSize(), mutableMesh, 0);
		if (unshadedData != null) {
			transferBlockVertexData(unshadedData.vertexBuffer(), unshadedData.drawState().vertexCount(), unshadedData.drawState().format().getVertexSize(), mutableMesh, unshadedStartVertex);
		}
		return new SuperByteBuffer(mutableMesh.toImmutable(), unshadedStartVertex);
	}

	private static class ThreadLocalObjects {
		public final PoseStack identityPoseStack = new PoseStack();
		public final RandomSource random = RandomSource.createNewThreadLocalInstance();
		public final ShadeSeparatingVertexConsumer shadeSeparatingWrapper = new ShadeSeparatingVertexConsumer();
		public final BufferBuilder shadedBuilder = new BufferBuilder(512);
		public final BufferBuilder unshadedBuilder = new BufferBuilder(512);
	}
}
