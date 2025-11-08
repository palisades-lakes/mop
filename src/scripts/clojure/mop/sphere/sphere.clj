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
   :version "2025-11-05"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.arcball :as arcball]
   [mop.geom.mesh :as mesh]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw]
   [mop.lwjgl.util :as lwjgl])

  (:import
   [java.awt.image WritableRaster]
   [mop.geom.mesh QuadMesh]
   [org.apache.commons.geometry.euclidean.threed Vector3D]
   [org.lwjgl.glfw GLFW]))

;;-------------------------------------------------------------
;; UI state

(def mouse-button (atom false))
(def arcball (atom (arcball/ball -1 -1)))
(def window (glfw/start-window "sphere" mouse-button arcball))

;;-------------------------------------------------------------

(def ^WritableRaster color-image
  (image/get-writeable-raster
   "images/lroc_color_poles_2k.tif"
   "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif"))

;; elevation image relative to ?

(def ^WritableRaster elevation-image
  (image/get-writeable-raster
   "images/ldem_4.tif"
   "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif"))

;;----------------------------------------------------------------------
;; base geometry
;;----------------------------------------------------------------------
;; km
(def radius 1737.4)

(def ^QuadMesh mesh-r3
  ;; transform to R3
  (rn/transform
   (s2/r3-embedding Vector3D/ZERO radius)
   ;; S2 initial embedding
   (cmplx/subdivide-4
    (cmplx/subdivide-4
     (cmplx/subdivide-4
      (cmplx/subdivide-4
       (cmplx/subdivide-4
        (mesh/standard-quad-sphere))))))))

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(def sphere
  (lwjgl/setup {:vertex-shader "src/scripts/clojure/mop/sphere/sphere-vertex.glsl"
                :fragment-shader "src/scripts/clojure/mop/sphere/sphere-fragment.glsl"
                :mesh-r3 mesh-r3
                :radius radius
                :color-image color-image
                :elevation-image elevation-image
                }))

;;----------------------------------------------------

(lwjgl/uniform (:program sphere) "quaternion" (:q-origin @arcball))

;; TODO: call on window resize
(lwjgl/uniform (:program sphere)  "aspect" (rn/aspect (glfw/window-wh window)))

(while (not (GLFW/glfwWindowShouldClose window))
  (glfw/draw-faces window (:nindices sphere))
  (GLFW/glfwPollEvents)
  (when @mouse-button
    (lwjgl/uniform (:program sphere) "quaternion"
                   (arcball/current-q @arcball (glfw/cursor-xy window)))))

(glfw/clean-up window sphere)
