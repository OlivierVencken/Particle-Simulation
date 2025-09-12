#version 430

in vec4 v_color;
layout(location = 0) out vec4 outColor;

void main() {
    vec2 uv = gl_PointCoord * 2.0 - 1.0;  // [-1, 1]
    float r = length(uv);
    float edge = fwidth(r);            
    float alpha = 1.0 - smoothstep(1.0 - edge, 1.0, r);

    if (r > 1.0) {
        discard;
    }

    outColor = vec4(v_color.rgb, v_color.a * alpha);
}