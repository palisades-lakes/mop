(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon\moon.clj
;;----------------------------------------------------------------
(ns mop.moon.moon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Start with spherical quad mesh, subdivide, and transform to R^3.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.arcball :as arcball]
   [mop.geom.mesh :as mesh]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.lwjgl.glfw.util :as glfw]
   [mop.lwjgl.util :as lwjgl])

  (:import
   [java.lang Math]
   [mop.cmplx.complex QuadComplex]
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
  (glfw/start-window "moon" mouse-button arcball))

;;-------------------------------------------------------------
;; color texture
;;-------------------------------------------------------------

(let [[texture _ _]
      (lwjgl/int-texture-from-image-file
       "images/lroc_color_poles_2k.tif"
       "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif")]
  (def color-texture texture)
  ;(def color-width w)
  ;(def color-height h)
  )

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

(let [embedding-r3 (s2/r3-embedding Vector3D/ZERO radius)
      ;; S2 initial embedding
      mesh-s2 (cmplx/subdivide-4
               (cmplx/subdivide-4
                (cmplx/subdivide-4
                 (cmplx/subdivide-4
                  (cmplx/subdivide-4
                   (mesh/standard-quad-sphere))))))
      ;; transform to R3
      ^QuadMesh mesh-r3 (rn/transform embedding-r3 mesh-s2)
      ;embedding-texture (s2/equirectangular-embedding color-width color-height)
      ;^QuadMesh mesh-texture (rn/transform embedding-texture mesh-s2)
      ]
  ;; check
  (def vao-sphere (lwjgl/setup-vao mesh-r3))
  (println "faces:" (count (.faces ^QuadComplex (.cmplx mesh-r3))))
  (println "vertices:" (count (.vertices ^QuadComplex (.cmplx mesh-r3))))
  )

(def light (rn/unit-vector 1.0 1.0 1.0))

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def ^Integer program
  (lwjgl/make-program
   {GL46/GL_VERTEX_SHADER
    "src/scripts/clojure/mop/moon/moon-vertex.glsl"
    GL46/GL_FRAGMENT_SHADER
    "src/scripts/clojure/mop/moon/moon-fragment.glsl"}))

;;----------------------------------------------------
;; only way I've found to get cursive to stop complaining
;; about no matching call
;;TODO: clean this up, more general!
(let [index (int (GL46/glGetAttribLocation ^int program "point"))
      size (int 3)
      stride (int (* 5 Float/BYTES))
      pointer (long (* index Float/BYTES))]
  (GL46/glVertexAttribPointer index size GL46/GL_FLOAT false stride pointer)
  (GL46/glEnableVertexAttribArray index))
(let [index (int (GL46/glGetAttribLocation ^int program "tex"))
      size (int 2)
      stride (int (* 5 Float/BYTES))
      pointer (long (* index Float/BYTES))]
  (GL46/glVertexAttribPointer index size GL46/GL_FLOAT false stride pointer)
  (GL46/glEnableVertexAttribArray index))

(GL46/glUseProgram program)

(GL46/glUniform1f
 (GL46/glGetUniformLocation program "fov")
 (Math/toRadians 20.0))

;; TODO: units?
(GL46/glUniform1f
 (GL46/glGetUniformLocation program "distance")
 (* (.doubleValue radius) 12.0))

;; used to calculate texture coordinates, shouldn't be passed to GLSL
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
 (rn/float-coordinates light))

(GL46/glUniform1i (GL46/glGetUniformLocation program "colorTexture") 0)
(GL46/glActiveTexture GL46/GL_TEXTURE0)
(GL46/glBindTexture GL46/GL_TEXTURE_2D color-texture)
(lwjgl/check-error)
(GL46/glTexParameteri GL46/GL_TEXTURE_2D GL46/GL_TEXTURE_MIN_FILTER GL46/GL_NEAREST)
(GL46/glTexParameteri GL46/GL_TEXTURE_2D
                      GL46/GL_TEXTURE_WRAP_S
                      GL46/GL_CLAMP_TO_BORDER)
(lwjgl/check-error)
(GL46/glTexParameteri GL46/GL_TEXTURE_2D
                      GL46/GL_TEXTURE_WRAP_T
                      GL46/GL_CLAMP_TO_BORDER)
(lwjgl/check-error)
(GL46/glTexParameterfv GL46/GL_TEXTURE_2D
                       GL46/GL_TEXTURE_BORDER_COLOR
                       (lwjgl/make-float-buffer (float-array [1.0 0.0 1.0 1.0])))
(lwjgl/check-error)

(GL46/glUniform1i
 (GL46/glGetUniformLocation program "elevationTexture") 1)
(GL46/glActiveTexture GL46/GL_TEXTURE1)
(GL46/glBindTexture GL46/GL_TEXTURE_2D elevation-texture)

(GL46/glEnable GL46/GL_CULL_FACE)
(GL46/glCullFace GL46/GL_BACK)
(GL46/glClearColor 0.2 0.2 0.2 1.0) ;; dark gray

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
