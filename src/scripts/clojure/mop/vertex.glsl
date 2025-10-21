// :author  "palisades dot lakes at gmail dot com"
// :version "2025-10-21"

#version 130

uniform vec4 quaternion;
uniform float fov;
uniform float distance;
uniform float aspect;

in vec3 point;
out vec3 vpoint;

vec3 qRotate( vec4 q, vec3 v ){
	return v + 2.0*cross(cross(v, q.xyz ) + q.w*v, q.xyz); }

void main() {

  vec3 p = qRotate(quaternion,point) + vec3(0, 0, distance);

  // Project vertex creating normalized device coordinates
  float f = 1.0 / tan(fov / 2.0);
  float proj_x = (p.x / p.z) * f;
  float proj_y = (p.y / p.z) * f * aspect;
  float proj_z = p.z / (2.0 * distance);

  // Output to shader pipeline.
  gl_Position = vec4(proj_x, proj_y, proj_z, 1);
  vpoint = point;
}