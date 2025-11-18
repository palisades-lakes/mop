(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------

(ns ^{:author "palisades dot lakes at gmail dot com"
      :date   "2025-11-17"
      :doc    "Tests for mop.geom.icoshedron."}

  mop.test.geom.icosahedron

  (:require [clojure.test :as t]
            [mop.cmplx.complex :as cmplx]
            [mop.geom.icosahedron :as icosahedron]
            [mop.geom.mesh :as mesh]
            [mop.geom.s2 :as s2])
  (:import [org.apache.commons.geometry.euclidean.threed.mesh TriangleMesh]
           [org.apache.commons.numbers.core Precision]))

;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.geom.icosahedron clojure:test
;;------------------------------------------------------------------------------

(defn check-area [embedding faces]
  (let [total (reduce
               +
               (map
                (fn [f]
                  (let [area (mesh/signed-area embedding f)]
                   (t/is (pos? area))
                   area))
                faces))
        precision (Precision/doubleEquivalenceOfEpsilon 1.0e-14)]
    (t/is (.eq precision (* 4 Math/PI) total))))

(t/deftest orientation
  (t/testing
   (let [^TriangleMesh uncut (icosahedron/s2-icosahedron)
         embedding (mesh/embedding uncut)]
     (check-area embedding (cmplx/faces (mesh/cmplx uncut))))))

(t/testing
 (let [^TriangleMesh cut (icosahedron/s2-cut-icosahedron)
       embedding (mesh/embedding cut)]
   (check-area embedding (cmplx/faces (mesh/cmplx cut)))))

(t/testing
 (let [^TriangleMesh r3 (icosahedron/r3-icosahedron)
       embedding (update-vals
                  (mesh/embedding r3) s2/r3-to-s2)]
   (check-area embedding (cmplx/faces (mesh/cmplx r3)))))

(t/testing
 (let [^TriangleMesh u2 (icosahedron/u2-cut-icosahedron)
       embedding (update-vals
                  (mesh/embedding u2) s2/u2-to-s2)]
   (check-area embedding (cmplx/faces (mesh/cmplx u2))))
  )

;;------------------------------------------------------------------------------
