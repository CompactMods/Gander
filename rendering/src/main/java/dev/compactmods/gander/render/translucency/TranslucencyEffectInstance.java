package dev.compactmods.gander.render.translucency;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.compactmods.gander.render.mixin.GlStateManagerAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Map;
import java.util.function.IntSupplier;

// This is needed because Mojank:tm:
public class TranslucencyEffectInstance extends EffectInstance
{
	protected final Object2IntMap<String> samplerTextureMap = new Object2IntOpenHashMap<>();

	public TranslucencyEffectInstance(final ResourceProvider pResourceProvider, final String pName) throws IOException
	{
		super(pResourceProvider, pName);
		samplerTextureMap.defaultReturnValue(GlConst.GL_TEXTURE_2D);
	}

	@Override
	public void apply()
	{
		RenderSystem.assertOnGameThread();
		this.dirty = false;
		lastAppliedEffect = this;
		this.blend.apply();
		if (this.programId != lastProgramId) {
			ProgramManager.glUseProgram(this.programId);
			lastProgramId = this.programId;
		}

		for (int i = 0; i < this.samplerLocations.size(); i++) {
			String name = this.samplerNames.get(i);
			IntSupplier textureIdSupplier = this.samplerMap.get(name);
			if (textureIdSupplier != null) {
				RenderSystem.activeTexture(33984 + i);
				int textureId = textureIdSupplier.getAsInt();
				int textureKind = samplerTextureMap.getInt(name);
				if (textureId != -1) {
					_bindTexture(textureId, textureKind);
					Uniform.uploadInteger(this.samplerLocations.get(i), i);
				}
			}
		}

		for (Uniform uniform : this.uniforms) {
			uniform.upload();
		}
	}

	private static void _bindTexture(int texture, int kind) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (texture != GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding) {
			GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].binding = texture;
			GL11.glBindTexture(kind, texture);
		}
	}

	public void setSampler(final String name, final IntSupplier textureId, final int textureType)
	{
		super.setSampler(name, textureId);
		this.samplerTextureMap.put(name, textureType);
	}
}
