(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon.clj
;;----------------------------------------------------------------
(ns mop.moon


  {:doc "Mesh Viewer demo using lwjgl and glfw.
  See https://svs.gsfc.nasa.gov/4720/ for texture and elevation images."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-21"}

  (:require
   [fastmath.vector :refer [add mult normalize sub vec3]]
   [mop.geom.util :as geom]
   [mop.lwjgl.glfw.util :as glfw]
   [mop.lwjgl.util :as lwjgl])
  (:import
   [java.lang Math]
   [org.apache.commons.geometry.euclidean.threed
    Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.threed.rotation
    QuaternionRotation]
   [org.lwjgl.glfw GLFW]
   [org.lwjgl.opengl GL46]))

;;-------------------------------------------------------------
;; UI state

(def mouse-button (atom false))
(def sphere-pt-origin (atom (Vector3D$Unit/from 0.0 0.0 1.0)))
(def q-origin (atom (QuaternionRotation/identity)))

;;-------------------------------------------------------------

(def window
  (glfw/start-window "moon" mouse-button sphere-pt-origin q-origin))

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
    (mult
     (normalize
      (add c (add (mult u (/ i n)) (mult v (/ j n)))))
     radius)))

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

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def vertex-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/vertex.glsl")
                     GL46/GL_VERTEX_SHADER))

(def fragment-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/fragment.glsl")
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
 0.1)
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "diffuse")
 0.9)
(GL46/glUniform3f
 (GL46/glGetUniformLocation program "light")
 (light 0) (light 1) (light 2))
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
 program "quaternion" @q-origin)

;; TODO: call on window resize
(lwjgl/aspect-ratio program (glfw/window-wh window) "aspect")

(while (not (GLFW/glfwWindowShouldClose window))
  (glfw/draw-quads window (count indices-sphere-high))
  (GLFW/glfwPollEvents)
  (when @mouse-button
    (let [sphere-pt (geom/sphere-pt
                     (glfw/cursor-xy window)
                     (glfw/window-center window)
                     (glfw/window-radius window))
          dq (QuaternionRotation/createVectorRotation
               sphere-pt @sphere-pt-origin)
          q (.multiply dq @q-origin)]
      (lwjgl/push-quaternion-coordinates program "quaternion" q))))

(glfw/clean-up window program
               vao-sphere-high
               color-texture
               elevation-texture)
