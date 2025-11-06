// :author  "palisades dot lakes at gmail dot com"
// :version "2025-11-03"

#version 130

uniform vec4 quaternion;
uniform float fov;
uniform float distance;
uniform float aspect;

in vec3 point;
out vec3 vpoint;

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

void main () {

  vec3 p = qRotate(quaternion,point) + vec3(0, 0, -distance);

  // Project vertex creating normalized device coordinates
  float f = 1.0 / tan(fov / 2.0); // TODO: constant, move out of shader
  float proj_x = (p.x / -p.z) * f;
  float proj_y = (p.y / -p.z) * f * aspect;
  float proj_z = p.z / (2.0 * distance);

  // Output to shader pipeline.
  gl_Position = vec4(proj_x, proj_y, proj_z, 1);
  vpoint = point;
}