/**
 * Credit: Patchouli
 */

package dev.compactmods.gander.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

/**
 * Since {@link net.minecraft.client.renderer.block.LiquidBlockRenderer} doesn't use the pose stack at all, we need to
 * both (1) un-transform the positions by {@code pos}, and also re-transform them using the {@link PoseStack}.
 */
public final class FluidVertexConsumer extends VertexConsumerWrapper {
    private final PoseStack pose;
    private final BlockPos pos;

    public FluidVertexConsumer(VertexConsumer parent, PoseStack pose, BlockPos pos) {
        super(parent);

        this.pose = pose;
        this.pos = pos.immutable();
    }

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		final float dx = pos.getX() & 15;
		final float dy = pos.getY() & 15;
		final float dz = pos.getZ() & 15;
		return parent.addVertex(pose.last().pose(), x - dx, y - dy, z - dz);
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		return parent.setNormal(pose.last(), x, y, z);
	}
}
