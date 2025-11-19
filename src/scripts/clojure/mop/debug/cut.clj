(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\cut.clj
;;----------------------------------------------------------------
(ns mop.debug.cut
  {:doc     "cut mesh at 'dateline'"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-18"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.geom.tetrahedron :as tetrahedron])
  (:import [mop.cmplx.complex SimplicialComplex2D VertexPair]
           [mop.geom.mesh Mesh]))

;;-------------------------------------------------------------

(let [^Mesh mesh (tetrahedron/s2-up-forwards)
      ^SimplicialComplex2D cmplx (mesh/cmplx mesh)
      ;faces (mesh/faces mesh)
      pairs (cmplx/vertex-pairs cmplx)
      s2 (.embedding mesh)]
  (doseq [^VertexPair pair pairs]
    (let [^ZeroSimplex a (.z0 pair)
          ^ZeroSimplex b (.z1 pair)]
      (println pair)
      (println (s2/dateline-crossing (s2 a) (s2 b))))))




