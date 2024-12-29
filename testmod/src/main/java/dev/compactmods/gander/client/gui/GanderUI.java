package dev.compactmods.gander.client.gui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.ui.pipeline.BakedVirtualLevelScreenPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenLevelRenderingContext;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.BakedLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class GanderUI extends Screen {

    private BakedLevel scene;
    private final Supplier<BakedVirtualLevelScreenPipeline> pipeline = Suppliers.memoize(BakedVirtualLevelScreenPipeline::new);

    private final SceneCamera camera;
    private BakedLevelScreenLevelRenderingContext renderingContext;

    private Component sceneSource;

    GanderUI() {
        super(Component.empty());
        this.camera = new SceneCamera();
    }

    GanderUI(StructureSceneDataRequest dataRequest) {
        this();
        PacketDistributor.sendToServer(dataRequest);
    }

    @Override
    protected void init() {
        super.init();
        updateSceneRenderers();
    }

    private void updateSceneRenderers() {
        if (renderingContext != null)
            renderingContext.dispose();

        if (this.scene != null) {
            renderingContext = new BakedLevelScreenLevelRenderingContext(this.scene, width, height);
        }
    }

    private void disposeSceneRenderers() {
        if (renderingContext != null)
            renderingContext.dispose();
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

        if (this.scene == null || this.renderingContext == null)
            return;

        final var pipe = pipeline.get();

        var stack = graphics.pose();
        final var projMatrix = RenderSystem.getProjectionMatrix();

        final var frustum = new Frustum(new Matrix4f().rotate(camera.rotation().conjugate(new Quaternionf())),
            projMatrix);

        final var manager = pipe.createLifecycleManager();

        manager.setup(renderingContext, graphics, stack, camera, projMatrix);

        // TODO: Map layers somewhere - renderingContext.translucencyChain.layers()
        renderingContext.translucencyChain.prepareLayer(Gander.asResource("main"));

        // pipe.staticGeometryPass(renderingContext, graphics, partialTicks, RenderType.solid(), stack, renderingContext.camera, projMatrix);
        // pipe.staticGeometryPass(renderingContext, graphics, partialTicks, RenderType.cutoutMipped(), stack, renderingContext.camera, projMatrix);
        // pipe.staticGeometryPass(renderingContext, graphics, partialTicks, RenderType.cutout(), stack, renderingContext.camera, projMatrix);

        renderingContext.translucencyChain.prepareLayer(Gander.asResource("entity"));
        // pipe.blockEntitiesPass(renderingContext, graphics, partialTicks, stack, renderingContext.camera, frustum, graphics.bufferSource());

        renderingContext.translucencyChain.process();

//         manager.teardown(renderingContext, graphics, stack, renderingContext.camera, projMatrix);
    }

    private void renderSceneSourceLabel(GuiGraphics graphics) {
        if (this.sceneSource != null) {
            graphics.pose().pushPose();
            // graphics.pose().translate(getRectangle().left(), );
            graphics.drawCenteredString(font, sceneSource, width / 2, 10, DyeColor.WHITE.getFireworkColor());
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        camera.zoom((float) pScrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int code, int scanCode, int modifiers) {
        final float rotateSpeed = 1 / 12f;

        if (code == InputConstants.KEY_R) {
            camera.resetLook();
            renderingContext.recalculateTranslucency(camera);
            return true;
        }

        if (code == InputConstants.KEY_UP) {
            camera.lookUp(rotateSpeed);
            renderingContext.recalculateTranslucency(camera);
            return true;
        }

        if (code == InputConstants.KEY_DOWN) {
            camera.lookDown(rotateSpeed);
            renderingContext.recalculateTranslucency(camera);
            return true;
        }

        if (code == InputConstants.KEY_LEFT) {
            camera.lookLeft(rotateSpeed);
            renderingContext.recalculateTranslucency(camera);
            return true;
        }

        if (code == InputConstants.KEY_RIGHT) {
            camera.lookRight(rotateSpeed);
            renderingContext.recalculateTranslucency(camera);
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
        disposeSceneRenderers();
    }

    public void setSceneSource(Component src) {
        this.sceneSource = src;
    }

    public void setScene(BakedLevel scene) {
        this.scene = scene;
        updateSceneRenderers();
    }
}
