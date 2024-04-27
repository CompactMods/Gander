package dev.compactmods.gander.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.minecraft.world.level.BlockAndTintGetter;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

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

	private final Vector2f cameraRotation;
	private static final Vector2f DEFAULT_ROTATION = new Vector2f((float) Math.toRadians(-25), (float) Math.toRadians(-135));

	private final CompassOverlay compassOverlay;
	private int screenWidth;
	private int screenHeight;

	private final SceneCamera camera;
	private final Vector3f cameraTarget;

	private Vector3f lookFrom;
	private boolean shouldRenderCompass;
	private float scale;

	public SpatialRenderer(int screenWidth, int screenHeight) {
		this.compassOverlay = new CompassOverlay();
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;

		this.camera = new SceneCamera();
		this.cameraTarget = new Vector3f();
		this.lookFrom = new Vector3f();
		this.shouldRenderCompass = false;
		this.scale = 1f;
		this.blockEntityPositions = Collections.emptySet();
		this.cameraRotation = new Vector2f(DEFAULT_ROTATION);

		this.setCameraRotation(DEFAULT_ROTATION);
	}

	public void setCameraRotation(Vector2f rotation) {
		var newLookFrom = new Vector3f(0, 0, 1);
		newLookFrom.rotateX(rotation.x);
		newLookFrom.rotateY(rotation.y);
		// look.mul(16);

		if (lookFrom.distance(newLookFrom) > 1 && bakedLevel != null)
			bakedLevel.resortTranslucency(newLookFrom);

		this.lookFrom = newLookFrom;
		camera.lookAt(this.lookFrom, cameraTarget, new Vector3f(0, 1, 0));
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

		PoseStack poseStack = graphics.pose();

		poseStack.pushPose();
		{
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();

			// has to be outside of MS transforms, important for vertex sorting
			RenderSystem.backupProjectionMatrix();
			Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
			RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

			poseStack.translate(0, 0, -10000);

			// Center (screen)
			poseStack.translate(screenWidth / 2f, screenHeight / 2f, 400);
			poseStack.mulPoseMatrix(new Matrix4f().negateY());

			// poseStack.scale(2.5f, 2.5f, 2.5f);

			poseStack.pushPose();
			{
				poseStack.scale(16, 16, 16);

				poseStack.mulPose(camera.rotation());
				poseStack.translate(blockBoundaries.getXSpan() / -2f,
						-1f * (blockBoundaries.getYSpan() / 2f),
						blockBoundaries.getZSpan() / -2f);

				if (bakedLevel != null) {
					// ScreenBlockRenderer.render(bakedLevel, poseStack, lookFrom, projectionMatrix, partialTicks);

					ScreenBlockRenderer.maybePrepareTranslucency(RenderType.solid());
					ScreenBlockRenderer.renderSectionLayer(bakedLevel, RenderType.solid(), poseStack, lookFrom, projectionMatrix);

					ScreenBlockRenderer.maybePrepareTranslucency(RenderType.cutoutMipped());
					ScreenBlockRenderer.renderSectionLayer(bakedLevel, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);

					ScreenBlockRenderer.maybePrepareTranslucency(RenderType.cutout());
					ScreenBlockRenderer.renderSectionLayer(bakedLevel, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);

					ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, lookFrom, buffer, partialTicks);

					ScreenBlockRenderer.maybePrepareTranslucency(RenderType.translucent());
					ScreenBlockRenderer.renderSectionLayer(bakedLevel, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
					ScreenBlockRenderer.processTranslucency(partialTicks);
				}
			}
			poseStack.popPose();

			RenderSystem.restoreProjectionMatrix();
		}
		poseStack.popPose();
		// TODO Fix Particles
		// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);
	}

	public void scale(double scale) {
		this.scale += scale;
		this.scale = Mth.clamp(this.scale, 1, 50);
	}

	public void resize(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
		this.setCameraRotation(cameraRotation);
	}
}
