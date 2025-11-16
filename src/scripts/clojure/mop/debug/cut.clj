(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\cut.clj
;;----------------------------------------------------------------
(ns mop.debug.cut
  {:doc     "cut mesh at 'dateline'"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}

  (:require
   [mop.commons.debug :as debug]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2])
  (:import [mop.cmplx.complex TwoSimplex]
           [mop.geom.mesh Mesh]))

;;-------------------------------------------------------------

(let [^Mesh icosahedron ((comp
                          ;cmplx/subdivide-4
                          ;cmplx/subdivide-4
                          ;cmplx/subdivide-4
                          ;cmplx/subdivide-4
                          ;cmplx/subdivide-4
                          )
                         (icosahedron/s2-icosahedron))
      s2 (.embedding icosahedron)]
  (println
   (count
   (filter
    #(not (nil? %))
   (map (fn [^TwoSimplex face]
          (let [abi (s2/dateline-crossing (s2 (.z0 face)) (s2 (.z1 face)))
                bci (s2/dateline-crossing (s2 (.z1 face)) (s2 (.z2 face)))
                cai (s2/dateline-crossing (s2 (.z2 face)) (s2 (.z0 face)))]
            (when (or abi bci cai)
              (debug/echo face)
              ;(debug/echo ab abi)
              ;(debug/echo bc bci)
              ;(debug/echo ca cai)
              face)))
        (mesh/faces icosahedron)))))
  (println (count (mesh/faces icosahedron))))




