package dev.compactmods.gander.client;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.ui.widget.SpatialRenderer;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.baked.BakedLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import org.jetbrains.annotations.NotNull;

public class GanderUI extends Screen {

	private BakedLevel scene;
	private SpatialRenderer renderer;
	protected boolean autoRotate = false;

	private Component sceneSource;

	GanderUI() {
		super(Component.empty());
	}

	@Override
	protected void init() {
		super.init();
		this.renderer = this.addRenderableWidget(new SpatialRenderer(width, height));
		updateSceneRenderers();
	}

	private void updateSceneRenderers() {
		if (this.scene != null) {
			renderer.setData(scene);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.scene != null) {
			var level = ((VirtualLevel) scene.originalLevel().get());
			level.tick(minecraft.getPartialTick());
			level.animateTick();
		}

		if (autoRotate) {
			this.renderer.camera().lookLeft((float) Math.toRadians(2.5));
			this.renderer.recalculateTranslucency();
		}
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		if (this.sceneSource != null) {
			graphics.pose().pushPose();
			graphics.drawCenteredString(font, sceneSource, width / 2, 10, DyeColor.WHITE.getFireworkColor());
			graphics.pose().popPose();
		}
	}

	@Override
	public boolean mouseScrolled(double pMouseX, double pMouseY, double pScroll) {
		this.renderer.zoom(pScroll);
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
			renderer.camera().resetLook();
			this.renderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_UP) {
			renderer.camera().lookUp(rotateSpeed);
			this.renderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_DOWN) {
			renderer.camera().lookDown(rotateSpeed);
			this.renderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_LEFT) {
			renderer.camera().lookLeft(rotateSpeed);
			this.renderer.recalculateTranslucency();
			return true;
		}

		if (code == InputConstants.KEY_RIGHT) {
			renderer.camera().lookRight(rotateSpeed);
			this.renderer.recalculateTranslucency();
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
		if (this.scene != null) {
			renderer.dispose();
			renderables.remove(renderer);
		}
	}

	public void setSceneSource(Component src) {
		this.sceneSource = src;
	}

	public void setScene(BakedLevel scene) {
		this.scene = scene;
		updateSceneRenderers();
	}
}
