package dev.compactmods.gander.render.translucency.shader;

final class FragmentShaderGenerator
{
	// This class is static, yo.
	private FragmentShaderGenerator() { }

	public static StringBuilder generate(int layerCount)
	{
		var builder = new StringBuilder();
		builder.append("#version 150\n");

		appendVariables(builder, layerCount);
		appendUtilityMethods(builder);
		appendMain(builder, layerCount);

		return builder;
	}

	private static void appendVariables(StringBuilder builder, int layerCount)
	{
		builder.append("""
			uniform sampler2DArray InputLayersColorSampler;
			uniform sampler2DArray InputLayersDepthSampler;
			in vec2 texCoord;
			""");
		builder.append("vec4 ordered_colors[").append(layerCount).append("];\n");
		builder.append("float ordered_depths[").append(layerCount).append("];\n");
		builder.append("""
			int ordered_size;
			out vec4 fragColor;
			""");
	}

	private static void appendUtilityMethods(StringBuilder builder)
	{
		// Shifts elements to the right by 1
		builder.append("""
				void shift_elements_right(int index) {
				    for (int i = ordered_size; i > index; i--) {
				        ordered_colors[i] = ordered_colors[i - 1];
				        ordered_depths[i] = ordered_depths[i - 1];
				    }
				    ordered_size++;
				}
				""");

		// Inserts into the list in its ordered position.
		builder.append("""
				void insert_ordered(vec4 color, float depth) {
				    if (color.a == 0) {
				        return;
				    }
				    if (ordered_size == 0) {
				        ordered_colors[0] = color;
				        ordered_depths[0] = depth;
				        ordered_size++;
				        return;
				    }
				    for (int i = 0; i < ordered_size; i++) {
				        if (depth < ordered_depths[i]) {
				            shift_elements_right(i);
				            ordered_colors[i] = color;
				            ordered_depths[i] = depth;
				            return;
				        }
				    }
				    ordered_colors[ordered_size] = color;
				    ordered_depths[ordered_size] = depth;
				    ordered_size++;
				}
				""");
		// Blends two colors together, assuming premultiplied alpha
		builder.append("""
				vec4 blend(vec4 dst, vec4 src) {
				    vec3 color = src.rgb + (dst.rgb * (1.0 - src.a));
				    float blendedAlpha = max(dst.a, (src.a * 1) + (dst.a * 0));
				    return vec4(color.rgb, blendedAlpha);
				}
				""");
	}

	private static void appendMain(StringBuilder builder, int layerCount)
	{
		builder.append("""
				void main() {
				    ordered_size = 0;
				    for (int i = 0; i < %d; i++) {
				        vec4 color = texture(InputLayersColorSampler, vec3(texCoord.xy, i));
				        float depth = texture(InputLayersDepthSampler, vec3(texCoord.xy, i)).r;
				        insert_ordered(color, depth);
				    }
				    if (ordered_size == 0) {
				        discard;
				    }
				    vec4 blendedColor = ordered_colors[ordered_size - 1];
				    for (int i = ordered_size - 2; i >= 0; i--) {
				        blendedColor = blend(blendedColor, ordered_colors[i]);
				    }
				    gl_FragDepth = ordered_depths[0];
				    fragColor = blendedColor;
				}
				""".formatted(layerCount));
	}
}
