package dev.compactmods.gander.outliner;

import dev.compactmods.gander.client.render.RenderBufferHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.MultiBufferSource;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record AABBOutline(AABB bb, int color, Outline.OutlineParams params) implements Renderable {

	public void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera, float pt) {
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;
		renderBox(ms, buffer, camera, bb, color, lightmap, disableLineNormals);
	}

	@Override
	public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

	}

	protected void renderBox(PoseStack ms, MultiBufferSource buffer, Vec3 camera, AABB box, int color, int lightmap, boolean disableLineNormals) {
		final Vector3f minPosTemp1 = new Vector3f();
		final Vector3f maxPosTemp1 = new Vector3f();
		final Vector3f originTemp = new Vector3f();

		Vector3f minPos = minPosTemp1;
		Vector3f maxPos = maxPosTemp1;

		boolean cameraInside = box.contains(camera);
		boolean cull = !cameraInside && !params.disableCull;
		float inflate = cameraInside ? -1 / 128f : 1 / 128f;

		box = box.move(camera.scale(-1));
		minPos.set((float) box.minX - inflate, (float) box.minY - inflate, (float) box.minZ - inflate);
		maxPos.set((float) box.maxX + inflate, (float) box.maxY + inflate, (float) box.maxZ + inflate);

		float lineWidth = params.getLineWidth();
		if (lineWidth == 0)
			return;

		// VertexConsumer consumer = buffer.getBuffer(RenderTypes.getOutlineSolid());
		VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
		renderBoxEdges(ms, consumer, minPos, maxPos, lineWidth, color, lightmap, disableLineNormals, originTemp);
	}

	protected void renderBoxEdges(PoseStack ms, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, float lineWidth, int color, int lightmap, boolean disableNormals, Vector3f origin) {
		PoseStack.Pose pose = ms.last();

		float lineLengthX = maxPos.x() - minPos.x();
		float lineLengthY = maxPos.y() - minPos.y();
		float lineLengthZ = maxPos.z() - minPos.z();

		origin.set(minPos);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), minPos.y(), minPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), maxPos.y(), minPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), minPos.y(), maxPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), maxPos.y(), maxPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), minPos.y(), maxPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), maxPos.y(), minPos.z());
		RenderBufferHelper.bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);
	}


}
