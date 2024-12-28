package dev.compactmods.gander.ui.pipeline.context;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.VertexSorting;

import dev.compactmods.gander.core.Gander;
import dev.compactmods.gander.core.camera.SceneCamera;
import dev.compactmods.gander.level.VirtualLevel;
import dev.compactmods.gander.render.geometry.BakedLevel;
import dev.compactmods.gander.render.rendertypes.RenderTypeStore;
import dev.compactmods.gander.render.translucency.TranslucencyChain;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.stream.Collectors;

public class BakedLevelScreenLevelRenderingContext implements ScreenLevelRenderingContext {
    public SceneCamera camera;
    @Nullable
    public BlockAndTintGetter blockAndTints;
    public int width;
    public int height;
    @Nullable
    public BakedLevel bakedLevel;
    @Nullable BoundingBox blockBoundaries;
    public Set<BlockPos> blockEntityPositions;
    GraphicsStatus prevGraphics;
    public final RenderTarget renderTarget;
    public final TranslucencyChain translucencyChain;
    public final RenderTypeStore renderTypeStore;
    private boolean isDisposed;

    // SETUP
    private Matrix4f originalMatrix;
    private VertexSorting originalSorting;

    public BakedLevelScreenLevelRenderingContext(BakedLevel bakedLevel, int width, int height) {
        this.width = width;
        this.height = height;

        this.camera = new SceneCamera();

        this.bakedLevel = bakedLevel;
        this.blockAndTints = bakedLevel.originalLevel();
        this.blockBoundaries = bakedLevel.blockBoundaries();

        this.blockEntityPositions = BlockPos.betweenClosedStream(blockBoundaries)
            .filter(p -> blockAndTints.getBlockState(p).hasBlockEntity())
            .map(BlockPos::immutable)
            .collect(Collectors.toUnmodifiableSet());

        final var mc = Minecraft.getInstance();
        this.renderTarget = new TextureTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
        renderTarget.setClearColor(0, 0, 0, 0);

        this.translucencyChain = TranslucencyChain.builder()
            .addLayer(Gander.asResource("main"))
            .addLayer(Gander.asResource("entity"))
            .addLayer(Gander.asResource("water"))
            .addLayer(Gander.asResource("translucent"))
            .addLayer(Gander.asResource("item_entity"))
            .addLayer(Gander.asResource("particles"))
            .addLayer(Gander.asResource("clouds"))
            .addLayer(Gander.asResource("weather"))
            .build(renderTarget);

        this.renderTypeStore = new RenderTypeStore(this.translucencyChain);
        this.isDisposed = false;
    }

    @Override
    public GraphicsStatus previousGraphicsStatus() {
        return prevGraphics;
    }

    @Override
    public void setPreviousGraphicsStatus(GraphicsStatus status) {
        this.prevGraphics = status;
    }

    public void recalculateTranslucency() {
        if (bakedLevel != null) {
            bakedLevel.resortTranslucency(camera.getLookFrom());
        }
    }

    public void dispose() {
        if (isDisposed) return;
        this.isDisposed = true;
        renderTarget.destroyBuffers();
        renderTypeStore.dispose();
    }

    public boolean canRender() {
        return this.blockAndTints != null && this.blockBoundaries != null;
    }

    public void storePreviousRenderState() {
        this.originalMatrix = RenderSystem.getProjectionMatrix();
        this.originalSorting = RenderSystem.getVertexSorting();
    }

    public void restorePreviousRenderingState() {
        RenderSystem.setProjectionMatrix(originalMatrix, originalSorting);
    }
}
