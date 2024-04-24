package dev.compactmods.gander.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class ScreenEntityRenderer {

	public static void renderEntities(Collection<Entity> entities, PoseStack ms, MultiBufferSource buffer, Camera ari, float pt) {
		Vec3 Vector3d = ari.getPosition();
		double d0 = Vector3d.x();
		double d1 = Vector3d.y();
		double d2 = Vector3d.z();

		for (Entity entity : entities) {
			if (entity.tickCount == 0) {
				entity.xOld = entity.getX();
				entity.yOld = entity.getY();
				entity.zOld = entity.getZ();
			}
			renderEntity(entity, d0, d1, d2, pt, ms, buffer);
		}

		var bs = Minecraft.getInstance().renderBuffers().bufferSource();
		bs.endBatch(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
		bs.endBatch(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
		bs.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
		bs.endBatch(RenderType.entitySmoothCutout(InventoryMenu.BLOCK_ATLAS));
	}

	private static void renderEntity(Entity entity, double x, double y, double z, float pt, PoseStack ms,
							  MultiBufferSource buffer) {
		double d0 = Mth.lerp(pt, entity.xOld, entity.getX());
		double d1 = Mth.lerp(pt, entity.yOld, entity.getY());
		double d2 = Mth.lerp(pt, entity.zOld, entity.getZ());
		float f = Mth.lerp(pt, entity.yRotO, entity.getYRot());
		EntityRenderDispatcher renderManager = Minecraft.getInstance()
				.getEntityRenderDispatcher();
		int light = renderManager.getRenderer(entity)
				.getPackedLightCoords(entity, pt);
		renderManager.render(entity, d0 - x, d1 - y, d2 - z, f, pt, ms, buffer, light);
	}

}
