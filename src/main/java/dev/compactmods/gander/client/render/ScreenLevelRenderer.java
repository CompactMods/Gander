package dev.compactmods.gander.client.render;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.DiffuseLightCalculator;
import dev.compactmods.gander.render.ForcedDiffuseState;
import dev.compactmods.gander.utility.VecHelper;
import dev.compactmods.gander.utility.math.PoseTransformStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

public class ScreenLevelRenderer {

	private final Level level;
	private final BoundingBox blockBoundaries;

	List<BlockEntity> renderedBlockEntities;

	Vec3 prevAnimatedOffset = Vec3.ZERO;
	Vec3 animatedOffset = Vec3.ZERO;
	Vec3 prevAnimatedRotation = Vec3.ZERO;
	Vec3 animatedRotation = Vec3.ZERO;
	Vec3 centerOfRotation = Vec3.ZERO;

	public ScreenLevelRenderer(Level level, BoundingBox blockBoundaries) {
		this.level = level;
		this.blockBoundaries = blockBoundaries;
	}

	private void transformMS(PoseStack ms, float pt) {
		PoseTransformStack.of(ms)
				.translate(VecHelper.lerp(pt, prevAnimatedOffset, animatedOffset));
		if (!animatedRotation.equals(Vec3.ZERO) || !prevAnimatedRotation.equals(Vec3.ZERO)) {
			if (centerOfRotation == null)
				centerOfRotation = blockBoundaries.getCenter().getCenter();

			double rotX = Mth.lerp(pt, prevAnimatedRotation.x, animatedRotation.x);
			double rotZ = Mth.lerp(pt, prevAnimatedRotation.z, animatedRotation.z);
			double rotY = Mth.lerp(pt, prevAnimatedRotation.y, animatedRotation.y);
			PoseTransformStack.of(ms)
					.translate(centerOfRotation)
					.rotateX((float) rotX)
					.rotateZ((float) rotZ)
					.rotateY((float) rotY)
					.translateBack(centerOfRotation);
		}
	}

//	public void renderBreakProgress(MultiBufferSource.BufferSource buffer, PoseStack ms, float partialTicks) {
//		if (redraw) {
//			renderedBlockEntities = null;
//			tickableBlockEntities = null;
//		}
//
//		ms.pushPose();
//		transformMS(ms, partialTicks);
//		renderBlockEntities(level, ms, buffer, partialTicks);
//
//		Map<BlockPos, Integer> blockBreakingProgressions = level.getBlockBreakingProgressions();
//		PoseStack overlayMS = null;
//
//		for (Map.Entry<BlockPos, Integer> entry : blockBreakingProgressions.entrySet()) {
//			BlockPos pos = entry.getKey();
//			if (!blockBoundaries.isInside(pos))
//				continue;
//
//			if (overlayMS == null) {
//				overlayMS = new PoseStack();
//				overlayMS.last().pose().set(ms.last().pose());
//				overlayMS.last().normal().set(ms.last().normal());
//			}
//
//			VertexConsumer builder = new SheetedDecalTextureGenerator(
//					buffer.getBuffer(ModelBakery.DESTROY_TYPES.get(entry.getValue())),
//					overlayMS.last().pose(),
//					overlayMS.last().normal(),
//					1);
//
//			ms.pushPose();
//			ms.translate(pos.getX(), pos.getY(), pos.getZ());
//			Minecraft.getInstance()
//					.getBlockRenderer()
//					.renderBreakingTexture(level.getBlockState(pos), pos, level, ms, builder, ModelData.EMPTY);
//
//			ms.popPose();
//		}
//
//		ms.popPose();
//	}

	public void render(Camera camera, MultiBufferSource.BufferSource buffer, PoseStack pose, float pt) {
		ForcedDiffuseState.pushCalculator(DiffuseLightCalculator.DEFAULT);
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

			renderIntoBuffer(buffer, type, pose, pt);

			if (isTranslucent && chain != null)
				chain.process(pt);
		}

		var entities = level.getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(blockBoundaries), Predicates.alwaysTrue());
		ScreenEntityRenderer.renderEntities(entities, pose, buffer, camera, pt);

		// TODO: Fix particles level.renderParticles(pose, buffer, camera, pt);

		pose.popPose();
		ForcedDiffuseState.popCalculator();
	}

	public void renderIntoBuffer(MultiBufferSource.BufferSource buffer, RenderType type, PoseStack pose, float partialTicks) {
		var b = buffer.getBuffer(type);
		pose.pushPose();
		{
			transformMS(pose, partialTicks);
			BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
			ModelBlockRenderer renderer = dispatcher.getModelRenderer();

			RandomSource random = RandomSource.createNewThreadLocalInstance();

			ModelBlockRenderer.enableCaching();
			BlockPos.betweenClosedStream(blockBoundaries).forEach(pos -> {
				BlockState state = level.getBlockState(pos);
				FluidState fluidState = level.getFluidState(pos);

				pose.pushPose();
				pose.translate(pos.getX(), pos.getY(), pos.getZ());

				if (state.getRenderShape() == RenderShape.MODEL) {
					BakedModel model = dispatcher.getBlockModel(state);
					BlockEntity blockEntity = level.getBlockEntity(pos);

					long seed = state.getSeed(pos);
					random.setSeed(seed);

					ModelData modelData = blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
					modelData = model.getModelData(level, pos, state, modelData);
					if (model.getRenderTypes(state, random, modelData).contains(type)) {
						renderer.tesselateBlock(level, model, state, pos, pose, b, true,
								random, seed, OverlayTexture.NO_OVERLAY, modelData, type);
					}
				}

				if (!fluidState.isEmpty() && ItemBlockRenderTypes.getRenderLayer(fluidState) == type)
					dispatcher.renderLiquid(pos, level, b, state, fluidState);

				pose.popPose();
			});
			ModelBlockRenderer.clearCache();
			buffer.endBatch();
		}
		pose.popPose();
	}

	private void renderBlockEntities(Level world, PoseStack ms, MultiBufferSource buffer, float pt) {
		BlockEntityRenderHelper.renderBlockEntities(world, renderedBlockEntities, ms, buffer, pt);
	}
}
