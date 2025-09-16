#version 430

layout(location = 0) in vec2 a_Position;
layout(location = 1) in vec2 a_TexCoord;

uniform vec4 u_UVRect; // x,y,w,h in texture space [0..1]

out vec2 v_TexCoord;

void main(){
  gl_Position = vec4(a_Position, 0.0, 1.0);
  v_TexCoord = u_UVRect.xy + a_TexCoord * u_UVRect.zw;
}

