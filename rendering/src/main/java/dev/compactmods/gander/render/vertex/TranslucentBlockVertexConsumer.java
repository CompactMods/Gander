package dev.compactmods.gander.render.vertex;

import java.util.Arrays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

public final class TranslucentBlockVertexConsumer extends VertexConsumerWrapper {

	private final float alpha;

	public TranslucentBlockVertexConsumer(VertexConsumer parent, float alpha) {
		super(parent);
		this.alpha = Mth.clamp(alpha, 0.1f, 1f);
	}

	@Override
	public void putBulkData(PoseStack.Pose pPose, BakedQuad pQuad, float[] pBrightness, float pRed, float pGreen, float pBlue, float pAlpha, int[] pLightmap, int pPackedOverlay, boolean p_331268_) {
		final var modifiedBrightness = new float[pBrightness.length];
		Arrays.fill(modifiedBrightness, 0.1f);

		super.putBulkData(pPose, pQuad, modifiedBrightness, pRed, pGreen, pBlue, pAlpha, pLightmap, pPackedOverlay, p_331268_);
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int pAlpha) {
		return parent.setColor(red, green, blue, this.alpha);
	}
}
