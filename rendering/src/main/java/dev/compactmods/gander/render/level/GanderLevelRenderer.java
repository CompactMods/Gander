package dev.compactmods.gander.render.level;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.render.pipeline.PipelineState;
import dev.compactmods.gander.render.toolkit.GanderRenderToolkit;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GanderLevelRenderer extends LevelRenderer {

    private final Minecraft game;
    private final Supplier<PipelineState> pipelineStateSupplier;
    protected int tickCounter;

    public GanderLevelRenderer(Minecraft minecraft,
                               EntityRenderDispatcher entityRenderDispatcher,
                               BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                               RenderBuffers renderBuffers,
                               Supplier<PipelineState> pipelineStateSupplier
    ) {
        super(minecraft, entityRenderDispatcher, blockEntityRenderDispatcher, renderBuffers);
        this.game = minecraft;
        this.pipelineStateSupplier = pipelineStateSupplier;
        this.tickCounter = 0;
    }

    @Override
    public void tickRain(Camera camera) {

    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return "Gander Level Renderer";
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {

    }

    @Override
    public void initOutline() {

    }

    @Override
    public void doEntityOutline() {

    }

    @Override
    public boolean shouldShowEntityOutlines() {
        return false;
    }

    @Override
    public void setLevel(@Nullable ClientLevel level) {

    }

    @Override
    public void graphicsChanged() {

    }

    @Override
    public void allChanged() {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public String getSectionStatistics() {
        return null;
    }

    @Override
    public SectionRenderDispatcher getSectionRenderDispatcher() {
        return null;
    }

    @Override
    public double getTotalSections() {
        return 0;
    }

    @Override
    public double getLastViewDistance() {
        return 0;
    }

    @Override
    public int countRenderedSections() {
        throw new RuntimeException("Submit a bug report to the Gander team!");
    }

    @Override
    public String getEntityStatistics() {
        throw new RuntimeException("Submit a bug report to the Gander team!");
    }

    @Override
    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection renderSection) {
        throw new RuntimeException("Submit a bug report to the Gander team!");
    }

    @Override
    public void prepareCullFrustum(Vec3 cameraPosition, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        throw new RuntimeException("Submit a bug report to the Gander team!");
    }

    @Override
    public void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {

    }

    @Override
    public void captureFrustum() {

    }

    @Override
    public void killFrustum() {

    }

    @Override
    public void tick() {
        this.tickCounter++;
    }

    @Override
    public void renderSky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup) {

    }

    @Override
    public void renderClouds(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, double camX, double camY, double camZ) {

    }

    @Override
    public void blockChanged(BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    }

    @Override
    public void setBlockDirty(BlockPos pos, BlockState oldState, BlockState newState) {

    }

    @Override
    public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {

    }

    @Override
    public void setSectionDirty(int sectionX, int sectionY, int sectionZ) {

    }

    @Override
    public Frustum getFrustum() {
        var state = pipelineStateSupplier.get();
        return state.get(GanderRenderToolkit.CULLING_FRUSTUM);
    }

    @Override
    public int getTicks() {
        return this.tickCounter;
    }

    @Override
    public void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {

    }

    @Override
    public void requestOutlineEffect() {

    }

    @Override
    public void playJukeboxSong(Holder<JukeboxSong> song, BlockPos pos) {
        // Mojang why.
    }

    @Override
    public void stopJukeboxSongAndNotifyNearby(BlockPos pos) {
        // Why are there jukebox methods in here.
    }

    @Override
    public void addParticle(ParticleOptions options, boolean force, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

    }

    @Override
    public void addParticle(ParticleOptions options, boolean force, boolean decreased, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void globalLevelEvent(int type, BlockPos pos, int data) {

    }

    @Override
    public void levelEvent(int type, BlockPos pos, int data) {

    }

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {

    }

    @Override
    public boolean hasRenderedAllSections() {
        return true;
    }

    @Override
    public void onChunkLoaded(ChunkPos chunkPos) {

    }

    @Override
    public void needsUpdate() {

    }

    @Override
    public void updateGlobalBlockEntities(Collection<BlockEntity> blockEntitiesToRemove, Collection<BlockEntity> blockEntitiesToAdd) {

    }

    @Override
    public boolean isSectionCompiled(BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable RenderTarget entityTarget() {
        return null;
    }

    @Override
    public @Nullable RenderTarget getTranslucentTarget() {
        return null;
    }

    @Override
    public @Nullable RenderTarget getItemEntityTarget() {
        return null;
    }

    @Override
    public @Nullable RenderTarget getParticlesTarget() {
        return null;
    }

    @Override
    public @Nullable RenderTarget getWeatherTarget() {
        return null;
    }

    @Override
    public @Nullable RenderTarget getCloudsTarget() {
        return null;
    }
}
