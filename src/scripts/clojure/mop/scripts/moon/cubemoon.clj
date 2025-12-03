(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon\cubemoon.clj
;;----------------------------------------------------------------
(ns mop.scripts.moon.cubemoon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Colored cube to help debugging.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.quads :as quads]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [mop.cmplx.complex VertexPair]
   [mop.geom.mesh Mesh]))

;;----------------------------------------------------------------------
(let [radius 1737.4
      ^Mesh cube-r3 (rn/transform radius (quads/standard-quad-cube))
      xyz (.embedding cube-r3)
      s2 (update-vals xyz s2/r3-to-s2)
      txt (update-vals s2 s2/s2-to-txt)
      rgba (update-vals s2 s2/s2-to-rgba)
      ;; unit vectors pointing out
      dual (update-vals s2 s2/s2-to-r3)
      pairs (sort (cmplx/vertex-pairs (.cmplx cube-r3)))
      ]
  (doseq [^VertexPair e pairs]
    (let [a (s2 (.z0 e))
          b (s2 (.z1 e))
          i (s2/dateline-crossing a b)]
      (println (.toString e))
      (when i
        (println (mcs/simple-string a) "->" (mcs/simple-string b))
        (println (mcs/simple-string i)))))

  (glfw/arcball-loop
   {:title           "icosamoon"
    :cmplx         (.cmplx cube-r3)
    :vertex-shader   "src/main/glsl/planet/vertex.glsl"
    :fragment-shader "src/main/glsl/planet/fragment.glsl"
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

;;----------------------------------------------------
