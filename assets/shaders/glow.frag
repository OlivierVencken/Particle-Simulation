#version 430

in vec4 v_color;
layout(location = 0) 
out vec4 outColor;

// increase size a lot
// enable GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
// instead of GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

void main() {
    vec2 uv = gl_PointCoord * 2.0 - 1.0;  // [-1, 1]
    float r = length(uv);
    float edge = fwidth(r);            
    float alpha = 1.0 - smoothstep(1.0 - edge, 1.0, r);

    vec2 pos = uv;
    float dist = 1.0/length(pos);
    dist *= 0.15;
    dist = pow(dist, 2.5);

    vec3 col = dist * v_color.rgb;

    col = 1.0 - exp( -col );

    if (r > 1.0) {
        discard;
    }

    outColor = vec4(col, 1.0);
}