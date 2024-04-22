package dev.compactmods.gander.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;

import net.minecraft.util.FastColor;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RenderBufferHelper {

	public static void bufferCuboidLine(PoseStack.Pose pose, VertexConsumer consumer, Vector3f origin, Direction direction,
								 float length, float width, int color, int lightmap, boolean disableNormals) {
		Vector3f minPos = new Vector3f();
		Vector3f maxPos = new Vector3f();

		float halfWidth = width / 2;
		minPos.set(origin.x() - halfWidth, origin.y() - halfWidth, origin.z() - halfWidth);
		maxPos.set(origin.x() + halfWidth, origin.y() + halfWidth, origin.z() + halfWidth);

		switch (direction) {
			case DOWN -> {
				minPos.add(0, -length, 0);
			}
			case UP -> {
				maxPos.add(0, length, 0);
			}
			case NORTH -> {
				minPos.add(0, 0, -length);
			}
			case SOUTH -> {
				maxPos.add(0, 0, length);
			}
			case WEST -> {
				minPos.add(-length, 0, 0);
			}
			case EAST -> {
				maxPos.add(length, 0, 0);
			}
		}

		bufferCuboid(pose, consumer, minPos, maxPos, color, lightmap, disableNormals);
	}

	protected static void bufferCuboid(PoseStack.Pose pose, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, int color, int lightmap, boolean disableNormals) {

		Vector4f posTransformTemp = new Vector4f();
		Vector3f normalTransformTemp = new Vector3f();

		float minX = minPos.x();
		float minY = minPos.y();
		float minZ = minPos.z();
		float maxX = maxPos.x();
		float maxY = maxPos.y();
		float maxZ = maxPos.z();

		Matrix4f posMatrix = pose.pose();

		posTransformTemp.set(minX, minY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x0 = posTransformTemp.x();
		double y0 = posTransformTemp.y();
		double z0 = posTransformTemp.z();

		posTransformTemp.set(minX, minY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x1 = posTransformTemp.x();
		double y1 = posTransformTemp.y();
		double z1 = posTransformTemp.z();

		posTransformTemp.set(maxX, minY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x2 = posTransformTemp.x();
		double y2 = posTransformTemp.y();
		double z2 = posTransformTemp.z();

		posTransformTemp.set(maxX, minY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x3 = posTransformTemp.x();
		double y3 = posTransformTemp.y();
		double z3 = posTransformTemp.z();

		posTransformTemp.set(minX, maxY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x4 = posTransformTemp.x();
		double y4 = posTransformTemp.y();
		double z4 = posTransformTemp.z();

		posTransformTemp.set(minX, maxY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x5 = posTransformTemp.x();
		double y5 = posTransformTemp.y();
		double z5 = posTransformTemp.z();

		posTransformTemp.set(maxX, maxY, maxZ, 1);
		posTransformTemp.mul(posMatrix);
		double x6 = posTransformTemp.x();
		double y6 = posTransformTemp.y();
		double z6 = posTransformTemp.z();

		posTransformTemp.set(maxX, maxY, minZ, 1);
		posTransformTemp.mul(posMatrix);
		double x7 = posTransformTemp.x();
		double y7 = posTransformTemp.y();
		double z7 = posTransformTemp.z();

		float r = FastColor.ARGB32.red(color);
		float g = FastColor.ARGB32.green(color);
		float b = FastColor.ARGB32.blue(color);
		float a = FastColor.ARGB32.alpha(color);

		Matrix3f normalMatrix = pose.normal();

		// down

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, -1, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx0 = normalTransformTemp.x();
		float ny0 = normalTransformTemp.y();
		float nz0 = normalTransformTemp.z();

		consumer.vertex(x0, y0, z0)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx0, ny0, nz0)
				.endVertex();

		consumer.vertex(x1, y1, z1)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx0, ny0, nz0)
				.endVertex();

		consumer.vertex(x2, y2, z2)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx0, ny0, nz0)
				.endVertex();

		consumer.vertex(x3, y3, z3)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx0, ny0, nz0)
				.endVertex();

		// up

		normalTransformTemp.set(0, 1, 0);
		normalTransformTemp.mul(normalMatrix);
		float nx1 = normalTransformTemp.x();
		float ny1 = normalTransformTemp.y();
		float nz1 = normalTransformTemp.z();

		consumer.vertex(x4, y4, z4)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx1, ny1, nz1)
				.endVertex();

		consumer.vertex(x5, y5, z5)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx1, ny1, nz1)
				.endVertex();

		consumer.vertex(x6, y6, z6)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx1, ny1, nz1)
				.endVertex();

		consumer.vertex(x7, y7, z7)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx1, ny1, nz1)
				.endVertex();

		// north

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, 0, -1);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx2 = normalTransformTemp.x();
		float ny2 = normalTransformTemp.y();
		float nz2 = normalTransformTemp.z();

		consumer.vertex(x7, y7, z7)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx2, ny2, nz2)
				.endVertex();

		consumer.vertex(x2, y2, z2)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx2, ny2, nz2)
				.endVertex();

		consumer.vertex(x1, y1, z1)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx2, ny2, nz2)
				.endVertex();

		consumer.vertex(x4, y4, z4)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx2, ny2, nz2)
				.endVertex();

		// south

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(0, 0, 1);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx3 = normalTransformTemp.x();
		float ny3 = normalTransformTemp.y();
		float nz3 = normalTransformTemp.z();

		consumer.vertex(x5, y5, z5)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx3, ny3, nz3)
				.endVertex();

		consumer.vertex(x0, y0, z0)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx3, ny3, nz3)
				.endVertex();

		consumer.vertex(x3, y3, z3)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx3, ny3, nz3)
				.endVertex();

		consumer.vertex(x6, y6, z6)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx3, ny3, nz3)
				.endVertex();

		// west

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(-1, 0, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx4 = normalTransformTemp.x();
		float ny4 = normalTransformTemp.y();
		float nz4 = normalTransformTemp.z();

		consumer.vertex(x4, y4, z4)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx4, ny4, nz4)
				.endVertex();

		consumer.vertex(x1, y1, z1)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx4, ny4, nz4)
				.endVertex();

		consumer.vertex(x0, y0, z0)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx4, ny4, nz4)
				.endVertex();

		consumer.vertex(x5, y5, z5)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx4, ny4, nz4)
				.endVertex();

		// east

		if (disableNormals) {
			normalTransformTemp.set(0, 1, 0);
		} else {
			normalTransformTemp.set(1, 0, 0);
		}
		normalTransformTemp.mul(normalMatrix);
		float nx5 = normalTransformTemp.x();
		float ny5 = normalTransformTemp.y();
		float nz5 = normalTransformTemp.z();

		consumer.vertex(x6, y6, z6)
				.color(r, g, b, a)
				.uv(0, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx5, ny5, nz5)
				.endVertex();

		consumer.vertex(x3, y3, z3)
				.color(r, g, b, a)
				.uv(0, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx5, ny5, nz5)
				.endVertex();

		consumer.vertex(x2, y2, z2)
				.color(r, g, b, a)
				.uv(1, 1)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx5, ny5, nz5)
				.endVertex();

		consumer.vertex(x7, y7, z7)
				.color(r, g, b, a)
				.uv(1, 0)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(lightmap)
				.normal(nx5, ny5, nz5)
				.endVertex();
	}

}
