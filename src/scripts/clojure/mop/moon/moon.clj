(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon\moon.clj
;;----------------------------------------------------------------
(ns mop.moon.moon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  See https://svs.gsfc.nasa.gov/4720/
  for texture and elevation images.
  Started with
  https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-26"}

  (:require
   [mop.geom.arcball :as arcball]
   [mop.geom.util :as geom]
   [mop.lwjgl.glfw.util :as glfw]
   [mop.lwjgl.util :as lwjgl])

  (:import
   [java.lang Math]
   [org.lwjgl.glfw GLFW]
   [org.lwjgl.opengl GL46]))

;;-------------------------------------------------------------
;; UI state

(def mouse-button (atom false))
(def arcball (atom (arcball/ball -1 -1)))

;;-------------------------------------------------------------

(def window
  (glfw/start-window "moon" mouse-button arcball))

;;-------------------------------------------------------------
;; color texture
;;-------------------------------------------------------------

(def ^Integer color-texture
  (lwjgl/int-texture-from-image-file
   "images/lroc_color_poles_2k.tif"
   "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif"))

;;-------------------------------------------------------------------
;; elevation image relative to ?
;;-------------------------------------------------------------------

(def ^Double radius (double 1737.4))

(let [[texture ^double r]
      (lwjgl/float-texture-from-image-file
       "images/ldem_4.tif"
       "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif")]
  (def ^Integer elevation-texture texture)
  (def resolution (/ (* lwjgl/TwoPI radius) r)))

;;----------------------------------------------------------------------
;; base geometry
;;----------------------------------------------------------------------

(def vertices-cube
  (float-array [-1.0 -1.0 -1.0
                1.0 -1.0 -1.0
                1.0  1.0 -1.0
                -1.0  1.0 -1.0
                -1.0 -1.0  1.0
                1.0 -1.0  1.0
                1.0  1.0  1.0
                -1.0  1.0  1.0]
               ))

(def indices-cube
  (int-array [0 3 2 1
              4 5 6 7
              0 4 7 3
              5 1 2 6
              2 3 7 6
              0 1 5 4]))

(def points
  (map (fn [[x y z]]
         (geom/make-vector (double x) (double y) (double z)))
       (partition 3 vertices-cube)))

(def corners
  (map (fn [[i _ _ _]] (nth points i))
       (partition 4 indices-cube)))

(def u-vectors
  (map (fn [[i j _ _]]
         (geom/subtract (nth points j) (nth points i)))
       (partition 4 indices-cube)))

(def v-vectors
  (map (fn [[i _ _ l]]
         (geom/subtract (nth points l) (nth points i)))
       (partition 4 indices-cube)))

(defn sphere-points [n c u v]
  (for [j (range (inc n)) i (range (inc n))]
    (geom/multiply
     (geom/normalize
      (geom/add
       c
       (geom/add
        (geom/multiply u (double (/ i n)))
        (geom/multiply v (double (/ j n))))))
     radius)))

(defn sphere-indices [n face]
  (for [j (range n) i (range n)]
    (let [offset (+ (* face (inc n) (inc n)) (* j (inc n)) i)]
      [offset (inc offset) (+ offset n 2) (+ offset n 1)])))

(def n2 16)
(def vertices-sphere-high
  (float-array
   (flatten
    (map geom/coordinates
         (flatten
          (map (partial sphere-points n2)
               corners u-vectors v-vectors))))))

(def indices-sphere-high
  (int-array (flatten (map (partial sphere-indices n2) (range 6)))))

(def vao-sphere-high
  (lwjgl/setup-vao vertices-sphere-high indices-sphere-high))

(def light (geom/unit-vector 1.0 1.0 1.0))

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def vertex-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/moon/moon-vertex.glsl")
                     GL46/GL_VERTEX_SHADER))

(def fragment-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/moon/moon-fragment.glsl")
                     GL46/GL_FRAGMENT_SHADER))

(def ^Integer program
  (lwjgl/make-program vertex-shader fragment-shader))

;;----------------------------------------------------
;; only way I've found to get cursive to stop complaining
;; about no matching call
(let [index (int (GL46/glGetAttribLocation ^int program "point"))
      size (int 3)
      type (int GL46/GL_FLOAT)
      normalized (boolean false)
      stride (int (* 3 Float/BYTES))
      pointer (long (* 0 Float/BYTES))]
  (GL46/glVertexAttribPointer index size type normalized stride pointer))

(GL46/glEnableVertexAttribArray 0)

(GL46/glUseProgram program)

(GL46/glUniform1f
 (GL46/glGetUniformLocation program "fov")
 (Math/toRadians 20.0))
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "distance")
 (* (.doubleValue radius) 12.0))
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "resolution")
 resolution)
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "ambient")
 0.3)
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "diffuse")
 0.5)
(GL46/glUniform3fv
 (GL46/glGetUniformLocation program "light")
 (geom/float-coordinates light))
(GL46/glUniform1i
 (GL46/glGetUniformLocation program "colorTexture")
 0)
(GL46/glUniform1i
 (GL46/glGetUniformLocation program "elevationTexture") 1)
(GL46/glActiveTexture GL46/GL_TEXTURE0)
(GL46/glBindTexture GL46/GL_TEXTURE_2D color-texture)
(GL46/glActiveTexture GL46/GL_TEXTURE1)
(GL46/glBindTexture GL46/GL_TEXTURE_2D elevation-texture)

(GL46/glEnable GL46/GL_CULL_FACE)
(GL46/glClearColor 0.0 0.0 0.0 1.0)

(lwjgl/push-quaternion-coordinates
 program "quaternion" (:q-origin @arcball))

;; TODO: call on window resize
(lwjgl/aspect-ratio program (glfw/window-wh window) "aspect")

(while (not (GLFW/glfwWindowShouldClose window))
  (glfw/draw-quads window (count indices-sphere-high))
  (GLFW/glfwPollEvents)
  (when @mouse-button
    (lwjgl/push-quaternion-coordinates
     program "quaternion"
     (arcball/current-q @arcball (glfw/cursor-xy window)))))

(glfw/clean-up window
               program
               vao-sphere-high
               color-texture
               elevation-texture)
