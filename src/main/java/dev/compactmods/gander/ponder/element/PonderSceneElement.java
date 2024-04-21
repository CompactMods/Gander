package dev.compactmods.gander.ponder.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.ponder.PonderLevel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public abstract class PonderSceneElement implements PonderElement {

	public abstract void renderFirst(PonderLevel world, MultiBufferSource.BufferSource buffer, PoseStack ms, float pt);

	public abstract void renderLayer(PonderLevel world, MultiBufferSource.BufferSource buffer, RenderType type, PoseStack ms, float pt);

	public abstract void renderLast(PonderLevel world, MultiBufferSource.BufferSource buffer, PoseStack ms, float pt);

}
