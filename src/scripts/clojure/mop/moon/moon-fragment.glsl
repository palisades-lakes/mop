// :author  "palisades dot lakes at gmail dot com"
// :version "2025-10-25"

#version 130

#define PI 3.1415926535897932384626433832795

uniform vec4 quaternion;
uniform vec3 light;
uniform float ambient;
uniform float diffuse;
uniform float resolution;
uniform sampler2D colorTexture;
uniform sampler2D elevationTexture;
in vec3 vpoint;
out vec4 fragColor;

// WARNING: only for unit transforms!!!
vec3 qInverseRotate( vec4 quat, vec3 v ){
  // TODO: why are xy signs reversed from java/clojure side?
  // TODO: would it be faster if vectorized?
  float qw = quat.w;
  float qx = -quat.x;
  float qy = -quat.y;
  float qz = -quat.z;

  float x = v.x;
  float y = v.y;
  float z = v.z;

  // calculate the Hamilton product of the quaternion and vector
  float iw = -(qx * x) - (qy * y) - (qz * z);
  float ix = (qw * x) + (qy * z) - (qz * y);
  float iy = (qw * y) + (qz * x) - (qx * z);
  float iz = (qw * z) + (qx * y) - (qy * x);

  // calculate the Hamilton product of the intermediate vector and
  // the inverse quaternion

  return vec3(
              (iw * -qx) + (ix * qw) + (iy * -qz) - (iz * -qy),
              (iw * -qy) - (ix * -qz) + (iy * qw) + (iz * -qx),
              (iw * -qz) + (ix * -qy) - (iy * -qx) + (iz * qw)
          );
}

vec3 orthogonal_vector(vec3 n) {
  vec3 b;
  if (abs(n.x) <= abs(n.y)) {
    if (abs(n.x) <= abs(n.z))
    b = vec3(1, 0, 0);
    else
    b = vec3(0, 0, 1);
  } else {
    if (abs(n.y) <= abs(n.z))
    b = vec3(0, 1, 0);
    else
    b = vec3(0, 0, 1);
  };
  return normalize(cross(n, b));
}

mat3 oriented_matrix(vec3 n) {
  vec3 o1 = orthogonal_vector(n);
  vec3 o2 = cross(n, o1);
  return mat3(n, o1, o2);
}

vec2 uv(vec3 p) {
  float u = atan(p.x, -p.z) / (2.0 * PI) + 0.5;
  float v = 0.5 - atan(p.y, length(p.xz)) / PI;
  return vec2(u, v);
}

vec3 color(vec2 uv) { return texture(colorTexture, uv).rgb; }

float elevation(vec3 p) { return texture(elevationTexture, uv(p)).r; }

vec3 normal(mat3 horizon, vec3 p)
{
  vec3 pl = p + horizon * vec3(0, -1,  0) * resolution;
  vec3 pr = p + horizon * vec3(0,  1,  0) * resolution;
  vec3 pu = p + horizon * vec3(0,  0, -1) * resolution;
  vec3 pd = p + horizon * vec3(0,  0,  1) * resolution;
  vec3 u = horizon * vec3(elevation(pr) - elevation(pl), 2 * resolution, 0);
  vec3 v = horizon * vec3(elevation(pd) - elevation(pu), 0, 2 * resolution);
  return normalize(cross(u, v));
}

void main() {
  mat3 horizon = oriented_matrix(normalize(vpoint));
  float phong = ambient +
  diffuse * max(0.0,dot(qInverseRotate(quaternion,light),normal(horizon, vpoint)));
  fragColor = vec4(color(uv(vpoint)) * phong, 1);
}