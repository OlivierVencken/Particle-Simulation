#version 430

layout(location = 0) in vec3 in_pos; // unused, but keeps linker happy

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
uniform float u_AspectScale; // height / width
const float DEFAULT_POINT_SIZE = 5.0;

void main() {
    uint id = uint(gl_InstanceID);
    Particle p = particles[id];

    gl_Position = vec4(p.pos.xy, 0.0, 1.0);
    gl_Position.x *= u_AspectScale; // shrink horizontally to make a square region
    v_color = p.col;

    gl_PointSize = (u_PointSize > 0.0) ? u_PointSize : DEFAULT_POINT_SIZE;
}