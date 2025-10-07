(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon.clj
;;----------------------------------------------------------------
(ns mop.moon

  {:doc "Mesh Viewer demo using lwjgl and glfw."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:require [clojure.java.io :as io]
            [clojure.math :refer [PI to-radians]]
            [fastmath.vector :refer [add mult normalize sub vec3]]
            [mop.lwjgl.glfw.util :as glfw]
            [mop.lwjgl.util :as lwjgl])
  (:import [java.awt.image BufferedImage WritableRaster]
           [java.nio ByteBuffer]
           [javax.imageio ImageIO]
           [org.lwjgl.glfw GLFW]
           [org.lwjgl.opengl GL11 GL13 GL20 GL30]) )

(def ^Double radius (double 1737.4))

(def mouse-pos (atom [0.0 0.0]))
(def mouse-button (atom false))

(glfw/init)

(def window
  (glfw/start-fullscreen-window "cube" mouse-pos mouse-button))

(defn download [url target]
  (with-open [in (io/input-stream url)
              out (io/output-stream target)]
    (print "Downloading" target "... ")
    (flush)
    (io/copy in out)
    (println "done")))

(def moon-tif "lroc_color_poles_2k.tif")
(when (not (.exists (io/file moon-tif)))
  (download
    "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif"
    moon-tif))

(def ^BufferedImage color (ImageIO/read (io/file moon-tif)))
(def ^WritableRaster color-raster (.getRaster color))
(def color-width (.getWidth color-raster))
(def color-height (.getHeight color-raster))
(def color-channels (.getNumBands color-raster))
(def color-pixels (int-array (* color-width color-height color-channels)))
(.getPixels color-raster
            0 0 ^long color-width ^long color-height ^ints color-pixels)

(def texture-color (GL11/glGenTextures))
(GL11/glBindTexture GL11/GL_TEXTURE_2D texture-color)
(^[int int int int int int int int ByteBuffer]
  GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA
                    color-width color-height 0
                    GL11/GL_RGB GL11/GL_UNSIGNED_BYTE
                    (lwjgl/make-byte-buffer
                     (byte-array (map unchecked-byte color-pixels))))
(GL11/glTexParameteri GL11/GL_TEXTURE_2D
                      GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D
                      GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D
                      GL11/GL_TEXTURE_WRAP_S GL11/GL_REPEAT)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D
                      GL11/GL_TEXTURE_WRAP_T GL11/GL_REPEAT)
(GL11/glBindTexture GL11/GL_TEXTURE_2D 0)

(def vertices-cube
  (float-array [-1.0 -1.0 -1.0
                 1.0 -1.0 -1.0
                -1.0  1.0 -1.0
                 1.0  1.0 -1.0
                -1.0 -1.0  1.0
                 1.0 -1.0  1.0
                -1.0  1.0  1.0
                 1.0  1.0  1.0]))

(def indices-cube
  (int-array [0 1 3 2
              6 7 5 4
              0 2 6 4
              5 7 3 1
              2 3 7 6
              4 5 1 0]))

(def vertex-shader-code "
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
  rot_y = mat3(vec3(cos(alpha), 0, sin(alpha)),
               vec3(0, 1, 0),
               vec3(-sin(alpha), 0, cos(alpha)));
  rot_x = mat3(vec3(1, 0, 0),
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
}")

(def points
  (map #(apply vec3 %)
       (partition 3 vertices-cube)))

(def corners
  (map (fn [[i _ _ _]] (nth points i))
       (partition 4 indices-cube)))

(def u-vectors
  (map (fn [[i j _ _]] (sub (nth points j) (nth points i)))
       (partition 4 indices-cube)))

(def v-vectors
  (map (fn [[i _ _ l]] (sub (nth points l) (nth points i)))
       (partition 4 indices-cube)))

(defn sphere-points [n c u v]
  (for [j (range (inc n)) i (range (inc n))]
       (mult (normalize (add c (add (mult u (/ i n)) (mult v (/ j n))))) radius)))

(defn sphere-indices [n face]
  (for [j (range n) i (range n)]
       (let [offset (+ (* face (inc n) (inc n)) (* j (inc n)) i)]
         [offset (inc offset) (+ offset n 2) (+ offset n 1)])))

(def n2 16)
(def vertices-sphere-high
  (float-array
   (flatten (map (partial sphere-points n2)
                 corners u-vectors v-vectors))))
(def indices-sphere-high
  (int-array (flatten (map (partial sphere-indices n2) (range 6)))))
(def vao-sphere-high
  (lwjgl/setup-vao vertices-sphere-high indices-sphere-high))

(def light (normalize (vec3 -1 0 -1)))

(def moon-ldem "ldem_4.tif")
(when (not (.exists (io/file moon-ldem)))
  (download "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif"
            moon-ldem))

(def ^BufferedImage ldem (ImageIO/read (io/file moon-ldem)))
(def ^WritableRaster ldem-raster (.getRaster ldem))
(def ldem-width (.getWidth ldem))
(def ldem-height (.getHeight ldem))
(def ldem-pixels (float-array (* ldem-width ldem-height)))
(do (.getPixels ldem-raster 0 0 ^long ldem-width ^long ldem-height ^floats ldem-pixels) nil)
(def resolution (/ (* 2.0 PI radius) ldem-width))

(def texture-ldem (GL11/glGenTextures))
(GL11/glBindTexture GL11/GL_TEXTURE_2D texture-ldem)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_S GL11/GL_REPEAT)
(GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_T GL11/GL_REPEAT)
(^[int int int int int int int int float/1] GL11/glTexImage2D
 GL11/GL_TEXTURE_2D 0 GL30/GL_R32F ldem-width ldem-height 0
 GL11/GL_RED GL11/GL_FLOAT ldem-pixels)

