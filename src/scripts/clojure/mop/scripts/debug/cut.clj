(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\cut.clj
;;----------------------------------------------------------------
(ns mop.scripts.debug.cut
  {:doc     "cut mesh at 'dateline'"
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-10-25"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2]
   [mop.geom.tetrahedron :as tetrahedron])
  (:import
    [mop.java.cmplx SimplicialComplex2D  UndirectedOneSimplex ZeroSimplex]
    [mop.java.geom.mesh Mesh]))

;;-------------------------------------------------------------

(let [^Mesh mesh (tetrahedron/s2-up-forwards)
      ^SimplicialComplex2D cmplx (mesh/cmplx mesh)
      ;faces (mesh/faces mesh)
      pairs (cmplx/vertex-pairs cmplx)
      s2 (.embedding mesh)]
  (doseq [^UndirectedOneSimplex pair pairs]
    (let [^ZeroSimplex a (.z0 pair)
          ^ZeroSimplex b (.z1 pair)]
      (println pair)
      (println (s2/dateline-crossing (s2 a) (s2 b))))))




