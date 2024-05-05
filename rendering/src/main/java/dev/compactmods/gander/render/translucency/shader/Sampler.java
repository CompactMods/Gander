package dev.compactmods.gander.render.translucency.shader;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.mixin.GlStateManagerAccessor;

import org.lwjgl.opengl.GL11;

record Sampler(int location, int textureSlot)
{
	public static Sampler get(int programId, String name, int textureSlot)
	{
		return new Sampler(
			Uniform.glGetUniformLocation(programId, name),
			textureSlot);
	}

	public void bind(int textureId, int textureKind)
	{
		RenderSystem.activeTexture(textureSlot);
		_bindTexture(textureId, textureKind);
		Uniform.uploadInteger(location, textureSlot - GlConst.GL_TEXTURE0);
	}

	public void unbind()
	{
		RenderSystem.activeTexture(textureSlot);
		GlStateManager._bindTexture(0);
	}

	private static void _bindTexture(int texture, int kind) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (texture != GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding) {
			GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding = texture;
			GL11.glBindTexture(kind, texture);
		}
	}
}
