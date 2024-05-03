package dev.compactmods.gander.render.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;

import com.mojang.blaze3d.vertex.VertexFormat;

import dev.compactmods.gander.render.rendertypes.GanderCompositeRenderType;
import dev.compactmods.gander.render.translucency.TranslucentRenderTargetLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(RenderType.CompositeRenderType.class)
public abstract class CompositeRenderTypeMixin extends RenderType implements GanderCompositeRenderType {

	@Shadow
	@Final
	private RenderType.CompositeState state;

	@Mutable
	@Shadow
	@Final
	private Optional<RenderType> outline;

	//region

	/**
	 * Required to extend RenderType
	 */
	private CompositeRenderTypeMixin(String n, VertexFormat f, VertexFormat.Mode m, int b, boolean a, boolean s, Runnable r1, Runnable r2) {
		super(n, f, m, b, a, s, r1, r2);
	}
	//endregion

	@Accessor
	abstract void setOutline(Optional<RenderType> outline);

	@Override
	@Accessor("state")
	public abstract CompositeState state();

	//region Actual code
	@Override
	public RenderType targetingTranslucentRenderTarget(TranslucentRenderTargetLayer newTarget, TranslucentRenderTargetLayer mainTarget) {
		var newState = new RenderType.CompositeState(state.textureState,
				state.shaderState, state.transparencyState, state.depthTestState, state.cullState,
				state.lightmapState, state.overlayState, state.layeringState, new RenderStateShard.OutputStateShard(newTarget.toString(), () -> {
			if (Minecraft.useShaderTransparency()) {
				newTarget.bindWrite(false);
			}
		}, () -> {
			if (Minecraft.useShaderTransparency()) {
				mainTarget.bindWrite(false);
			}
		}),
				state.texturingState, state.writeMaskState, state.lineState, state.colorLogicState,
				state.outlineProperty);

		var thing = RenderType.create(this.name, this.format(), this.mode(),
				this.bufferSize(), affectsCrumbling(), sortOnUpload, newState);

		((CompositeRenderTypeMixin) (Object) thing).setOutline(this.outline
				.map(o -> ((GanderCompositeRenderType) o).targetingTranslucentRenderTarget(mainTarget, mainTarget)));

		//noinspection UnreachableCode
		return thing;
	}
	//endregion
}
