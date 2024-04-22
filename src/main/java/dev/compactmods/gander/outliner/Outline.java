package dev.compactmods.gander.outliner;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.MultiBufferSource;

import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.utility.Color;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public abstract class Outline {

	protected final OutlineParams params;

	protected final Vector4f colorTemp = new Vector4f();

	public Outline() {
		params = new OutlineParams();
	}

	public OutlineParams getParams() {
		return params;
	}

	public abstract void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera, float pt);

	public static class OutlineParams {
		protected Direction highlightedFace;
		protected boolean fadeLineWidth;
		protected boolean disableCull;
		protected boolean disableLineNormals;
		protected float alpha;
		protected int lightmap;
		protected Color rgb;
		private float lineWidth;

		public OutlineParams() {
			alpha = 1;
			lineWidth = 1 / 32f;
			fadeLineWidth = true;
			rgb = Color.WHITE;
			lightmap = LightTexture.FULL_BRIGHT;
		}

		// builder

		public OutlineParams colored(int color) {
			rgb = new Color(color, false);
			return this;
		}

		public OutlineParams colored(Color c) {
			rgb = c.copy();
			return this;
		}

		public OutlineParams lightmap(int light) {
			lightmap = light;
			return this;
		}

		public OutlineParams lineWidth(float width) {
			this.lineWidth = width;
			return this;
		}

		public OutlineParams highlightFace(@Nullable Direction face) {
			highlightedFace = face;
			return this;
		}

		public OutlineParams disableLineNormals() {
			disableLineNormals = true;
			return this;
		}

		public OutlineParams disableCull() {
			disableCull = true;
			return this;
		}

		// getter

		public float getLineWidth() {
			return fadeLineWidth ? alpha * lineWidth : lineWidth;
		}

		public Direction getHighlightedFace() {
			return highlightedFace;
		}

		public void loadColor(Vector4f vec) {
			vec.set(rgb.getRedAsFloat(), rgb.getGreenAsFloat(), rgb.getBlueAsFloat(), rgb.getAlphaAsFloat() * alpha);
		}
	}

}
