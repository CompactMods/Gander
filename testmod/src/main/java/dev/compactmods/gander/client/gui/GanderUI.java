package dev.compactmods.gander.client.gui;

import java.io.IOException;

import com.mojang.blaze3d.platform.InputConstants;

import dev.compactmods.gander.GanderLib;
import dev.compactmods.gander.client.gui.widget.SpatialRenderer;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public class GanderUI extends Screen {

	protected boolean autoRotate = false;

	private BakedLevel scene;
	//	private SpatialRenderer topRenderer;
//	private SpatialRenderer frontRenderer;
//	private SpatialRenderer leftRenderer;
	private SpatialRenderer orthoRenderer;

	private Component sceneSource;
	private ScreenRectangle renderableArea;

	GanderUI() {
		super(Component.empty());
	}

	GanderUI(StructureSceneDataRequest dataRequest) {
		this();
		GanderLib.CHANNEL.sendToServer(dataRequest);
	}

	@Override
	protected void init() {
		super.init();

		int totalWidth = 400 + 10 + 120;
		int totalHeight = 320;

		this.renderableArea = new ScreenRectangle((width - totalWidth) / 2, (height - totalHeight) / 2, totalWidth, totalHeight);

		int runningY = renderableArea.top();

		try {
//			this.topRenderer = this.addRenderableWidget(new SpatialRenderer(renderableArea.left(), runningY, 120, 100));
			//		this.topRenderer.camera().lookDirection(Direction.DOWN);
			runningY += 110;

//			this.frontRenderer = this.addRenderableWidget(new SpatialRenderer(renderableArea.left(), runningY, 120, 100));
			//		this.frontRenderer.camera().lookDirection(Direction.NORTH);
			runningY += 110;

//			this.leftRenderer = this.addRenderableWidget(new SpatialRenderer(renderableArea.left(), runningY, 120, 100));

//			this.orthoRenderer = this.addRenderableWidget(new SpatialRenderer(
//					renderableArea.left() + 120 + 10, renderableArea.top(),
//					400, 320));

			this.orthoRenderer = this.addRenderableWidget(new SpatialRenderer(
					0, 0,
					width, height));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		updateSceneRenderers();
	}

	private void updateSceneRenderers() {
		if (this.scene != null) {
//			topRenderer.setData(scene);
//			frontRenderer.setData(scene);
//			leftRenderer.setData(scene);
			orthoRenderer.setData(scene);
		}
	}

	private void disposeSceneRenderers() {
		if (this.scene != null) {
//			topRenderer.setData(scene);
//			frontRenderer.setData(scene);
//			leftRenderer.setData(scene);
			orthoRenderer.dispose();
			renderables.remove(orthoRenderer);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.scene != null) {
			// TODO: :)
			var level = ((VirtualLevel)scene.originalLevel().get());
			level.tick(minecraft.getPartialTick());
			// level.animateTick();
		}

		if (autoRotate) {
			this.orthoRenderer.camera().lookLeft((float) Math.toRadians(2.5));
			this.orthoRenderer.recalculateTranslucency();
		}
	}

	/*@Override
	public void renderTransparentBackground(GuiGraphics pGuiGraphics) {

	}*/

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		if (this.sceneSource != null) {
			graphics.pose().pushPose();
			// graphics.pose().translate(getRectangle().left(), );
			graphics.drawCenteredString(font, sceneSource, width / 2, 10, DyeColor.WHITE.getFireworkColor());
			graphics.pose().popPose();
		}
		var poseStack = graphics.pose();

		poseStack.pushPose();
		{
//			graphics.drawCenteredString(font, frontRenderer.getRectangle().toString(), width / 2, 20, DyeColor.WHITE.getFireworkColor());

//			graphics.fill(topRenderer.getX(), topRenderer.getY(), topRenderer.getX() + topRenderer.getWidth(), topRenderer.getY() + topRenderer.getHeight(), CommonColors.WHITE);
//			graphics.fill(leftRenderer.getX(), leftRenderer.getY(), leftRenderer.getX() + leftRenderer.getWidth(), leftRenderer.getY() + leftRenderer.getHeight(), CommonColors.SOFT_RED);
//			graphics.fill(frontRenderer.getX(), frontRenderer.getY(), frontRenderer.getX() + frontRenderer.getWidth(), frontRenderer.getY() + frontRenderer.getHeight(), CommonColors.SOFT_YELLOW);
//			graphics.fill(orthoRenderer.getX(), orthoRenderer.getY(), orthoRenderer.getX() + orthoRenderer.getWidth(), orthoRenderer.getY() + orthoRenderer.getHeight(), CommonColors.BLACK);
		}
		poseStack.popPose();

	}

	@Override
	public boolean mouseScrolled(double pMouseX, double pMouseY, double pScroll) {
		this.orthoRenderer.zoom(pScroll);
		return true;
	}

	@Override
	public boolean keyPressed(int code, int scanCode, int modifiers) {
		final float rotateSpeed = 1 / 12f;

		if (code == InputConstants.KEY_A) {
			this.autoRotate = !autoRotate;
			return true;
		}

		if (code == InputConstants.KEY_R) {
			orthoRenderer.camera().resetLook();
			this.orthoRenderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_UP) {
			orthoRenderer.camera().lookUp(rotateSpeed);
			this.orthoRenderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_DOWN) {
			orthoRenderer.camera().lookDown(rotateSpeed);
			this.orthoRenderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_LEFT) {
			orthoRenderer.camera().lookLeft(rotateSpeed);
			this.orthoRenderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_RIGHT) {
			orthoRenderer.camera().lookRight(rotateSpeed);
			this.orthoRenderer.recalculateTranslucency();
			return true;
		}

		return super.keyPressed(code, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		this.disposeSceneRenderers();
	}

	public void setSceneSource(Component src) {
		this.sceneSource = src;
	}

	public void setScene(BakedLevel scene) {
		this.scene = scene;
		updateSceneRenderers();
	}
}
