// :author  "palisades dot lakes at gmail dot com"
// :version "2025-11-07"

#version 330
#define PI 3.1415926535897932384626433832795
uniform vec4 quaternion;
uniform float fov;
uniform float distance;
uniform float aspect;

layout (location = 0) in vec3 xyzIn;
layout (location = 1) in vec4 rgbaIn;
layout (location = 2) in vec3 dualIn;
layout (location = 3) in vec2 txtIn;

out vec3 xyzOut;
out vec4 rgbaOut;
out vec3 dualOut;
out vec2 txtOut;

vec3 qRotate (vec4 quat, vec3 v) {
  // TODO: would it be faster if vectorized?
  float qw = quat.w;
  float qx = quat.x;
  float qy = quat.y;
  float qz = quat.z;

  float x = v.x;
  float y = v.y;
  float z = v.z;

  // calculate the Hamilton product of the quaternion and vector
  float iw = -(qx * x) - (qy * y) - (qz * z);
  float ix =  (qw * x) + (qy * z) - (qz * y);
  float iy =  (qw * y) + (qz * x) - (qx * z);
  float iz =  (qw * z) + (qx * y) - (qy * x);

  // calculate the Hamilton product of the intermediate vector and
  // the inverse quaternion

  return vec3((iw * -qx) + (ix *  qw) + (iy * -qz) - (iz * -qy),
              (iw * -qy) - (ix * -qz) + (iy *  qw) + (iz * -qx),
              (iw * -qz) + (ix * -qy) - (iy * -qx) + (iz *  qw));
}

vec2 uv (vec3 p) {
  float u = atan(p.x, -p.z) / (2.0 * PI) + 0.5;
  float v = 0.5 - atan(p.y, length(p.xz)) / PI;
  return vec2(u, v);
}

void main () {

  vec3 p = qRotate(quaternion,xyzIn) + vec3(0, 0, -distance);

  // Project vertex creating normalized device coordinates
  float f = 1.0 / tan(fov / 2.0); // TODO: constant, move out of shader
  float proj_x = (p.x / -p.z) * f;
  float proj_y = (p.y / -p.z) * f * aspect;
  float proj_z = p.z / (2.0 * distance);

  // Output to shader pipeline.
  gl_Position = vec4(proj_x, proj_y, -proj_z, 1);
  xyzOut = xyzIn;
  rgbaOut = rgbaIn;
  dualOut = dualIn;
  //txtOut = uv(xyzOut);
  txtOut = txtIn;
}