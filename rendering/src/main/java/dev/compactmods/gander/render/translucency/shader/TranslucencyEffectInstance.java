package dev.compactmods.gander.render.translucency.shader;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.charset.StandardCharsets;

// This is needed because Mojank:tm:
public final class TranslucencyEffectInstance implements AutoCloseable
{
	private final Uniform projectionMatrix;
	private final Uniform outSize;
	private final Sampler inputLayersColorSampler;
	private final Sampler inputLayersDepthSampler;

	private int vertexShader;
	private int fragmentShader;
	private int shaderProgram;

	private int colorTexture;
	private int depthTexture;

	public TranslucencyEffectInstance(final int layerCount)
	{
		this.projectionMatrix = new Uniform("ProjMat", Uniform.UT_MAT4, 16, null);
		this.outSize = new Uniform("OutSize", Uniform.UT_FLOAT2, 2, null);

		this.vertexShader = generateShader(layerCount, GlConst.GL_VERTEX_SHADER, VertexShaderGenerator::generate);
		this.fragmentShader = generateShader(layerCount, GlConst.GL_FRAGMENT_SHADER, FragmentShaderGenerator::generate);
		this.shaderProgram = generateShaderProgram(this.vertexShader, this.fragmentShader);

		this.projectionMatrix.setLocation(Uniform.glGetUniformLocation(shaderProgram, projectionMatrix.getName()));
		this.outSize.setLocation(Uniform.glGetUniformLocation(shaderProgram, outSize.getName()));
		this.inputLayersColorSampler = Sampler.get(shaderProgram, "InputLayersColorSampler", GlConst.GL_TEXTURE0);
		this.inputLayersDepthSampler = Sampler.get(shaderProgram, "InputLayersDepthSampler", GlConst.GL_TEXTURE1);
	}

	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();

		projectionMatrix.close();
		outSize.close();

		if (vertexShader >= 0)
		{
			GlStateManager.glDeleteShader(vertexShader);
			this.vertexShader = -1;
		}
		if (fragmentShader >= 0)
		{
			GlStateManager.glDeleteShader(fragmentShader);
			this.fragmentShader = -1;
		}
		if (shaderProgram >= 0)
		{
			GlStateManager.glDeleteProgram(shaderProgram);
			this.shaderProgram = -1;
		}
	}

	public void apply()
	{
		RenderSystem.assertOnRenderThread();

		GlStateManager._enableBlend();
		GlStateManager._blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager._blendEquation(GlConst.GL_FUNC_ADD);
		GlStateManager._glUseProgram(this.shaderProgram);

		inputLayersColorSampler.bind(colorTexture, GL32.GL_TEXTURE_2D_ARRAY);
		inputLayersDepthSampler.bind(depthTexture, GL32.GL_TEXTURE_2D_ARRAY);
		projectionMatrix.upload();
		outSize.upload();
	}

	public void clear()
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._glUseProgram(0);

		inputLayersColorSampler.unbind();
		inputLayersDepthSampler.unbind();
	}

	public void setColorSampler(final int textureId)
	{
		colorTexture = textureId;
	}

	public void setDepthSampler(final int textureId)
	{
		depthTexture = textureId;
	}

	public void setProjectionMatrix(final Matrix4f matrix)
	{
		projectionMatrix.set(matrix);
	}

	public void setOutputSize(final float width, final float height)
	{
		outSize.set(width, height);
	}

	private static int generateShader(final int layerCount, final int kind, Int2ObjectFunction<StringBuilder> shaderSource)
	{
		final var shaderId = GlStateManager.glCreateShader(kind);
		final var builder = shaderSource.get(layerCount);

		final var bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
		final var buffer = MemoryUtil.memAlloc(bytes.length + 1);
		buffer.put(bytes);
		buffer.put((byte)0);
		buffer.flip();

		try (var stack = MemoryStack.stackPush())
		{
			var addr = stack.pointers(buffer);
			GL20.nglShaderSource(shaderId, 1, addr.address0(), 0L);
		}
		finally
		{
			MemoryUtil.memFree(buffer);
		}
		GlStateManager.glCompileShader(shaderId);

		final var compileStatus = GlStateManager.glGetShaderi(shaderId, GlConst.GL_COMPILE_STATUS);
		if (compileStatus != GlConst.GL_TRUE)
		{
			// TODO: log info here and use a better result
			throw new RuntimeException("Failed to compile shader");
		}

		return shaderId;
	}

	private static int generateShaderProgram(final int vertexShader, final int fragmentShader)
	{
		RenderSystem.assertOnRenderThread();
		final var programId = GlStateManager.glCreateProgram();
		GlStateManager.glAttachShader(programId, vertexShader);
		GlStateManager.glAttachShader(programId, fragmentShader);
		GlStateManager.glLinkProgram(programId);

		final var linkStatus = GlStateManager.glGetProgrami(programId, GlConst.GL_LINK_STATUS);
		if (linkStatus != GlConst.GL_TRUE)
		{
			// TODO: log info here and use a better result
			throw new RuntimeException("Failed to link programs");
		}

		return programId;
	}
}
