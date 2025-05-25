package dev.compactmods.gander.render.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface GlStateManagerAccessor
{
	// TODO 21.5 Port @Accessor("TEXTURES")
	// static GlStateManager.TextureState[] getTEXTURES() { throw new UnsupportedOperationException(); }

	@Accessor("activeTexture")
	static int getActiveTexture()
	{ throw new UnsupportedOperationException(); }
}
