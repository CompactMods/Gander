package dev.compactmods.gander.client.gui.widget;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.RenderTypeHelper;
import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.minecraft.world.level.BlockAndTintGetter;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpatialRenderer extends AbstractWidget {

	private @Nullable BlockAndTintGetter blockAndTints;
	private @Nullable BakedLevel bakedLevel;
	private @Nullable BoundingBox blockBoundaries;
	private Set<BlockPos> blockEntityPositions;

	private final CompassOverlay compassOverlay;

	private final SceneCamera camera;

	private boolean shouldRenderCompass;
	private float scale;

	private final RenderTarget RENDER_TARGET;
	private final PostChain translucencyChain;

	private boolean isDisposed = false;

	public SpatialRenderer(int x, int y, int width, int height) throws IOException {
		super(x, y, width, height, Component.empty());

		this.compassOverlay = new CompassOverlay();
		this.camera = new SceneCamera();
		this.shouldRenderCompass = false;
		this.scale = 1f;
		this.blockEntityPositions = Collections.emptySet();

		final var mc = Minecraft.getInstance();
		this.RENDER_TARGET = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);

		RENDER_TARGET.resize(RENDER_TARGET.width, RENDER_TARGET.height, Minecraft.ON_OSX);
//		RENDER_TARGET.setClearColor(0, 0, 0, 0);

		// pulled from LevelRenderer
		this.translucencyChain = new PostChain(mc.textureManager, mc.getResourceManager(), RENDER_TARGET, new ResourceLocation("shaders/post/transparency.json"));
		this.translucencyChain.resize(RENDER_TARGET.width, RENDER_TARGET.height);
	}

	public void dispose() {
		if(isDisposed) return;
		this.isDisposed = true;
		translucencyChain.close();
		RENDER_TARGET.destroyBuffers();
	}

	public SceneCamera camera() {
		return this.camera;
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
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
			poseStack.translate(getX(), getY(), 0);
			// graphics.fill(0,0, width, height, CommonColors.WHITE);
			renderScene(graphics, blockEntities, buffer, partialTicks);
		}
		poseStack.popPose();

		// TODO Fix Particles
		// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);
	}

	private void renderScene(GuiGraphics graphics, Stream<BlockEntity> blockEntities, MultiBufferSource.BufferSource buffer, float partialTicks) {
		PoseStack poseStack = graphics.pose();

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();

		RENDER_TARGET.clear(Minecraft.ON_OSX);
		RenderTypeHelper.OUTPUT_STATE_SHARD_MAP.values().forEach(output -> {
			final var t = translucencyChain.getRenderTarget(output);
			if (t != null) t.clear(Minecraft.ON_OSX);
		});
		RENDER_TARGET.bindWrite(true);

		renderSceneForRealsies(blockEntities, buffer, partialTicks, poseStack);

		final var mainTarget = Minecraft.getInstance().getMainRenderTarget();

		RenderSystem.backupProjectionMatrix();
		mainTarget.bindWrite(false);
		RENDER_TARGET.blitToScreen(RENDER_TARGET.width, RENDER_TARGET.height, false);
		RenderSystem.restoreProjectionMatrix();

		renderCompass(graphics, partialTicks, poseStack);
	}

	private void renderSceneForRealsies(Stream<BlockEntity> blockEntities, MultiBufferSource.BufferSource buffer, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			poseStack.translate(0, 0, -10000);

			// Center (screen)
			preparePose(poseStack);

			final var lookFrom = camera.getLookFrom();

			if (bakedLevel != null) {
//					final var projectionMatrix = new Matrix4f();
				final var projectionMatrix = RenderSystem.getProjectionMatrix();

				ScreenBlockRenderer.renderSectionLayer(bakedLevel, translucencyChain, RenderType.solid(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, translucencyChain, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, translucencyChain, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);

				ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, lookFrom, translucencyChain, buffer, partialTicks);

				ScreenBlockRenderer.prepareTranslucency(this.translucencyChain);
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, translucencyChain, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
				RENDER_TARGET.bindWrite(false);
				translucencyChain.process(partialTicks);
			}
		}

		poseStack.popPose();
	}

	private void renderCompass(GuiGraphics graphics, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			preparePose(poseStack);
			compassOverlay.render(graphics, partialTicks);
		}
		poseStack.popPose();
	}

	private void preparePose(PoseStack poseStack) {
		poseStack.translate(width / 2f, height / 2f, 400);
		poseStack.mulPoseMatrix(new Matrix4f().negateY());

		poseStack.scale(16, 16, 16);

		poseStack.mulPose(camera.rotation());
		poseStack.translate(blockBoundaries.getXSpan() / -2f,
				-1f * (blockBoundaries.getYSpan() / 2f),
				blockBoundaries.getZSpan() / -2f);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrator) {
	}

	public void scale(double scale) {
		this.scale += scale;
		this.scale = Mth.clamp(this.scale, 1, 50);
	}

	@Override
	protected boolean isValidClickButton(int pButton) {
		return false;
	}
}
