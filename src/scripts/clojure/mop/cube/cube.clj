(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\cube\cube.clj
;;----------------------------------------------------------------
(ns mop.cube.cube
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Colored cube to help debugging.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
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
  (glfw/start-window "cube" mouse-button arcball))

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
  (float-array
   (mapv #(* radius %)
         [-1.0 -1.0 -1.0
          1.0 -1.0 -1.0
          1.0  1.0 -1.0
          -1.0  1.0 -1.0
          -1.0 -1.0  1.0
          1.0 -1.0  1.0
          1.0  1.0  1.0
          -1.0  1.0  1.0]
         )))

(def indices-cube
  (int-array  [0 3 2 1
               4 5 6 7
               0 4 7 3
               5 1 2 6
               2 3 7 6
               0 1 5 4]))

(def vao-cube (lwjgl/setup-vao vertices-cube indices-cube))

;
(def light (geom/unit-vector -1 0 1))

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def vertex-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/cube/cube-vertex.glsl")
                     GL46/GL_VERTEX_SHADER))

(def fragment-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/cube/cube-fragment.glsl")
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
 0.2)
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "diffuse")
 0.8)
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
  (glfw/draw-quads window (count indices-cube))
  (GLFW/glfwPollEvents)
  (when @mouse-button
    (lwjgl/push-quaternion-coordinates
     program "quaternion"
     (arcball/current-q @arcball (glfw/cursor-xy window)))))

(glfw/clean-up window
               program
               vao-cube
               color-texture
               elevation-texture)
