(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\geojson\icosahedron.clj
;;----------------------------------------------------------------
(ns mop.scripts.geojson.icosahedron
  {:doc "(Uncut) icosahedron in geojson (geoedn) format."
   :author "palisades dot lakes at gmail dot com"
   :version "2026-03-04"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.commons.json :as json]
   [mop.cmplx.complex :as cmplx]
   [mop.geom.icosahedron :as icosahedron]
   [mop.io.geojson :as geojson])

  (:import
   [mop.geom.mesh Mesh]))
;;----------------------------------------------------------------

(let [^Mesh initial (icosahedron/s2-icosahedron)
      ^Mesh icosahedron ((comp
                          cmplx/midpoint-subdivide-4
                          cmplx/midpoint-subdivide-4
                          cmplx/midpoint-subdivide-4
                          )
                         initial)
      edn (geojson/geo-edn icosahedron)]
  (debug/echo (count (.vertices (.cmplx initial))))
  (debug/echo (count (.faces (.cmplx initial))))
  (debug/echo (count (.vertices (.cmplx icosahedron))))
  (debug/echo (count (.faces (.cmplx icosahedron))))
  (json/write-json edn "out/geojson/icosahedron.json")
  (pp/pprint edn (io/writer "out/geojson/icosahedron.edn"))
  )