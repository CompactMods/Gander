#version 150

uniform int InputLayerCount;
uniform sampler2DArray InputLayersColorSampler;
uniform sampler2DArray InputLayersDepthSampler;

in vec2 texCoord;
out vec4 fragColor;

vec3 blend( vec3 dst, vec4 src ) {
    return ( dst * ( 1.0 - src.a ) ) + src.rgb;
}

// Perform a variant of selection sort on all of the layers.
// Presumably, with a lot of layers, this will be pretty bad for performance.
// We'll have to do benchmarks.

void main() {
    vec3 accumulatedColor = vec3(0, 0, 0);
    float minimumDepth = 2f; // This value is larger than anything we'll ever sample

    // Run until we've accumulated all of the possible values
    for (int x = 0; x < InputLayerCount; x++) {
        float maximumThisRound = -1.0f; // This value is smaller than anything we'll ever sample
        int index = -1;

        // Find the maximum depth value SMALLER than minimumDepth
        for (int i = 0; i < InputLayerCount; i++) {
            float newDepth = texture(InputLayersDepthSampler, vec3(texCoord.xy, i)).r;
            if (newDepth > maximumThisRound && newDepth < minimumDepth) {
                maximumThisRound = newDepth;
                index = i;
            }
        }

        if (x == 0) {
            // We're the furthest layer, so use this as our ground truth.
            accumulatedColor = texture(InputLayersColorSampler, vec3(texCoord.xy, index)).rgb;
        } else {
            // Blend it with our current color.
            accumulatedColor = blend(accumulatedColor, texture(InputLayersColorSampler, vec3(texCoord.xy, index)));
        }

        // Update the minimum depth to the largest value we got this round, so we're always looking for smaller values.
        minimumDepth = maximumThisRound;
    }

    gl_FragDepth = minimumDepth;
    fragColor = vec4(accumulatedColor, 1.0);
}
