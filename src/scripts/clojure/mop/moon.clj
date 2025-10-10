(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon.clj
;;----------------------------------------------------------------
(ns mop.moon


  {:doc "Mesh Viewer demo using lwjgl and glfw.
  See https://svs.gsfc.nasa.gov/4720/ for texture and elevation images."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-10"}

  (:require [clojure.math :refer [PI to-radians]]
            [fastmath.vector :refer [add mult normalize sub vec3]]
            [mop.lwjgl.glfw.util :as glfw]
            [mop.lwjgl.util :as lwjgl])
  (:import [org.lwjgl.glfw GLFW]
           [org.lwjgl.opengl GL11 GL13 GL20 GL30 GL46]) )

;;-------------------------------------------------------------

(def mouse-pos (atom [0.0 0.0]))
(def mouse-button (atom false))

(glfw/init)

(def window
  (glfw/start-fullscreen-window "moon" mouse-pos mouse-button))

;;-------------------------------------------------------------
;; color texture
;;-------------------------------------------------------------

(def color-texture
  (lwjgl/int-texture-from-image-file
   "images/lroc_color_poles_2k.tif"
   "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif"))

;;-------------------------------------------------------------------
;; elevation image relative to ?
;;-------------------------------------------------------------------

(def ^Double radius (double 1737.4))

(let [[texture r]
      (lwjgl/float-texture-from-image-file
       "images/ldem_4.tif"
       "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif")]
  (def elevation-texture texture)
  (def resolution (/ (* 2.0 PI radius) r)))

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
                     GL30/GL_VERTEX_SHADER))

(def fragment-shader
  (lwjgl/make-shader (slurp "src/scripts/clojure/mop/fragment.glsl")
                     GL30/GL_FRAGMENT_SHADER))

(def program
  (lwjgl/make-program vertex-shader fragment-shader))

;;----------------------------------------------------

(GL46/glVertexAttribPointer
 (GL20/glGetAttribLocation ^int program "point")
 3 GL11/GL_FLOAT false (* 3 Float/BYTES) (* 0 Float/BYTES))

(GL20/glEnableVertexAttribArray 0)

(GL20/glUseProgram program)
(let [[w0 h0] (glfw/window-size window)]
  (GL20/glUniform2f
   (GL20/glGetUniformLocation ^int program "iResolution") w0 h0))

(GL20/glUniform1f
 (GL20/glGetUniformLocation ^int program "fov")
 (to-radians 20.0))
(GL20/glUniform1f
 (GL20/glGetUniformLocation ^int program "distance")
 (* (.doubleValue radius) 12.0))
(GL20/glUniform1f
 (GL20/glGetUniformLocation ^int program "resolution")
 resolution)
(GL20/glUniform1f
 (GL20/glGetUniformLocation ^int program "ambient")
 0.1)
(GL20/glUniform1f
 (GL20/glGetUniformLocation ^int program "diffuse")
 0.9)
(GL20/glUniform3f
 (GL20/glGetUniformLocation ^int program "light")
 (light 0) (light 1) (light 2))
(GL20/glUniform1i
 (GL20/glGetUniformLocation ^int program "colorTexture")
 0)
(GL20/glUniform1i
 (GL20/glGetUniformLocation ^int program "elevationTexture") 1)
(GL13/glActiveTexture GL13/GL_TEXTURE0)
(GL11/glBindTexture GL11/GL_TEXTURE_2D color-texture)
(GL13/glActiveTexture GL13/GL_TEXTURE1)
(GL11/glBindTexture GL11/GL_TEXTURE_2D elevation-texture)

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
(^[int] GL11/glDeleteTextures color-texture)
(^[int] GL11/glDeleteTextures elevation-texture)
(glfw/clean-up window)
