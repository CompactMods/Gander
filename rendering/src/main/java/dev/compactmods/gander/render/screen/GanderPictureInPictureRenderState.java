package dev.compactmods.gander.render.screen;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import dev.compactmods.gander.render.pipeline.PipelineState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;

public record GanderPictureInPictureRenderState(
    GuiGraphics graphics,
    Supplier<PipelineState> stateSupplier,
    ScreenRectangle renderArea
) implements PictureInPictureRenderState {

    @Override
    public int x0() {
        return renderArea.left();
    }

    @Override
    public int x1() {
        return renderArea.right();
    }

    @Override
    public int y0() {
        return renderArea.top();
    }

    @Override
    public int y1() {
        return renderArea.bottom();
    }


    @Override
    public float scale() {
        return 16f;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return renderArea;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return renderArea;
    }
}
