package dev.compactmods.gander.core;


import net.minecraft.resources.ResourceLocation;

public class Gander {

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath("gander", path);
    }

}
