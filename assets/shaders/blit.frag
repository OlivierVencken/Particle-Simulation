#version 430

in vec2 v_TexCoord;

uniform sampler2D u_Texture;
uniform int u_ForceWhite; // 0 = original color, 1 = white 

layout(location = 0) out vec4 outColor;

void main() {
    vec4 c = texture(u_Texture, v_TexCoord);
    if (u_ForceWhite != 0 && c.a > 0.001) {       
        outColor = vec4(1.0, 1.0, 1.0, c.a);
    } else {
        outColor = c;
    }
}