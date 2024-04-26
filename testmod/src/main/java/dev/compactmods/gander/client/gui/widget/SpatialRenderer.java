package dev.compactmods.gander.client.gui.widget;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.level.BoundedBlockAndTintGetter;
import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class SpatialRenderer implements Renderable {

	private @Nullable BoundedBlockAndTintGetter blockAndTints;
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
		this.scale = 24f;
		this.blockEntityPositions = Collections.emptySet();
	}

	public void prepareCamera(Vector2f rotation) {
		var look = new Vector3f(0, 0, 1);
		look.rotateX(rotation.x);
		look.rotateY(rotation.y);
		look.mul(10);

		this.lookFrom = look;
	}

	public void shouldRenderCompass(boolean render) {
		this.shouldRenderCompass = render;
	}

	public void setData(@Nullable BoundedBlockAndTintGetter data) {
		this.blockAndTints = data;
		this.blockEntityPositions = BlockPos.betweenClosedStream(blockAndTints.bounds())
				.filter(p -> blockAndTints.getBlockState(p).hasBlockEntity())
				.map(BlockPos::immutable)
				.collect(Collectors.toUnmodifiableSet());

		this.shouldRenderCompass = true;
		this.compassOverlay.setBounds(data.bounds());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.blockAndTints == null)
			return;

		var buffer = Minecraft.getInstance().renderBuffers().bufferSource();

		final var renderBoundaries = blockAndTints.bounds();
		final var blockEntities = blockEntityPositions
				.stream()
				.map(blockAndTints::getBlockEntity)
				.filter(Objects::nonNull);

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();

		// has to be outside of MS transforms, important for vertex sorting
		RenderSystem.backupProjectionMatrix();
		Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

		PoseStack poseStack = graphics.pose();

		// Center (screen)
		poseStack.translate(width / 2f, height / 2f, 400);
		poseStack.mulPose(new Matrix4f().scaling(1, -1, 1));

		poseStack.scale(scale, scale, scale);

		poseStack.pushPose();
		{
			camera.lookAt(this.lookFrom, cameraTarget, new Vector3f(0, 1, 0));

			poseStack.mulPose(camera.rotation());
			poseStack.translate(renderBoundaries.getXSpan() / -2f, -1f * (renderBoundaries.getYSpan() / 2f), renderBoundaries.getZSpan() / -2f);

			ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, buffer, partialTicks);
			ScreenBlockRenderer.render(blockAndTints, renderBoundaries, buffer, poseStack, partialTicks);

			// TODO Fix Particles
			// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);

//			ScreenFluidRenderer.renderFluidBox(new FluidStack(Fluids.WATER, 1),
//					AABB.encapsulatingFullBlocks(BlockPos.ZERO.above(5), new BlockPos(6, 10, 6)),
//					buffer, poseStack, LightTexture.FULL_BLOCK, true);

			if (this.shouldRenderCompass)
				this.compassOverlay.render(graphics, mouseX, mouseY, partialTicks);
		}
		poseStack.popPose();

		RenderSystem.restoreProjectionMatrix();
	}

	public void scale(double scale) {
		this.scale += scale;
		this.scale = Mth.clamp(this.scale, 1, 50);
	}
}
