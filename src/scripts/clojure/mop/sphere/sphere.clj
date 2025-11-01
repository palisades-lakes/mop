(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\sphere\sphere.clj
;;----------------------------------------------------------------
(ns mop.sphere.sphere
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Start with spherical quad mesh, subdivide, and transform to R^3.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:require
   [clojure.pprint :as pp]
   [mop.geom.arcball :as arcball]
   [mop.geom.mesh :as mesh]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.lwjgl.glfw.util :as glfw]
   [mop.lwjgl.util :as lwjgl])
  (:import
   [java.lang Math]
   [mop.geom.mesh QuadMesh]
   [org.apache.commons.geometry.euclidean.threed Vector3D]
   [org.lwjgl.glfw GLFW]
   [org.lwjgl.opengl GL46]))

;;-------------------------------------------------------------
;; UI state

(def mouse-button (atom false))
(def arcball (atom (arcball/ball -1 -1)))

;;-------------------------------------------------------------

(def window
  (glfw/start-window "sphere" mouse-button arcball))

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
;; km
(def ^Double radius 1737.4)

(let [[texture ^double r]
      (lwjgl/float-texture-from-image-file
       "images/ldem_4.tif"
       "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif")]
  (def ^Integer elevation-texture texture)
  (def resolution (/ (* lwjgl/TwoPI radius) r)))

;;----------------------------------------------------------------------
;; base geometry
;;----------------------------------------------------------------------

(let [embedding (s2/embedding Vector3D/ZERO radius)
      sphere-mesh (mesh/standard-quad-sphere)
      ^QuadMesh mesh (rn/transform embedding sphere-mesh)
      [coordinates elements] (mesh/coordinates-and-elements mesh)
      vertices-sphere (float-array coordinates)
      indices-sphere (int-array elements)]
  (pp/pprint (.embedding mesh))
  (def vao-sphere (lwjgl/setup-vao vertices-sphere indices-sphere))
  )

(def light (rn/unit-vector -1 0 1))

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def ^Integer program
  (lwjgl/make-program
   {GL46/GL_VERTEX_SHADER
   "src/scripts/clojure/mop/sphere/sphere-vertex.glsl"
    GL46/GL_FRAGMENT_SHADER
    "src/scripts/clojure/mop/sphere/sphere-fragment.glsl"}))

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
 (rn/float-coordinates light))
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
  (glfw/draw-quads window (:nindices vao-sphere))
  (GLFW/glfwPollEvents)
  (when @mouse-button
    (lwjgl/push-quaternion-coordinates
     program "quaternion"
     (arcball/current-q @arcball (glfw/cursor-xy window)))))

(glfw/clean-up window
               program
               vao-sphere
               color-texture
               elevation-texture)
