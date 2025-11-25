(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\moon\tetramoon.clj
;;----------------------------------------------------------------
(ns mop.moon.tetramoon
  {:doc "Mesh Viewer demo using lwjgl and glfw.
  Start with spherical quad mesh, subdivide, and transform to R^3.
  Started with https://clojurecivitas.github.io/opengl_visualization/main.html"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.geom.tetrahedron :as tetrahedron]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [mop.cmplx.complex VertexPair]
   [mop.geom.mesh Mesh]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;-------------------------------------------------------------

(println "LWJGL: " (org.lwjgl.Version/getVersion))

(let [radius 1737.4
      ^Mesh mesh
      #_(tetrahedron/s2-up-forwards)
      (tetrahedron/s2-up-backwards)
      #_((comp
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        cmplx/midpoint-subdivide-4
        )
       (tetrahedron/s2-up-forwards))
      s2 (.embedding mesh)
      u2 (update-vals s2 s2/s2-to-u2)
      txt (update-vals u2 s2/u2-to-txt)
      xyz (update-vals s2 (fn [^Point2S p] (rn/multiply (s2/s2-to-r3 p) radius)))
      rgba (update-vals s2 s2/s2-to-rgba)
      ;; unit vectors pointing out
      dual (update-vals s2 s2/s2-to-r3)
      pairs (sort (cmplx/vertex-pairs (.cmplx mesh)))
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
   {:title           "tetramoon"
    :cmplx         (.cmplx mesh)
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