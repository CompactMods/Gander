package dev.compactmods.gander.ponder.element;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.compactmods.gander.ponder.PonderLevel;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public abstract class PonderSceneElement extends PonderElement {

	public abstract void renderFirst(PonderLevel world, MultiBufferSource buffer, PoseStack ms, float pt);

	public abstract void renderLayer(PonderLevel world, MultiBufferSource buffer, RenderType type, PoseStack ms, float pt);

	public abstract void renderLast(PonderLevel world, MultiBufferSource buffer, PoseStack ms, float pt);

}
