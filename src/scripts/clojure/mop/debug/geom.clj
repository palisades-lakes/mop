(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\geom.clj
;;----------------------------------------------------------------
(ns mop.debug.geom
  {:doc     "check subdivision"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-12"}
  (:require
   [mop.commons.debug :as debug]
   [mop.geom.mesh :as mesh])
  (:import [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
           [org.apache.commons.geometry.euclidean.twod Vector2D]
           [org.apache.commons.geometry.spherical.twod Point2S]))
;;-------------------------------------------------------------
(let [TWO_PI (double (* 2.0 Math/PI))]

  (defn ^Point2S txt-to-s2 [^Vector2D t]
    "Map [0,1]^2 to S2 (azimuth,polar) with wrap-around for coordinates outside [0,1].
    (x,y) -> (2*PI*x,PI(1-y))"
    (Point2S/of (* TWO_PI (.getX t)) (* Math/PI (- 1.0 (.getY t)))))

  (defn ^Vector3D s2-to-txt [^Point2S p]
    (Vector2D/of (/ (.getAzimuth p) TWO_PI) (- 1.0 (/ (.getPolar p) Math/PI))))

  (defn ^Vector3D$Unit s2-to-r3 [^Point2S p]
    (.getVector p))

  (defn ^Point2S r3-to-s2 [^Vector3D v]
    (Point2S/from v))
  )

(defmacro round-trips [v]
  (println "v:" v)
  `(do
     (debug/echo ~v)
     (debug/echo (txt-to-s2 ~v))
     (debug/echo (s2-to-txt (txt-to-s2 ~v)))
     (debug/echo (s2-to-r3 (txt-to-s2 ~v)))
     (debug/echo (r3-to-s2 (s2-to-r3 (txt-to-s2 ~v))))
     (println)
     )
  )

(defmacro midpoints [t0 t1]
  (let [s0 (gensym t0)
        s1 (gensym t1)]
    `(let [~s0 (txt-to-s2 ~t0)
           ~s1 (txt-to-s2 ~t1)]
       (debug/echo ~t0 ~t1)
       (debug/echo (mesh/midpoint ~t0 ~t1))
       (debug/echo ~s0 ~s1)
       (debug/echo (txt-to-s2 (mesh/midpoint ~t0 ~t1)))
       (debug/echo (mesh/midpoint ~s0 ~s1))
       (println))))

;;-------------------------------------------------------------
(let [;TWO_PI (double (* 2.0 Math/PI))
      ;delta (/ TWO_PI 5.0)
      ;rho (/ Math/PI 3.0)
      dx (double (/ 1.0 9.0))
      dy (double (/ 1.0 3.0))
      ;a (Vector2D/of (* 0 dx) 1)
      ;b (Vector2D/of (* 2 dx) 1)
      f (Vector2D/of (* -1 dx) (* 2 dy))
      g (Vector2D/of (* 1 dx) (* 2 dy))
      q (Vector2D/of (* 10 dx) dy)
      v (Vector2D/of (* 9 dx) 0)]
  ;;(midpoints a b)
  (midpoints f g)
  (midpoints q v)
  ;(round-trips a)
  ;(round-trips b)
  ;(round-trips f)
  ;(round-trips g)
  ;(round-trips q)
  ;(round-trips v)
  )
