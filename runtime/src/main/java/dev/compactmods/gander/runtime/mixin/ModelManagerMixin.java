package dev.compactmods.gander.runtime.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.compactmods.gander.runtime.baked.texture.AtlasIndexer;
import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import dev.compactmods.gander.runtime.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.AtlasSet.StitchResult;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin
{
    @Unique
    @Final
    public ModelRebaker modelRebaker;
    @Unique
    @Final
    public AtlasIndexer atlasIndexer;

    @Unique
    private CompletableFuture<Void> rebakeMeshes(
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor)
    {
        // TODO: investigate whether we need any other hooks here
        return CompletableFuture.runAsync(
            () -> modelRebaker.rebakeModels((ModelManagerAccessor)this, pReloadProfiler),
            pBackgroundExecutor);
    }

    @Inject(
        method = "<init>",
        at = @At("RETURN"))
    private void construct(CallbackInfo ci)
    {
        this.modelRebaker = new ModelRebaker();
        this.atlasIndexer = new AtlasIndexer();
    }

    @ModifyExpressionValue(
        method = "reload",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/AtlasSet;scheduleLoad(Lnet/minecraft/server/packs/resources/ResourceManager;ILjava/util/concurrent/Executor;)Ljava/util/Map;"))
    public Map<ResourceLocation, CompletableFuture<StitchResult>> gander$invokeTextureBake(
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> original,

        PreparableReloadListener.PreparationBarrier pPreparationBarrier,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor)
    {
        return atlasIndexer.bakeAtlasIndices(original, pBackgroundExecutor);
    }

    @ModifyReturnValue(
        method = "reload",
        at = @At("RETURN"))
    public CompletableFuture<Void> gander$invokeMeshRebake(
        CompletableFuture<Void> original,

        PreparableReloadListener.PreparationBarrier pPreparationBarrier,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor)
    {
        return original.thenCompose(nothing -> rebakeMeshes(pReloadProfiler, pBackgroundExecutor));
    }
}
