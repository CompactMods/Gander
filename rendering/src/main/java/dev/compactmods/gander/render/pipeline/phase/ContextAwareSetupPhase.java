package dev.compactmods.gander.render.pipeline.phase;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.Camera;

public interface ContextAwareSetupPhase<TCtx> extends PipelinePhase {

    boolean setup(PipelineState state, TCtx ctx, Camera camera);
}
