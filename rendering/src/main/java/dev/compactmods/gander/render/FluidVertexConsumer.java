/**
 * Credit: Patchouli
 */

package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.BlockPos;

/**
 * Since {@link net.minecraft.client.renderer.block.LiquidBlockRenderer} doesn't use the pose stack at all, we need to
 * both (1) un-transform the positions by {@code pos}, and also re-transform them using the {@link PoseStack}.
 */
public record FluidVertexConsumer(VertexConsumer prior, PoseStack pose, BlockPos pos) implements VertexConsumer {

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		final float dx = pos.getX() & 15;
		final float dy = pos.getY() & 15;
		final float dz = pos.getZ() & 15;
		return prior.addVertex(pose.last().pose(), x - dx, y - dy, z - dz);
	}

	@Override
	public VertexConsumer setColor(final int i, final int i1, final int i2, final int i3)
	{
		return prior.setColor(i, i1, i2, i3);
	}

	@Override
	public VertexConsumer setUv(final float v, final float v1)
	{
		return prior.setUv(v, v1);
	}

	@Override
	public VertexConsumer setUv1(final int i, final int i1)
	{
		return prior.setUv1(i, i1);
	}

	@Override
	public VertexConsumer setUv2(final int i, final int i1)
	{
		return prior.setUv2(i, i1);
	}

	@Override
	public VertexConsumer setNormal(final float v, final float v1, final float v2)
	{
		return prior.setNormal(v, v1, v2);
	}
}
