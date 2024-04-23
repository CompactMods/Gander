package dev.compactmods.gander.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.DiffuseLightCalculator;
import dev.compactmods.gander.render.ForcedDiffuseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class ScreenBlockRenderer {

	public static void render(BlockAndTintGetter level, BoundingBox blockBoundaries, MultiBufferSource.BufferSource buffer, PoseStack pose, float partialTicks) {
		ForcedDiffuseState.pushCalculator(DiffuseLightCalculator.DEFAULT);
		pose.pushPose();

		Minecraft mc = Minecraft.getInstance();

		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

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

			renderIntoBuffer(level, blockBoundaries, buffer, type, pose);

			if (isTranslucent && chain != null)
				chain.process(partialTicks);
		}


		pose.popPose();
		ForcedDiffuseState.popCalculator();
	}

	public static void renderIntoBuffer(BlockAndTintGetter level, BoundingBox blockBoundaries, MultiBufferSource.BufferSource buffer, RenderType type, PoseStack pose) {
		var vertices = buffer.getBuffer(type);
		pose.pushPose();
		{
			BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
			ModelBlockRenderer renderer = dispatcher.getModelRenderer();

			RandomSource random = RandomSource.createNewThreadLocalInstance();

			ModelBlockRenderer.enableCaching();
			BlockPos.betweenClosedStream(blockBoundaries).forEach(pos -> {
				BlockState state = level.getBlockState(pos);
				FluidState fluidState = level.getFluidState(pos);

				pose.pushPose();
				pose.translate(pos.getX(), pos.getY(), pos.getZ());

				ModelData modelData;
				if (state.getRenderShape() == RenderShape.MODEL) {
					BakedModel model = dispatcher.getBlockModel(state);
					BlockEntity blockEntity = level.getBlockEntity(pos);
					modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
					modelData = model.getModelData(level, pos, state, modelData);

					long seed = state.getSeed(pos);
					random.setSeed(seed);

					if (model.getRenderTypes(state, random, modelData).contains(type)) {
						dispatcher.renderBatched(state, pos, level, pose, vertices, true, random, modelData, type);
					}
				}

				if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == type)
					dispatcher.renderLiquid(pos, level, new FluidVertexConsumer(vertices, pose, pos), state, fluidState);

				pose.popPose();
			});


			ModelBlockRenderer.clearCache();
			buffer.endBatch();
		}
		pose.popPose();
	}
}
