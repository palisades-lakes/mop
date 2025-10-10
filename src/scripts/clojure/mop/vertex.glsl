#version 130

#define M_PI 3.1415926535897932384626433832795

uniform float fov;
uniform float distance;
uniform vec2 iResolution;
uniform vec2 iMouse;

in vec3 point;
out vec3 vpoint;
out mat3 rot_y;
out mat3 rot_x;

void main()
{
  // Rotate and translate vertex
  float alpha = iMouse.x / iResolution.x * M_PI * 2.0 + M_PI;
  float beta = (0.5 - iMouse.y / iResolution.y) * M_PI * 2.0;
  rot_y = mat3(
    vec3(cos(alpha), 0, sin(alpha)),
    vec3(0, 1, 0),
    vec3(-sin(alpha), 0, cos(alpha)));
  rot_x = mat3(
    vec3(1, 0, 0),
    vec3(0, cos(beta), -sin(beta)),
    vec3(0, sin(beta), cos(beta)));
  vec3 p = rot_x * rot_y * point + vec3(0, 0, distance);

  // Project vertex creating normalized device coordinates
  float f = 1.0 / tan(fov / 2.0);
  float aspect = iResolution.x / iResolution.y;
  float proj_x = p.x / p.z * f;
  float proj_y = p.y / p.z * f * aspect;
  float proj_z = p.z / (2.0 * distance);

  // Output to shader pipeline.
  gl_Position = vec4(proj_x, proj_y, proj_z, 1);
  vpoint = point;
}