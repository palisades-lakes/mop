(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\icosamoon\icosamoon.clj
;;----------------------------------------------------------------
(ns mop.icosamoon.icosamoon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-19"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [mop.geom.mesh Mesh]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;-------------------------------------------------------------

(let [radius 1737.4
      ^Mesh icosahedron
      ((comp
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        )
       (icosahedron/u2-cut-icosahedron))
      u2 (.embedding icosahedron)
      s2 (update-vals u2 s2/u2-to-s2)
      txt (update-vals u2 s2/u2-to-txt)
      xyz (update-vals s2 (fn [^Point2S p] (rn/multiply (s2/s2-to-r3 p) radius)))
      rgba (update-vals s2 s2/s2-to-rgba)
      ;; unit vectors pointing out
      dual (update-vals s2 s2/s2-to-r3)
      ]


  (glfw/arcball-loop
   {:title           "icosamoon"
    :cmplx         (.cmplx icosahedron)
    :vertex-shader   "src/scripts/clojure/mop/icosamoon/icosamoon-vertex.glsl"
    :fragment-shader "src/scripts/clojure/mop/icosamoon/icosamoon-fragment.glsl"
    :txt-embedding  txt
    :s2-embedding   s2
    :xyz-embedding  xyz
    :dual-embedding  dual
    :rgba-embedding  rgba
    :radius          radius
    :color-image
    (image/get-image
     "images/lroc_color_poles_2k.tif"
     "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif")
    :elevation-image
    (image/get-image
     "images/ldem_4.tif"
     "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif")})
  )