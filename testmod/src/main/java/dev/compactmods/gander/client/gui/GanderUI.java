package dev.compactmods.gander.client.gui;

import com.mojang.blaze3d.platform.InputConstants;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.ui.widget.SpatialRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderUI extends Screen {

    private BakedLevel scene;
    private SpatialRenderer renderer;
    private Component sceneSource;

    GanderUI() {
        super(Component.empty());
    }

    GanderUI(StructureSceneDataRequest dataRequest) {
        this();
        PacketDistributor.sendToServer(dataRequest);
    }

    @Override
    protected void init() {
        super.init();
        if(this.scene != null)
            setScene(scene);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.scene != null) {
            // TODO: :)
            var level = ((VirtualLevel) scene.originalLevel());
            level.tick(minecraft.getTimer().getRealtimeDeltaTicks());
            // level.animateTick();
        }
    }

    @Override
    public void renderTransparentBackground(GuiGraphics pGuiGraphics) {

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderSceneSourceLabel(graphics);
    }

    private void renderSceneSourceLabel(GuiGraphics graphics) {
        if (this.sceneSource != null) {
            graphics.pose().pushPose();
            graphics.drawCenteredString(font, sceneSource, width / 2, 10, DyeColor.WHITE.getFireworkColor());
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if(this.renderer != null)
            renderer.zoom(pScrollY);

        return true;
    }

    @Override
    public boolean keyPressed(int code, int scanCode, int modifiers) {
        final float rotateSpeed = 1 / 12f;

        if (code == InputConstants.KEY_R) {
            renderer.camera().resetLook();
//            renderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_UP) {
            renderer.camera().lookUp(rotateSpeed);
//            renderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_DOWN) {
            renderer.camera().lookDown(rotateSpeed);
//            renderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_LEFT) {
            renderer.camera().lookLeft(rotateSpeed);
//            renderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_RIGHT) {
            renderer.camera().lookRight(rotateSpeed);
//            renderer.recalculateTranslucency();
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
        if(this.renderer != null)
            this.renderer.dispose();
    }

    public void setSceneSource(Component src) {
        this.sceneSource = src;
    }

    public void setScene(BakedLevel scene) {
        this.scene = scene;
        if(this.renderer != null) {
            this.renderer.dispose();
            this.renderables.remove(this.renderer);
        }

        this.renderer = addRenderableOnly(new SpatialRenderer(this.scene));
    }
}
