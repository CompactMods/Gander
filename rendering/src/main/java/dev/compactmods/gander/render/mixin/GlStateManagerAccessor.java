package dev.compactmods.gander.render.mixin;

import com.mojang.blaze3d.platform.GlStateManager;

import com.mojang.blaze3d.platform.GlStateManager.TextureState;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor
{
	@Accessor("TEXTURES")
	static GlStateManager.TextureState[] getTEXTURES()
	{ throw new UnsupportedOperationException(); }

	@Accessor("activeTexture")
	static int getActiveTexture()
	{ throw new UnsupportedOperationException(); }
}
