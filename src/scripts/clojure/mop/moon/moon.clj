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
   :version "2025-11-15"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.commons.debug :as debug]
   [mop.geom.quads :as quads]
   [mop.geom.rn :as rn]
   [mop.geom.s2 :as s2]
   [mop.image.util :as image]
   [mop.lwjgl.glfw.util :as glfw])

  (:import
   [mop.cmplx.complex VertexPair]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;-------------------------------------------------------------

(println "LWJGL: " (org.lwjgl.Version/getVersion))

(let [radius 1737.4
      s2-mesh
      ((comp
        cmplx/subdivide-4
        ;;cmplx/subdivide-4
        ;;cmplx/subdivide-4
        ;;cmplx/subdivide-4
        ;;cmplx/subdivide-4
        )
       (quads/standard-quad-sphere))
      s2 (.embedding s2-mesh)
      txt (update-vals s2 s2/s2-to-txt)
      xyz (update-vals s2 (fn [^Point2S p] (rn/multiply (s2/s2-to-r3 p) radius)))
      rgba (update-vals s2 s2/s2-to-rgba)
      ;; unit vectors pointing out
      dual (update-vals s2 s2/s2-to-r3)
      pairs (sort(cmplx/vertex-pairs (.cmplx s2-mesh)))
      ]
  (doseq [^VertexPair e pairs]
    (let [a (s2 (.z0 e))
          b (s2 (.z1 e))
          i (s2/dateline-crossing a b)]
      (println (.toString e))
      (when i
        (println (debug/simple-string a) "->" (debug/simple-string b))
        (println (debug/simple-string i)))))

  (glfw/arcball-loop
   {:title           "icosamoon"
    :cmplx         (.cmplx s2-mesh)
    :vertex-shader   "src/scripts/clojure/mop/icosamoon/icosamoon-vertex.glsl"
    :fragment-shader "src/scripts/clojure/mop/icosamoon/icosamoon-fragment.glsl"
    :txt-embedding  txt
    :s2-embedding   s2
    :xyz-embedding  xyz
    :dual-embedding  dual
    :rgba-embedding  rgba
    :radius          radius
    :color-image
    (image/get-writeable-raster
     "images/lroc_color_poles_2k.tif"
     "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/lroc_color_poles_2k.tif")
    :elevation-image
    (image/get-writeable-raster
     "images/ldem_4.tif"
     "https://svs.gsfc.nasa.gov/vis/a000000/a004700/a004720/ldem_4.tif")})
  )