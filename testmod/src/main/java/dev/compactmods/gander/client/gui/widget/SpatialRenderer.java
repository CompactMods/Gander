package dev.compactmods.gander.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import com.mojang.math.Axis;

import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.level.BoundedBlockAndTintGetter;
import dev.compactmods.gander.render.ScreenFluidRenderer;
import dev.compactmods.gander.render.baked.BakedLevel;
import dev.compactmods.gander.render.baked.LevelBakery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SpatialRenderer implements Renderable {

	private @Nullable BlockAndTintGetter blockAndTints;
	private @Nullable BakedLevel bakedLevel;
	private @Nullable BoundingBox blockBoundaries;
	private Set<BlockPos> blockEntityPositions;

	private final CompassOverlay compassOverlay;
	private final int width;
	private final int height;

	private final SceneCamera camera;
	private final Vector3f cameraTarget;

	private Vector3f lookFrom;
	private boolean shouldRenderCompass;
	private float scale;

	public SpatialRenderer(int width, int height) {
		this.compassOverlay = new CompassOverlay();
		this.width = width;
		this.height = height;

		this.camera = new SceneCamera();
		this.cameraTarget = new Vector3f();
		this.lookFrom = new Vector3f();
		this.shouldRenderCompass = false;
		this.scale = 1f;
		this.blockEntityPositions = Collections.emptySet();
	}

	public void prepareCamera(Vector2f rotation) {
		var look = new Vector3f(0, 0, 1);
		look.rotateX(rotation.x);
		look.rotateY(rotation.y);
		// look.mul(16);

		if (lookFrom.distance(look) > 1 && bakedLevel != null)
			bakedLevel.resortTranslucency(look);

		this.lookFrom = look;
	}

	public void shouldRenderCompass(boolean render) {
		this.shouldRenderCompass = render;
	}

	public void setData(BakedLevel bakedLevel) {
		this.bakedLevel = bakedLevel;
		this.blockAndTints = bakedLevel.originalLevel().get();
		this.blockBoundaries = bakedLevel.blockBoundaries();

		this.blockEntityPositions = BlockPos.betweenClosedStream(blockBoundaries)
				.filter(p -> blockAndTints.getBlockState(p).hasBlockEntity())
				.map(BlockPos::immutable)
				.collect(Collectors.toUnmodifiableSet());

		this.shouldRenderCompass = true;
		this.compassOverlay.setBounds(blockBoundaries);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.blockAndTints == null || this.blockBoundaries == null)
			return;

		var buffer = graphics.bufferSource();

		final var blockEntities = blockEntityPositions
				.stream()
				.map(blockAndTints::getBlockEntity)
				.filter(Objects::nonNull);

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();

		// has to be outside of MS transforms, important for vertex sorting
		RenderSystem.backupProjectionMatrix();
		Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
		RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

		PoseStack poseStack = graphics.pose();
		poseStack.translate(0, 0, -10000);

		// Center (screen)
		poseStack.translate(width / 2f, height / 2f, 400);
		poseStack.mulPoseMatrix(new Matrix4f().negateY());

		// poseStack.scale(2.5f, 2.5f, 2.5f);

		camera.lookAt(this.lookFrom, cameraTarget, new Vector3f(0, 1, 0));

		poseStack.pushPose();
		{
//			poseStack.scale(16, 16, 16);

			poseStack.mulPose(camera.rotation());
			poseStack.translate(blockBoundaries.getXSpan() / -2f,
					-1f * (blockBoundaries.getYSpan() / 2f),
					blockBoundaries.getZSpan() / -2f);

			if (bakedLevel != null) {
				ScreenBlockRenderer.renderBakedLevel(bakedLevel, poseStack, camera.getPosition().toVector3f(), projectionMatrix);
				ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, camera.getPosition().toVector3f(), buffer, partialTicks);
			}
		}
		poseStack.popPose();

		RenderSystem.restoreProjectionMatrix();

		// TODO Fix Particles
		// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);
	}

	public void scale(double scale) {
		this.scale += scale;
		this.scale = Mth.clamp(this.scale, 1, 50);
	}
}
