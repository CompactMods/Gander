package dev.compactmods.gander.render.screen;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.render.level.GanderLevelRenderer;
import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.function.Supplier;

public class GanderScreenToolkit {

    public static boolean switchToFabulous(PipelineState state) {
        final var opts = Minecraft.getInstance().options;
        state.set(GanderRenderToolkit.PREVIOUS_GRAPHICS_MODE, opts.graphicsMode().get());
        opts.graphicsMode().set(GraphicsStatus.FABULOUS);
        return true;
    }

    public static boolean revertGraphicsMode(PipelineState pipelineState) {
        final var opts = Minecraft.getInstance().options;
        opts.graphicsMode().set(pipelineState.get(GanderRenderToolkit.PREVIOUS_GRAPHICS_MODE));
        return true;
    }

    public static boolean setupRenderTarget(PipelineState state) {
        final var mc = Minecraft.getInstance();

        final var renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 0);

        state.set(GanderRenderToolkit.RENDER_TARGET, renderTarget);
        return true;
    }

    public static boolean setupTranslucencyChain(PipelineState pipelineState) {
        final var renderTarget = pipelineState.get(GanderRenderToolkit.RENDER_TARGET);

        final var translucencyChain = TranslucencyChain.builder()
            .addLayer(Gander.asResource("main"))
            .addLayer(Gander.asResource("entity"))
            .addLayer(Gander.asResource("water"))
            .addLayer(Gander.asResource("translucent"))
            .addLayer(Gander.asResource("item_entity"))
            .addLayer(Gander.asResource("particles"))
            .addLayer(Gander.asResource("clouds"))
            .addLayer(Gander.asResource("weather"))
            .build(renderTarget);

        pipelineState.set(GanderRenderToolkit.TRANSLUCENCY_CHAIN, translucencyChain);

        final var renderTypeStore = new RenderTypeStore(translucencyChain);
        pipelineState.set(GanderRenderToolkit.RENDER_TYPE_STORE, renderTypeStore);
        return true;
    }

    public static void backupProjectionMatrix(PipelineState state) {
        state.set(GanderRenderToolkit.ORIGINAL_MATRIX, RenderSystem.getProjectionMatrix());
        state.set(GanderRenderToolkit.ORIGINAL_VERTEX_SORTING, RenderSystem.getVertexSorting());
    }

    public static void restoreProjectionMatrix(PipelineState state) {
        RenderSystem.setProjectionMatrix(state.get(GanderRenderToolkit.ORIGINAL_MATRIX), state.get(GanderRenderToolkit.ORIGINAL_VERTEX_SORTING));
    }

    public static Matrix4f getProjectionMatrix(PipelineState state, double fov) {
        final var renderBounds = state.get(GanderRenderToolkit.RENDER_BOUNDS);
        Matrix4f matrix4f = new Matrix4f();

        return matrix4f.perspective(
            (float)(fov * (float) (Math.PI / 180.0)),
            (float) renderBounds.width() / renderBounds.height(),
            0.05F,
            32f // From GameRenderer: renderDistance * 4
        );
    }

    public static Matrix4f getViewMatrix(Camera camera) {
        final var quaternionf = camera.rotation().conjugate(new Quaternionf());
        return new Matrix4f().rotation(quaternionf);
    }

    public static Frustum makeCullFrustum(PipelineState state) {
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var frustumMatrix = state.getOrDefault(GanderRenderToolkit.MODEL_VIEW_MATRIX, getViewMatrix(camera));
        final var projectionMatrix = state
            .getOrDefault(GanderRenderToolkit.PROJECTION_MATRIX, getProjectionMatrix(state, 70));

        var cullingFrustum = new Frustum(frustumMatrix, projectionMatrix);
        cullingFrustum.prepare(camera.getPosition().x(), camera.getPosition().y(), camera.getPosition().z());

        return cullingFrustum;
    }

    // TODO: Maybe subclass the level renderer here - but AUGH that thing is hardcoded af
    public static LevelRenderer makeLevelRenderer(Supplier<PipelineState> stateSupplier) {
        final var mc = Minecraft.getInstance();
        return new GanderLevelRenderer(
            mc,
            mc.getEntityRenderDispatcher(),
            mc.getBlockEntityRenderDispatcher(),
            new RenderBuffers(Runtime.getRuntime().availableProcessors()),
            stateSupplier
        );
    }

    public static RenderLevelStageEvent makeRenderStageEvent(RenderLevelStageEvent.Stage stage, PipelineState state) {
        final var mc = Minecraft.getInstance();
        final var levelRenderer = state.get(GanderRenderToolkit.LEVEL_RENDERER);
        final var poseStack = new PoseStack();
        final var camera = state.get(GanderRenderToolkit.CAMERA);
        final var projMatrix = state.get(GanderRenderToolkit.PROJECTION_MATRIX);
        final var viewMatrix = state.get(GanderRenderToolkit.MODEL_VIEW_MATRIX);
        final var frustum = state.get(GanderRenderToolkit.CULLING_FRUSTUM);

        return new RenderLevelStageEvent(stage, levelRenderer, poseStack, viewMatrix, projMatrix,
            levelRenderer.getTicks(), mc.getTimer(), camera, frustum);
    }
}
