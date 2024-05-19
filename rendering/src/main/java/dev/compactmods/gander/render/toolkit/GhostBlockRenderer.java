package dev.compactmods.gander.render.toolkit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.compactmods.gander.render.RenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class GhostBlockRenderer {

//	public static void renderTransparentBlock(BlockState state, @Nullable BlockPos pos, PoseStack matrix) {
//		final var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
//		renderTransparentBlock(state, pos, matrix, buffers, 100);
//	}
//
//	public static void renderTransparentBlock(BlockState state, @Nullable BlockPos pos, PoseStack matrix, MultiBufferSource buffer) {
//		renderTransparentBlock(state, pos, matrix, buffer, 100);
//	}

	public static void renderTransparentBlock(BlockState state, @Nullable BlockPos pos, PoseStack matrix, MultiBufferSource buffer, int ticksLeft) {
		final Minecraft mc = Minecraft.getInstance();
		final BlockColors colors = mc.getBlockColors();

		// clamp b/n 0-100, effective range 0 - 0.9f
		final float alpha = ticksLeft >= 100 ? 0.9f : 0.9f * Math.max(ticksLeft / 100f, .1f);

		VertexConsumer builder = buffer.getBuffer(RenderTypes.PHANTOM);
		final BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
		BakedModel model = dispatcher.getBlockModel(state);
		if (model != mc.getModelManager().getMissingModel()) {
			for(Direction dir : Direction.values())
				model.getQuads(state, dir, mc.level.random, ModelData.EMPTY, null)
						.forEach(quad -> addQuad(state, pos, matrix, mc, colors, builder, quad, alpha));

			model.getQuads(state, null, mc.level.random, ModelData.EMPTY, null)
					.forEach(quad -> addQuad(state, pos, matrix, mc, colors, builder, quad, alpha));
		}
	}

	private static void addQuad(BlockState state, @Nullable BlockPos pos, PoseStack matrix, Minecraft mc, BlockColors colors, VertexConsumer builder, BakedQuad quad, float alpha) {
		int color = quad.isTinted() ? colors.getColor(state, mc.level, pos, quad.getTintIndex()) :
				FastColor.ARGB32.color(255, 255, 255, 255);

		final float red = FastColor.ARGB32.red(color) / 255f;
		final float green = FastColor.ARGB32.green(color) / 255f;
		final float blue = FastColor.ARGB32.blue(color) / 255f;

		final float trueAlpha = Mth.clamp(0.01f, alpha, 0.06f);
		builder.putBulkData(matrix.last(), quad,
				red,
				green,
				blue,
				trueAlpha,
				LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, false);
	}
}

