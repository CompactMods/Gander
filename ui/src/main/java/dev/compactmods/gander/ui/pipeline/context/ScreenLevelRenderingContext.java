package dev.compactmods.gander.ui.pipeline.context;

import dev.compactmods.gander.render.pipeline.context.LevelRenderingContext;
import net.minecraft.client.GraphicsStatus;

public interface ScreenLevelRenderingContext extends LevelRenderingContext {
    GraphicsStatus previousGraphicsStatus();
    void setPreviousGraphicsStatus(GraphicsStatus status);
}
