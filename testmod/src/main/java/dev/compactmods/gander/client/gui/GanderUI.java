package dev.compactmods.gander.client.gui;

import com.mojang.blaze3d.platform.InputConstants;

import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.ui.widget.SpatialRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

public class GanderUI extends Screen {

    private BakedLevel scene;
    private SpatialRenderer activeRenderer;
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        if(activeRenderer != null) {
            var renderArea = activeRenderer.getRenderArea();
            graphics.fill(renderArea.left(), renderArea.top(), renderArea.right(), renderArea.bottom(),
                FastColor.ARGB32.color(120, CommonColors.BLACK));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (this.sceneSource != null) {
            graphics.pose().pushPose();
            graphics.drawCenteredString(font, sceneSource, width / 2, 10, DyeColor.WHITE.getFireworkColor());
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = Mth.floor(mouseX);
        int y = Mth.floor(mouseY);
        var renderClicked = renderables.stream()
            .filter(SpatialRenderer.class::isInstance)
            .map(SpatialRenderer.class::cast)
            .filter(r -> r.getRenderArea().containsPoint(x, y))
            .findFirst();

        renderClicked.ifPresent(r -> this.activeRenderer = r);
        return true;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if(this.activeRenderer != null)
            activeRenderer.zoom(pScrollY);

        return true;
    }

    @Override
    public boolean keyPressed(int code, int scanCode, int modifiers) {
        final float rotateSpeed = 1 / 12f;

        if (code == InputConstants.KEY_R) {
            activeRenderer.camera().resetLook();
            activeRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_UP) {
            activeRenderer.camera().lookUp(rotateSpeed);
            activeRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_DOWN) {
            activeRenderer.camera().lookDown(rotateSpeed);
            activeRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_LEFT) {
            activeRenderer.camera().lookLeft(rotateSpeed);
            activeRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_RIGHT) {
            activeRenderer.camera().lookRight(rotateSpeed);
            activeRenderer.recalculateTranslucency();
            return true;
        }

        return super.keyPressed(code, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void setSceneSource(Component src) {
        this.sceneSource = src;
    }

    public void setScene(BakedLevel scene) {
        this.scene = scene;

        this.renderables.clear();

        this.activeRenderer = addRenderableOnly(new SpatialRenderer(this.scene, 100, 20, 200, 100));
        this.activeRenderer.camera().zoom(-10);

        var s2 = addRenderableOnly(new SpatialRenderer(this.scene, 310, 20, 200, 100));
        s2.camera().lookDirection(Direction.DOWN);
        s2.camera().zoom(-10);

        var s3 = addRenderableOnly(new SpatialRenderer(this.scene, 100, 130, 200, 100));
        s3.camera().lookDirection(Direction.NORTH);
        s3.camera().zoom(-10);

        var s4 = addRenderableOnly(new SpatialRenderer(this.scene, 310, 130, 200, 100));
        s4.camera().lookDirection(Direction.WEST);
        s4.camera().zoom(-10);

        var s5 = addRenderableOnly(new SpatialRenderer(this.scene, 100, 240, 200, 100));
        s5.camera().lookDirection(Direction.UP);
        s5.camera().zoom(-10);
    }
}
