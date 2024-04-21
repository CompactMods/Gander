package dev.compactmods.gander.outliner;

import net.minecraft.client.renderer.MultiBufferSource;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AABBOutline extends Outline {

	protected AABB bb;

	protected final Vector3f minPosTemp1 = new Vector3f();
	protected final Vector3f maxPosTemp1 = new Vector3f();
	protected final Vector3f originTemp = new Vector3f();

	public AABBOutline(AABB bb) {
		setBounds(bb);
	}

	public void setBounds(AABB bb) {
		this.bb = bb;
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera, float pt) {
		params.loadColor(colorTemp);
		Vector4f color = colorTemp;
		int lightmap = params.lightmap;
		boolean disableLineNormals = params.disableLineNormals;
		renderBox(ms, buffer, camera, bb, color, lightmap, disableLineNormals);
	}

	protected void renderBox(PoseStack ms, MultiBufferSource buffer, Vec3 camera, AABB box, Vector4f color, int lightmap, boolean disableLineNormals) {
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
		renderBoxEdges(ms, consumer, minPos, maxPos, lineWidth, color, lightmap, disableLineNormals);
	}

	protected void renderBoxEdges(PoseStack ms, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, float lineWidth, Vector4f color, int lightmap, boolean disableNormals) {
		Vector3f origin = originTemp;

		PoseStack.Pose pose = ms.last();

		float lineLengthX = maxPos.x() - minPos.x();
		float lineLengthY = maxPos.y() - minPos.y();
		float lineLengthZ = maxPos.z() - minPos.z();

		origin.set(minPos);
		bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);
		bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), minPos.y(), minPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);
		bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), maxPos.y(), minPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), minPos.y(), maxPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);
		bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);

		origin.set(minPos.x(), maxPos.y(), maxPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.EAST, lineLengthX, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), minPos.y(), maxPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.UP, lineLengthY, lineWidth, color, lightmap, disableNormals);

		origin.set(maxPos.x(), maxPos.y(), minPos.z());
		bufferCuboidLine(pose, consumer, origin, Direction.SOUTH, lineLengthZ, lineWidth, color, lightmap, disableNormals);
	}

}
