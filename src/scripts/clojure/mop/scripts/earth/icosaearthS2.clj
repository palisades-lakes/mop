(set! *warn-on-reflection* true)
;;----------------------------------------------------------------
;; mvn install & clj src\scripts\clojure\mop\scripts\earth\icosaearthS2.clj
;;----------------------------------------------------------------
(ns mop.scripts.earth.icosaearthS2
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2026-03-16"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [mop.java.geom.mesh Mesh]
   [org.apache.commons.geometry.spherical.twod Point2S]))
;;-------------------------------------------------------------
(set! *unchecked-math* :warn-on-boxed)
;;-------------------------------------------------------------

(let [radius 6371.0
      ^Mesh icosahedron
      ((comp
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        )
       (icosahedron/s2-cut-icosahedron))
      s2 (.embedding icosahedron)
      u2 (update-vals s2 s2/s2-to-u2)
      txt (update-vals u2 s2/u2-to-txt)
      xyz (update-vals s2 (fn [^Point2S p] (rn/multiply (s2/s2-to-r3 p) radius)))
      rgba (update-vals s2 s2/s2-to-rgba)
      ;; unit vectors pointing out
      dual (update-vals s2 s2/s2-to-r3)
      ]
  
  (glfw/arcball-loop
   {:title "earth"
    :cmplx (.cmplx icosahedron)
    :vertex-shader   "src/main/glsl/planet/vertex.glsl"
    :fragment-shader "src/main/glsl/planet/fragment.glsl"
    :txt-embedding txt
    :s2-embedding s2
    :xyz-embedding xyz
    :dual-embedding dual
    :rgba-embedding rgba
    :radius radius
    :color-image
    (image/get-image
     "images/earth/world.topo.bathy.200412.3x21600x10800-16384x8192.png")
    #_(image/get-image
     "images/earth/world.topo.bathy.200412.3x21600x10800.png"
     "https://eoimages.gsfc.nasa.gov/images/imagerecords/73000/73909/world.topo.bathy.200412.3x21600x10800.png")
    :elevation-image
    (image/get-image
       "images/earth/gebco_08_rev_elev_21600x10800-16384x8192.png")
       #_(image/get-image
          "images/earth/gebco_08_rev_elev_21600x10800.png"
     "https://eoimages.gsfc.nasa.gov/images/imagerecords/73000/73934/gebco_08_rev_elev_21600x10800.png")
    }))