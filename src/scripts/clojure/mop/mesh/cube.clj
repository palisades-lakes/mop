;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\cube.clj
;;----------------------------------------------------------------
(ns mop.mesh.cube
  (:require
   [clojure.pprint :as pp]
   [mop.geom.util :as geom]
   [mop.mesh.simplex :as simplex]))

(let [z0 (simplex/make)
      z1 (simplex/make)
      z2 (simplex/make)
      z3 (simplex/make)
      z4 (simplex/make)
      z5 (simplex/make)
      z6 (simplex/make)
      z7 (simplex/make)
      q0321 (simplex/make-quad z0 z3 z2 z1)
      q4567 (simplex/make-quad z4 z5 z6 z7)
      q0473 (simplex/make-quad z0 z4 z7 z3)
      q5126 (simplex/make-quad z5 z1 z2 z6)
      q2376 (simplex/make-quad z2 z3 z7 z6)
      q0154 (simplex/make-quad z0 z1 z5 z4)
      p0 (geom/make-vector -1.0 -1.0 -1.0)
      p1 (geom/make-vector 1.0 -1.0 -1.0)
      p2 (geom/make-vector 1.0  1.0 -1.0)
      p3 (geom/make-vector -1.0  1.0 -1.0)
      p4 (geom/make-vector -1.0 -1.0  1.0)
      p5 (geom/make-vector 1.0 -1.0  1.0)
      p6 (geom/make-vector 1.0  1.0  1.0)
      p7 (geom/make-vector -1.0  1.0  1.0)
      embedding {z0 p0
                 z1 p1z2 p2
                 z3 p3
                 z4 p4
                 z5 p5
                 z6 p6
                 z7 p7}
      ]
  (pp/pprint (str "z0: " z0))
  (pp/pprint (str "z1: " z1))
  (pp/pprint (str "(identical? z0 z1): " (identical? z0 z1)))
  (pp/pprint (str "e0: " e0))
  (pp/pprint (str "e1: " e1))
  (pp/pprint (str "(identical? e0 e1): " (identical? e0 e1)))
  (pp/pprint (str "e2: " e2))
  (pp/pprint (str "(identical? e0 e2): " (identical? e0 e2)))
  (pp/pprint (sort [e2 e1 e0]))
  (pp/pprint (str "f0: " f0))
  (pp/pprint
  (try
    (.compareTo z0 e1)
    (catch ClassCastException e
      "Expected ClassCastException thrown."))))

