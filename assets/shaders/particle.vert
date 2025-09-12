#version 430

struct Particle {
    vec4 pos;   // xyz: position; 
    vec4 vel;   // velocity  
    vec4 col;   // color (rgba)
    vec4 group; // group info
};

layout(std430, binding = 0) buffer Particles {
    Particle particles[];
};

out vec4 v_color;

uniform float u_PointSize;
const float DEFAULT_POINT_SIZE = 5.0;

void main() {
    uint id = uint(gl_InstanceID);
    Particle p = particles[id];

    gl_Position = vec4(p.pos.xy, 0.0, 1.0);
    gl_PointSize = (u_PointSize > 0.0) ? u_PointSize : DEFAULT_POINT_SIZE;
    v_color = p.col;
}