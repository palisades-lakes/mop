(set! *warn-on-reflection* true)
(set! *unchecked-math* true) ;; for c.math.combinatorics
;;------------------------------------------------------------------------------

(ns ^{:author "palisades dot lakes at gmail dot com"
      :date   "2025-11-17"
      :doc    "Tests for mop.geom.s2."}

  mop.test.geom.s2

  (:require [clojure.math.combinatorics :as comb]
            [clojure.test :as t]
            [mop.geom.s2 :as s2])
  (:import [org.apache.commons.geometry.spherical.twod Point2S]))

(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.geom.s2 clojure:test
;;------------------------------------------------------------------------------

(t/deftest s2-signed-area
  (t/testing
   "Signed area of spherical triangles."
    (let [half_pi (* 0.5 Math/PI)
          a (s2/point 0.0 0.0)
          b (s2/point 0.0 half_pi)
          c (s2/point half_pi half_pi)]
      (t/is (== half_pi (s2/signed-area a b c)))
      (t/is (== (- half_pi) (s2/signed-area b a c))))
    (let [a (s2/point 0.0 0.0)
          b (s2/point 0.0 Math/PI)
          c (s2/point Math/PI Math/PI)]
      (t/is (== Math/PI (s2/signed-area a b c)))
      (t/is (== (- Math/PI) (s2/signed-area b a c))))))

;;------------------------------------------------------------------------------

(t/deftest s2-azimuth-range
  (t/testing
   "Point2S azimuth range"
    ;; NOTE: de-duping in s2 point construction,
    ;; possible performance hit
    (let [TWO_PI (double (* 2.0 Math/PI))
          p00 (s2/point 0.0 0.0)
          p10 (s2/point Math/PI 0.0)
          p20 (s2/point TWO_PI 0.0)
          p01 (s2/point 0.0 (/ Math/PI 2))
          p11 (s2/point Math/PI (/ Math/PI 2))
          p21 (s2/point TWO_PI (/ Math/PI 2))
          p02 (s2/point 0.0 Math/PI)
          p12 (s2/point Math/PI Math/PI)
          p22 (s2/point TWO_PI Math/PI)
          eq-dateline [p01 p21]
          n-pole [p00 p20]
          s-pole [p02 p22]
          polar (concat n-pole s-pole)
          dateline (concat polar eq-dateline)
          anti [p10 p11 p12]]
      (doseq [points [n-pole s-pole eq-dateline]]
        (doseq [[^Point2S p ^Point2S q]
                (comb/combinations points 2)]
          (t/is (identical? p q))
          (print-str p \newline q)))
      (doseq [^Point2S p dateline]
        (t/is (== 0.0 (s2/azimuth p))))
      (doseq [^Point2S p anti]
        (t/is (== Math/PI (s2/azimuth p))))
      (doseq [^Point2S p n-pole]
        (t/is (== 0.0 (s2/polar p))))
      (doseq [^Point2S p s-pole]
        (t/is (== Math/PI (s2/polar p))))
      (doseq [^Point2S p eq-dateline]
        (t/is (== (/ Math/PI 2) (s2/polar p))))
      )))

;;------------------------------------------------------------------------------