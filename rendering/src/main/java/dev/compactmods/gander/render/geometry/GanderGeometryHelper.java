package dev.compactmods.gander.render.geometry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionCompiler;

public class GanderGeometryHelper {

    private static boolean IS_INITIALIZED = false;
    public static SectionCompiler SECTION_COMPILER;

    public static boolean initialized() {
        return IS_INITIALIZED;
    }

    public static void setup() {
        final var mc = Minecraft.getInstance();

        SECTION_COMPILER = new SectionCompiler(mc.getBlockRenderer(), mc.getBlockEntityRenderDispatcher());

        IS_INITIALIZED = true;
    }
}
