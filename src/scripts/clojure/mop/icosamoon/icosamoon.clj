(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
; clj src\scripts\clojure\mop\icosamoon\icosamoon.clj
;;----------------------------------------------------------------
(ns mop.icosamoon.icosamoon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Start with spherical quad mesh, subdivide, and transform to R^3.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-07"}

  (:require
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [java.awt.image WritableRaster]
   [mop.geom.mesh TriangleMesh]
   [org.apache.commons.geometry.euclidean.threed Vector3D]))

;;-------------------------------------------------------------

(println "LWJGL: " (org.lwjgl.Version/getVersion))

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

;; S2 initial embedding
(def ^TriangleMesh s2-mesh
  ((comp ;;cmplx/subdivide-4
         ;;cmplx/subdivide-4
         ;;cmplx/subdivide-4
         ;;cmplx/subdivide-4
         ;;cmplx/subdivide-4
         )
   (mesh/spherical-icosahedron)
   )
  )

;;----------------------------------------------------
;; TODO: smarter shader construction in order to not depend on
;; shared names btwn clojure and glsl code,
;; and to reuse common functions

(glfw/arcball-loop
 {:title "icosamoon"
  :vertex-shader   "src/scripts/clojure/mop/icosamoon/icosamoon-vertex.glsl"
  :fragment-shader "src/scripts/clojure/mop/icosamoon/icosamoon-fragment.glsl"
  :s2-mesh s2-mesh
  :xyz-embedding  (s2/r3-embedding Vector3D/ZERO radius)
  ;; unit vectors pointing out
  :dual-embedding  (s2/r3-embedding Vector3D/ZERO 1.0)
  :rgba-embedding  s2/rgba
  :txt-embedding (s2/equirectangular-embedding 1.0 1.0)
  :radius          radius
  :color-image     color-image
  :elevation-image elevation-image
  })