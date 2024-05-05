package dev.compactmods.gander.render.translucency.shader;

final class VertexShaderGenerator
{
	// This class is static, yo.
	private VertexShaderGenerator() { }

	public static StringBuilder generate(int layerCount)
	{
		return new StringBuilder("""
		#version 150

		in vec4 Position;

		uniform mat4 ProjMat;
		uniform vec2 OutSize;

		out vec2 texCoord;

		void main() {
		    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
		    gl_Position = vec4(outPos.xy, 0.2, 1.0);
		    texCoord = Position.xy / OutSize;
		}
		""");
	}
}
