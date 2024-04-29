package dev.compactmods.gander.client.gui.widget;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.render.rendertypes.RedirectedRenderTypeStore;
import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.render.baked.BakedLevel;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

import org.lwjgl.opengl.GL11;

public class SpatialRenderer extends AbstractWidget {

	private @Nullable BlockAndTintGetter blockAndTints;
	private @Nullable BakedLevel bakedLevel;
	private @Nullable BoundingBox blockBoundaries;
	private Set<BlockPos> blockEntityPositions;

	private final CompassOverlay compassOverlay;

	private final SceneCamera camera;

	private boolean shouldRenderCompass;
	private float scale;

	private final RenderTarget renderTarget;
	private final PostChain translucencyChain;
	private final RenderTypeStore renderTypeStore;

	private boolean isDisposed = false;

	public SpatialRenderer(int x, int y, int width, int height) throws IOException {
		super(x, y, width, height, Component.empty());

		this.compassOverlay = new CompassOverlay();
		this.camera = new SceneCamera();
		this.shouldRenderCompass = false;
		this.scale = 16f;
		this.blockEntityPositions = Collections.emptySet();

		final var mc = Minecraft.getInstance();
		this.renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);

		renderTarget.enableStencil();
		renderTarget.setClearColor(0, 0, 0, 0);

		// pulled from LevelRenderer
		var translucencyChain = new PostChain(mc.textureManager, mc.getResourceManager(), renderTarget, GanderLib.asResource("shaders/post/transparency.json"));
		translucencyChain.resize(renderTarget.width, renderTarget.height);
		this.translucencyChain = translucencyChain;
		this.renderTypeStore = new RedirectedRenderTypeStore(translucencyChain);
	}

	public void dispose() {
		if(isDisposed) return;
		this.isDisposed = true;
		renderTarget.destroyBuffers();
		renderTypeStore.dispose();
	}

	public SceneCamera camera() {
		return camera;
	}

	public void recalculateTranslucency() {
		if (bakedLevel != null) {
			bakedLevel.resortTranslucency(camera.getLookFrom());
		}
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

		var width = Minecraft.getInstance().getWindow().getWidth();
		var height = Minecraft.getInstance().getWindow().getHeight();
		if (width != renderTarget.width || height != renderTarget.height)
		{
			renderTarget.resize(width, height, true);
			translucencyChain.resize(renderTarget.width, renderTarget.height);
			recalculateTranslucency();
		}

		var originalMatrix = RenderSystem.getProjectionMatrix();
		var originalSorting = RenderSystem.getVertexSorting();

		final var renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4;
		var projectionMatrix = new Matrix4f().setPerspective(
				(float)Math.PI / 2f,
				(float)renderTarget.width / (float)renderTarget.height,
				0.05f,
				renderDistance);

		final var blockEntities = blockEntityPositions
				.stream()
				.map(blockAndTints::getBlockEntity)
				.filter(Objects::nonNull);
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		{
			var poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushPose();
			poseStack2.setIdentity();
			RenderSystem.applyModelViewMatrix();

			poseStack.setIdentity();
			poseStack.mulPose(camera.rotation());
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();

			renderTypeStore.clear();
			renderTarget.bindWrite(true);
			renderMinecraft();

			RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(camera.getLookFrom()));
			renderScene(blockEntities, buffer, partialTicks, poseStack);

			RenderSystem.stencilFunc(GlConst.GL_EQUAL, 1, 0xFF);
			RenderSystem.stencilMask(0xFF);

			final var mainTarget = Minecraft.getInstance().getMainRenderTarget();

			mainTarget.bindWrite(true);
			renderTarget.blitToScreen(renderTarget.width, renderTarget.height, false);

			GL11.glDisable(GL11.GL_STENCIL_TEST);

			RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(camera.getLookFrom()));
			renderCompass(graphics, partialTicks, poseStack);

			poseStack2.popPose();
			RenderSystem.applyModelViewMatrix();
		}
		poseStack.popPose();

		RenderSystem.setProjectionMatrix(originalMatrix, originalSorting);

		// TODO Fix Particles
		// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);
	}

	private void renderMinecraft()
	{
		var target = renderTarget;
		var other = Minecraft.getInstance().getMainRenderTarget();
		GlStateManager._glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, other.frameBufferId);
		GlStateManager._glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0,
				0,
				other.width,
				other.height,
				0,
				0,
				target.width,
				target.height,
				GlConst.GL_COLOR_BUFFER_BIT,
				GlConst.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
	}

	private void renderScene(Stream<BlockEntity> blockEntities, MultiBufferSource.BufferSource buffer, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			poseStack.scale(16, 16, 16);
			poseStack.translate(
					blockBoundaries.getXSpan() / -2f,
					blockBoundaries.getYSpan() / -2f,
					blockBoundaries.getZSpan() / -2f);

			final var lookFrom = camera.getLookFrom();

			if (bakedLevel != null) {
				var projectionMatrix = RenderSystem.getProjectionMatrix();

				GL11.glEnable(GL11.GL_STENCIL_TEST);
				RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
				RenderSystem.stencilFunc(GlConst.GL_ALWAYS, 1, 0xFF);
				RenderSystem.stencilMask(0xFF);
				RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

				ScreenBlockRenderer.renderSectionLayer(bakedLevel, renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);

				ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, lookFrom, renderTypeStore, buffer, partialTicks);

				renderTypeStore.prepareTranslucency();
				ScreenBlockRenderer.renderSectionLayer(bakedLevel, renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);

				RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
				RenderSystem.stencilFunc(GlConst.GL_EQUAL, 1, 0xFF);
				RenderSystem.stencilMask(0xFF);

				renderTypeStore.processTransclucency(partialTicks);
			}
		}

		poseStack.popPose();
	}

	private void renderCompass(GuiGraphics graphics, float partialTicks, PoseStack poseStack) {
		poseStack.pushPose();
		{
			poseStack.translate(
					blockBoundaries.getXSpan() / -2f,
					blockBoundaries.getYSpan() / -2f,
					blockBoundaries.getZSpan() / -2f);

			var position = camera.getLookFrom();
			poseStack.translate(-position.x, -position.y, -position.z);
			poseStack.last().pose().negateY();
			poseStack.scale(1/16f, 1/16f, 1/16f);

			compassOverlay.render(graphics, partialTicks);
		}
		poseStack.popPose();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrator) {
	}

	public void zoom(double factor) {
		camera.zoom((float)factor);
	}

	@Override
	protected boolean isValidClickButton(int pButton) {
		return false;
	}
}
