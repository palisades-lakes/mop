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
   [mop.cmplx.complex :as cmplx]
   [mop.commons.debug :as debug]
   [mop.geom.icosahedron :as icosahedron]
   [mop.geom.mesh :as mesh]
   [mop.geom.s2 :as s2])
  (:import [mop.cmplx.complex TwoSimplex]
           [mop.geom.mesh Mesh]
           [mop.java.geom Point2U]))

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
          (let [ab (cmplx/simplex (.z0 face) (.z1 face))
                ;bc (cmplx/simplex (.z1 face) (.z2 face))
                ;ca (cmplx/simplex (.z2 face) (.z0 face))
                abi (s2/intersect-dateline (mesh/arc s2 ab))
                bci (s2/intersect-dateline (mesh/arc s2 (.z1 face) (.z2 face)))
                cai (s2/intersect-dateline (mesh/arc s2 (.z2 face) (.z0 face)))]
            (when (or abi bci cai)
              (debug/echo face)
              ;(debug/echo ab abi)
              ;(debug/echo bc bci)
              ;(debug/echo ca cai)
              face)))
        (mesh/faces icosahedron)))))
  (println (count (mesh/faces icosahedron))))

#_(let [a (cmplx/simplex "a")
        b (cmplx/simplex "b")
        c (cmplx/simplex "c")
        d (cmplx/simplex "d")
        ab (cmplx/simplex a b)
        bc (cmplx/simplex b c)
        ca (cmplx/simplex c a)
        cd (cmplx/simplex c d)
        abc (cmplx/simplex a b c)
        theta (double (/ Math/PI 3))
        u2 {a (Point2U/of (- theta) (/ Math/PI 2))
            b (Point2U/of theta (/ Math/PI 2))
            c (Point2U/of 0 theta)
            d (Point2U/of 0 (* 2 theta))
            }
        s2 (update-vals u2 s2/u2-to-s2)
        ab-arc (mesh/arc s2 ab)
        cd-arc (mesh/arc s2 cd)
        ]
    ;(debug/echo u2)
    (debug/echo s2)
    (debug/echo ab-arc)
    (debug/echo cd-arc)
    (debug/echo (s2/intersect ab-arc cd-arc))
    (debug/echo (s2/intersect-dateline ab-arc))
    )


