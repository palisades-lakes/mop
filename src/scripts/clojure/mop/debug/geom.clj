(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\geom.clj
;;----------------------------------------------------------------
(ns mop.debug.geom
  {:doc     "check subdivision"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-15"}
  (:require
   [mop.commons.debug :as debug]
   [mop.geom.s2 :as s2]
   [mop.geom.space :as space])
  (:import [org.apache.commons.geometry.euclidean.twod Vector2D]))
;;-------------------------------------------------------------

(defmacro round-trips [v]
  (println "v:" v)
  `(do
     (debug/echo ~v)
     (debug/echo (s2/txt-to-s2 ~v))
     (debug/echo (space/midpoint (s2/txt-to-s2 ~v)))
     (debug/echo (space/midpoint (s2/txt-to-s2 ~v)))
     (debug/echo (s2/r3-to-s2 (space/midpoint (s2/txt-to-s2 ~v))))
     (println)
     )
  )

(defmacro midpoints [t0 t1]
  (let [s0 (gensym t0)
        s1 (gensym t1)]
    `(let [~s0 (s2/txt-to-s2 ~t0)
           ~s1 (s2/txt-to-s2 ~t1)]
       (debug/echo ~t0 ~t1)
       (debug/echo (space/midpoint ~t0 ~t1))
       (debug/echo ~s0 ~s1)
       (debug/echo (s2/txt-to-s2 (space/midpoint ~t0 ~t1)))
       (debug/echo (space/midpoint ~s0 ~s1))
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
