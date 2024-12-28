package dev.compactmods.gander.client.gui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.InputConstants;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.RenderTypes;
import dev.compactmods.gander.ui.pipeline.BakedVirtualLevelScreenPipeline;
import dev.compactmods.gander.ui.pipeline.context.BakedLevelScreenLevelRenderingContext;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.network.StructureSceneDataRequest;
import dev.compactmods.gander.render.geometry.BakedLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class GanderUI extends Screen {

    protected boolean autoRotate = false;

    private BakedLevel scene;
    private final Supplier<BakedVirtualLevelScreenPipeline> pipeline = Suppliers.memoize(BakedVirtualLevelScreenPipeline::new);

    private BakedLevelScreenLevelRenderingContext renderingContext;

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

        if (autoRotate) {
//			this.orthoRenderer.camera().lookLeft((float) Math.toRadians(2.5));
//			this.orthoRenderer.recalculateTranslucency();
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

        pipe.setup(renderingContext, graphics, stack, renderingContext.camera, projMatrix);

        pipe.staticGeometryPass(renderingContext, graphics, partialTicks,
            RenderTypes.renderTypeForStage(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS),
            stack, renderingContext.camera, projMatrix);

//        for (var chunkRenderType : RenderTypes.GEOMETRY_STAGES.values()) {
//            // stack.mulPose(evt.getModelViewMatrix());
//            pipe.staticGeometryPass(renderingContext, graphics, partialTicks, chunkRenderType, stack, renderingContext.camera, projMatrix, new Vector3f());
//
////            if (chunkRenderType == G {
////            pipe.blockEntitiesPass(renderingContext, graphics, partialTicks,
////                evt.getPoseStack(),
////                evt.getCamera(),
////                evt.getFrustum(),
////                Minecraft.getInstance().renderBuffers().bufferSource(),
////                new Vector3f());
////        }
//        }

        pipe.teardown(renderingContext, graphics, stack, renderingContext.camera, projMatrix);
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
        // this.orthoRenderer.zoom(pScrollY);
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
//			orthoRenderer.camera().resetLook();
//			this.orthoRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_UP) {
//			orthoRenderer.camera().lookUp(rotateSpeed);
//			this.orthoRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_DOWN) {
//			orthoRenderer.camera().lookDown(rotateSpeed);
//			this.orthoRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_LEFT) {
//			orthoRenderer.camera().lookLeft(rotateSpeed);
//			this.orthoRenderer.recalculateTranslucency();
            return true;
        }

        if (code == InputConstants.KEY_RIGHT) {
//			orthoRenderer.camera().lookRight(rotateSpeed);
//			this.orthoRenderer.recalculateTranslucency();
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