(def fragment-shader-code "
#version 130

#define PI 3.1415926535897932384626433832795

uniform vec3 light;
uniform float ambient;
uniform float diffuse;
uniform float resolution;
uniform sampler2D moon;
uniform sampler2D ldem;
in vec3 vpoint;
in mat3 rot_y;
in mat3 rot_x;
out vec4 fragColor;

vec3 orthogonal_vector(vec3 n)
{
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

mat3 oriented_matrix(vec3 n)
{
  vec3 o1 = orthogonal_vector(n);
  vec3 o2 = cross(n, o1);
  return mat3(n, o1, o2);
}

vec2 uv(vec3 p)
{
  float u = atan(p.x, -p.z) / (2.0 * PI) + 0.5;
  float v = 0.5 - atan(p.y, length(p.xz)) / PI;
  return vec2(u, v);
}

vec3 color(vec2 uv)
{
  return texture(moon, uv).rgb;
}

float elevation(vec3 p)
{
  return texture(ldem, uv(p)).r;
}

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

void main()
{
  mat3 horizon = oriented_matrix(normalize(vpoint));
  float phong = ambient + diffuse * max(0.0, dot(transpose(rot_y) * light, normal(horizon, vpoint)));
  fragColor = vec4(color(uv(vpoint)) * phong, 1);
}")

(def vertex-shader
  (lwjgl/make-shader vertex-shader-code GL30/GL_VERTEX_SHADER))
(def fragment-shader
  (lwjgl/make-shader fragment-shader-code GL30/GL_FRAGMENT_SHADER))
(def program
  (lwjgl/make-program vertex-shader fragment-shader))

(GL20/glVertexAttribPointer
 (^[int CharSequence] GL20/glGetAttribLocation program "point")
 3 GL11/GL_FLOAT false (* 3 Float/BYTES) (* 0 Float/BYTES))
(GL20/glEnableVertexAttribArray 0)

(GL20/glUseProgram program)
(let [[w0 h0] (glfw/window-size window)]
  (GL20/glUniform2f
   (^[int CharSequence] GL20/glGetUniformLocation
    program "iResolution") w0 h0))

(GL20/glUniform1f (^[int CharSequence] GL20/glGetUniformLocation program "fov") (to-radians 20.0))
(GL20/glUniform1f (^[int CharSequence] GL20/glGetUniformLocation program "distance") (* (.doubleValue radius) 12.0))
(GL20/glUniform1f (^[int CharSequence] GL20/glGetUniformLocation program "resolution") resolution)
(GL20/glUniform1f (^[int CharSequence] GL20/glGetUniformLocation program "ambient") 0.1)
(GL20/glUniform1f (^[int CharSequence] GL20/glGetUniformLocation program "diffuse") 0.9)
(GL20/glUniform3f (^[int CharSequence] GL20/glGetUniformLocation program "light")
                  (light 0) (light 1) (light 2))
(GL20/glUniform1i (^[int CharSequence] GL20/glGetUniformLocation program "moon") 0)
(GL20/glUniform1i (^[int CharSequence] GL20/glGetUniformLocation program "ldem") 1)
(GL13/glActiveTexture GL13/GL_TEXTURE0)
(GL11/glBindTexture GL11/GL_TEXTURE_2D texture-color)
(GL13/glActiveTexture GL13/GL_TEXTURE1)
(GL11/glBindTexture GL11/GL_TEXTURE_2D texture-ldem)

(while (not (GLFW/glfwWindowShouldClose window))
       (when @mouse-button
         (GL20/glUniform2f
          (^[int CharSequence] GL20/glGetUniformLocation program "iMouse")
          (@mouse-pos 0) (@mouse-pos 1)))
       (GL11/glEnable GL11/GL_CULL_FACE)
       (GL11/glCullFace GL11/GL_BACK)
       (GL11/glClearColor 0.0 0.0 0.0 1.0)
       (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
       (GL11/glDrawElements
        GL11/GL_QUADS (count indices-sphere-high) GL11/GL_UNSIGNED_INT 0)
       (GLFW/glfwSwapBuffers window)
       (GLFW/glfwPollEvents))

(GL20/glDeleteProgram program)
(lwjgl/teardown-vao vao-sphere-high)
(^[int] GL11/glDeleteTextures texture-color)
(^[int] GL11/glDeleteTextures texture-ldem)
(GLFW/glfwDestroyWindow window)
(GLFW/glfwTerminate)
