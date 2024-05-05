package dev.compactmods.gander.render.translucency;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A builder for a {@link TranslucencyChain}
 */
public final class TranslucencyChainBuilder
{
	private final List<ResourceLocation> renderTargets;

	TranslucencyChainBuilder()
	{
		renderTargets = new ArrayList<>();
	}

	public TranslucencyChainBuilder addLayer(final ResourceLocation name)
	{
		// TODO: better exception
		if (renderTargets.contains(name))
		{
			throw new IllegalArgumentException("Already registered");
		}

		renderTargets.add(name);

		return this;
	}

	public TranslucencyChain build(final RenderTarget screenTarget)
	{
		final var chain = new TranslucencyChain(
			screenTarget,
			Collections.unmodifiableList(renderTargets));

		chain.resize(screenTarget.width, screenTarget.height);
		return chain;
	}
}
