package dev.compactmods.gander.render.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.compactmods.gander.render.baked.model.ModelRebaker;
import dev.compactmods.gander.render.baked.texture.AtlasBaker;
import dev.compactmods.gander.render.mixin.accessor.ModelManagerAccessor;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.AtlasSet.StitchResult;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.util.perf.Profiler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin
{
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
        return AtlasBaker.bakeAtlases(original, pBackgroundExecutor);
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
        return original.thenAcceptAsync(
            x -> ModelRebaker.rebakeModels((ModelManagerAccessor)this, pReloadProfiler),
            pBackgroundExecutor);
    }
}
