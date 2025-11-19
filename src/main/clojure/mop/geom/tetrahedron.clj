(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.tetrahedron
  {:doc     "Tetrahedra with various embeddings."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-18"}
  (:require [mop.cmplx.complex :as cmplx]
            [mop.geom.mesh :as mesh]
            [mop.geom.s2 :as s2])
  (:import [mop.geom.mesh Mesh]))
;;---------------------------------------------------------------

(defn ^Mesh s2-up-forwards []
  (let [da (double (/ Math/PI 3))
        p (double (Math/acos (/ -1.0 3.0)))
        a (cmplx/simplex "a")
        b (cmplx/simplex "b")
        c (cmplx/simplex "c")
        d (cmplx/simplex "d")
        cmplx (cmplx/simplicial-complex-2d
               (map #(apply cmplx/simplex %)
                    [[a b c] [a c d] [a d b] [b d c]]
                    ))
        embedding {a (s2/point 0.0 0.0)
                   b (s2/point (- da) p)
                   c (s2/point Math/PI p)
                   d (s2/point da p)
                   }]
    (mesh/triangle-mesh cmplx embedding)))

;;---------------------------------------------------------------

(defn ^Mesh s2-up-backwards []
  (let [da (double (/ (* 2 Math/PI) 3))
        p (double (Math/acos (/ -1.0 3.0)))
        a (cmplx/simplex "a")
        b (cmplx/simplex "b")
        c (cmplx/simplex "c")
        d (cmplx/simplex "d")
        cmplx (cmplx/simplicial-complex-2d
               (map #(apply cmplx/simplex %)
                    [[a b c] [a c d] [a d b] [b d c]]
                    ))
        embedding {a (s2/point 0.0 0.0)
                   b (s2/point 0 p)
                   c (s2/point (- da) p)
                   d (s2/point da p)
                   }]
    (mesh/triangle-mesh cmplx embedding)))

;;---------------------------------------------------------------
