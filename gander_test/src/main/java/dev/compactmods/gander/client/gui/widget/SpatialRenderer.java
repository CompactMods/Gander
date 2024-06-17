package dev.compactmods.gander.client.gui.widget;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.ScreenBlockEntityRender;
import dev.compactmods.gander.render.ScreenBlockRenderer;
import dev.compactmods.gander.SceneCamera;
import dev.compactmods.gander.render.baked.BakedLevel;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

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

	private final RenderTarget renderTarget;
	private final TranslucencyChain translucencyChain;
	private final RenderTypeStore renderTypeStore;

	private boolean isDisposed = false;

	public SpatialRenderer(int x, int y, int width, int height) throws IOException {
		super(x, y, width, height, Component.empty());

		this.compassOverlay = new CompassOverlay();
		this.camera = new SceneCamera();
		this.shouldRenderCompass = false;
		this.blockEntityPositions = Collections.emptySet();

		final var mc = Minecraft.getInstance();
		this.renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
		renderTarget.setClearColor(0, 0, 0, 0);

		var translucencyChain = TranslucencyChain.builder()
				.addLayer(GanderTestMod.asResource("main"))
				.addLayer(GanderTestMod.asResource("entity"))
				.addLayer(GanderTestMod.asResource("water"))
				.addLayer(GanderTestMod.asResource("translucent"))
				.addLayer(GanderTestMod.asResource("item_entity"))
				.addLayer(GanderTestMod.asResource("particles"))
				.addLayer(GanderTestMod.asResource("clouds"))
				.addLayer(GanderTestMod.asResource("weather"))
				.build(renderTarget);

		this.translucencyChain = translucencyChain;
		this.renderTypeStore = new RenderTypeStore(translucencyChain);
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

		var projectionMatrix = new Matrix4f().setPerspective(
				(float)Math.PI / 2f,
				(float)renderTarget.width / (float)renderTarget.height,
				0.05f,
				10000000);

		final var blockEntities = blockEntityPositions
				.stream()
				.map(blockAndTints::getBlockEntity)
				.filter(Objects::nonNull);
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		{
			var poseStack2 = RenderSystem.getModelViewStack();
			poseStack2.pushMatrix();
			poseStack2.identity();
			RenderSystem.applyModelViewMatrix();

			poseStack.setIdentity();
			poseStack.mulPose(camera.rotation());

			translucencyChain.clear();
			translucencyChain.prepareBackgroundColor(Minecraft.getInstance().getMainRenderTarget());
			renderTarget.bindWrite(true);

			RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(camera.getLookFrom()));
			renderScene(blockEntities, buffer, partialTicks, poseStack);

			final var mainTarget = Minecraft.getInstance().getMainRenderTarget();

			mainTarget.bindWrite(true);
			UGH(renderTarget);

			RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.byDistance(camera.getLookFrom()));
			//renderCompass(graphics, partialTicks, poseStack);

			poseStack2.popMatrix();
			RenderSystem.applyModelViewMatrix();
		}
		poseStack.popPose();

		RenderSystem.setProjectionMatrix(originalMatrix, originalSorting);

		// TODO Fix Particles
		// scene.getLevel().renderParticles(pose, buffer, camera, partialTicks);

//			var entityGetter = scene.getLevel().getEntities(EntityTypeTest.forClass(Entity.class), AABB.of(scene.getBounds()), e -> true);
//			ScreenEntityRenderer.renderEntities(entityGetter, poseStack, buffer, camera, partialTicks);
	}

	private void UGH(RenderTarget renderTarget)
	{
		// RenderTarget.blit disables alpha... :unamused:
		RenderSystem.assertOnRenderThread();
		GlStateManager._disableDepthTest();
		GlStateManager._depthMask(false);
		GlStateManager._viewport(0, 0, renderTarget.width, renderTarget.height);

		Minecraft minecraft = Minecraft.getInstance();
		ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
		shaderinstance.setSampler("DiffuseSampler", renderTarget.getColorTextureId());
		Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, 3000.0F);
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
		if (shaderinstance.MODEL_VIEW_MATRIX != null) {
			shaderinstance.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0F, 0.0F, -2000.0F));
		}

		if (shaderinstance.PROJECTION_MATRIX != null) {
			shaderinstance.PROJECTION_MATRIX.set(matrix4f);
		}

		shaderinstance.apply();
		float f = (float)width;
		float f1 = (float)height;
		float f2 = (float)renderTarget.viewWidth / (float)renderTarget.width;
		float f3 = (float)renderTarget.viewHeight / (float)renderTarget.height;
		Tesselator tesselator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.vertex(0.0, f1, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex(f, f1, 0.0).uv(f2, 0.0F).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex(f, 0.0, 0.0).uv(f2, f3).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, f3).color(255, 255, 255, 255).endVertex();
		BufferUploader.draw(bufferbuilder.end());
		shaderinstance.clear();
		GlStateManager._depthMask(true);
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

				translucencyChain.prepareLayer(GanderTestMod.asResource("main"));
				ScreenBlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.solid(), poseStack, lookFrom, projectionMatrix);

				ScreenBlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.cutoutMipped(), poseStack, lookFrom, projectionMatrix);

				ScreenBlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.cutout(), poseStack, lookFrom, projectionMatrix);

				translucencyChain.prepareLayer(GanderTestMod.asResource("entity"));
				ScreenBlockEntityRender.render(blockAndTints, blockEntities, poseStack, lookFrom, renderTypeStore, buffer, partialTicks);

				translucencyChain.prepareLayer(GanderTestMod.asResource("water"));
				translucencyChain.prepareLayer(GanderTestMod.asResource("translucent"));
				ScreenBlockRenderer.renderSectionFluids(bakedLevel, renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);
				ScreenBlockRenderer.renderSectionBlocks(bakedLevel, renderTypeStore, RenderType.translucent(), poseStack, lookFrom, projectionMatrix);

				translucencyChain.prepareLayer(GanderTestMod.asResource("item_entity"));
				translucencyChain.prepareLayer(GanderTestMod.asResource("particles"));
				translucencyChain.prepareLayer(GanderTestMod.asResource("clouds"));
				translucencyChain.prepareLayer(GanderTestMod.asResource("weather"));

				translucencyChain.process();
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
