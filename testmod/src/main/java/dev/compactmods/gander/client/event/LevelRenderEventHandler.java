package dev.compactmods.gander.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.Tesselator;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.render.vertex.TranslucentBlockVertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LevelRenderEventHandler {

	public static void onRenderStage(final RenderLevelStageEvent evt) {
		// TODO - Better pipeline handling here (ie pipeline#renderStage(evt.getStage())

		Minecraft mc = Minecraft.getInstance();
		final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		final var mainCamera = mc.gameRenderer.getMainCamera();
		final var player = mc.player;

		final var playerPos = player.blockPosition();

//		final var renderState = Blocks.BEACON.defaultBlockState();
		final var renderState = Blocks.GOLD_BLOCK.defaultBlockState();

		VirtualLevel vl = new VirtualLevel(mc.level.registryAccess());
		vl.setBlock(BlockPos.ZERO, renderState, Block.UPDATE_ALL);

		PoseStack stack = evt.getPoseStack();
		stack.pushPose();
		{
			Vec3 projectedView = mainCamera.getPosition();
//			stack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

			Vec3 offset = Vec3.atLowerCornerOf(playerPos.north(2));

			if (evt.getStage().equals(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)) {


				var model = mc.getBlockRenderer().getBlockModel(renderState);

				stack.pushPose();
				{
					stack.translate(offset.x, offset.y, offset.z);

//					var stack2 = RenderSystem.getModelViewStack();
//					stack2.pushMatrix();
//					stack2.identity();
//
//					stack2.mul(stack.last().pose()).invert();
//
//					RenderSystem.applyModelViewMatrix();


					for (RenderType type : model.getRenderTypes(renderState, mc.level.random, ModelData.EMPTY)) {
						var transVC = new TranslucentBlockVertexConsumer(buffers.getBuffer(type), .4f);
						mc.getBlockRenderer().getModelRenderer().tesselateBlock(
								vl,
								model,
								renderState,
								playerPos.north(2),
								stack,
								transVC,
								false,
								mc.level.random,
								0,
								OverlayTexture.NO_OVERLAY,
								ModelData.EMPTY,
								type
						);

						type.setupRenderState();
						if(buffers.getBuffer(type) instanceof BufferBuilder buffBuilder) {
							var e = buffBuilder.end();
							BufferUploader.drawWithShader(e);
						}

//						buffers.endBatch(RenderTypes.PHANTOM);
					}

//					stack2.popMatrix();
//					RenderSystem.applyModelViewMatrix();
				}
				stack.popPose();
			}

			if (evt.getStage().equals(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)) {
				stack.pushPose();
				{
					stack.translate(offset.x, offset.y, offset.z);
					if (renderState.hasBlockEntity()) {
						var blockEntity = vl.getBlockEntity(BlockPos.ZERO);
						renderBlockEntity(evt, vl, renderState, blockEntity, mc, stack, buffers);
					}
				}

				stack.popPose();
			}
		}
		stack.popPose();
	}

	private static void renderBlockEntity(RenderLevelStageEvent evt, VirtualLevel vl, BlockState renderState, BlockEntity blockEntity, Minecraft mc, PoseStack stack, MultiBufferSource.BufferSource buffers) {
		if (blockEntity == null) return;

		BlockEntityRenderer<BlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
		if (renderer == null) return;

		BlockPos pos = blockEntity.getBlockPos();
		stack.pushPose();
		{
			try {
				var transVC = new TranslucentBlockVertexConsumer(buffers.getBuffer(RenderTypes.PHANTOM), .5f);

				renderer.render(blockEntity, evt.getPartialTick(), stack, buffers, LightTexture.FULL_BLOCK, OverlayTexture.NO_OVERLAY);

			} catch (Exception e) {
				String message = "BlockEntity " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()) + " could not be rendered virtually.";
			}
		}
		stack.popPose();
	}
}
