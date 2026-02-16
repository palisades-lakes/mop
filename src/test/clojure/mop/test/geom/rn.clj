(set! *warn-on-reflection* true)
(set! *unchecked-math* true)                                ;; for c.math.combinatorics
;;------------------------------------------------------------------------------

(ns ^{:author "palisades dot lakes at gmail dot com"
      :date   "2026-02-16"
      :doc    "Tests for mop.geom.rn."}

  mop.test.geom.rn

  (:require
   [clojure.test :as t]
   [mop.geom.rn :as rn]))

(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.geom.rn clojure:test
;;------------------------------------------------------------------------------
;; TODO: precision relative to aspect ratio?

(defn- check-area [^double a p0 p1 p2]
  (t/is (== a (rn/signed-area p0 p1 p2)))
  (t/is (== a (rn/signed-area p2 p0 p1)))
  (t/is (== a (rn/signed-area p1 p2 p0)))
  (t/is (== (- a) (rn/signed-area p2 p1 p0)))
  (t/is (== (- a) (rn/signed-area p0 p2 p1)))
  (t/is (== (- a) (rn/signed-area p1 p0 p2))))

;; TODO: randomized triangles? more difficult aspect ratios?
;; TODO: at least randomized scaling and translation and (quaternion) rotation.

(t/deftest r2-signed-area
  (t/testing
   "Signed area of 2d triangles."
    (check-area 0.50
                (rn/vector 0.0 0.0) (rn/vector 1.0 0.0) (rn/vector 0.0 1.0))
    (check-area 0.25
                (rn/vector 0.0 0.0) (rn/vector 1.0 0.0) (rn/vector 0.0 0.5))
    (check-area 0.50
                (rn/vector 1.0 1.0) (rn/vector 2.0 1.0) (rn/vector 1.0 2.0))
    (check-area 0.25
                (rn/vector 1.0 1.0) (rn/vector 2.0 1.0) (rn/vector 1.0 1.5))))


;;------------------------------------------------------------------------------
