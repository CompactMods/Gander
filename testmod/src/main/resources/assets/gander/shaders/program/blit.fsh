#version 150

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D DiffuseSampler;

uniform vec4 ColorModulate;

in vec2 texCoord;

out vec4 fragColor;

void main(){
    gl_FragDepth = texture(DiffuseDepthSampler, texCoord).r;
    fragColor = texture(DiffuseSampler, texCoord) * ColorModulate;
}
