(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\s2.clj
;;----------------------------------------------------------------

(ns mop.scripts.debug.s2
  {:doc     "check spherical geometry"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-17"}
  (:require
   [mop.commons.debug :as debug]
   [mop.geom.s2 :as s2])
  (:import [org.apache.commons.geometry.spherical.twod Point2S]))

;;-------------------------------------------------------------
(let [TWO_PI (double (* 2.0 Math/PI))]

  (debug/echo (Point2S/of 0.0 0.0))
  (debug/echo (Point2S/of Math/PI 0.0))
  (debug/echo (Point2S/of TWO_PI 0.0))
  (debug/echo (Point2S/of 0.0 (/ Math/PI 2)))
  (debug/echo (Point2S/of TWO_PI (/ Math/PI 2)))
  (debug/echo (Point2S/of Math/PI (/ Math/PI 2)))
  (debug/echo (Point2S/of 0.0 Math/PI))
  (debug/echo (Point2S/of Math/PI Math/PI))
  (debug/echo (Point2S/of TWO_PI Math/PI))

  (debug/echo (s2/point 0.0 0.0))
  (debug/echo (s2/point Math/PI 0.0))
  (debug/echo (s2/point TWO_PI 0.0))
  (debug/echo (s2/point 0.0 (/ Math/PI 2)))
  (debug/echo (s2/point TWO_PI (/ Math/PI 2)))
  (debug/echo (s2/point Math/PI (/ Math/PI 2)))
  (debug/echo (s2/point 0.0 Math/PI))
  (debug/echo (s2/point Math/PI Math/PI))
  (debug/echo (s2/point TWO_PI Math/PI))

  )
