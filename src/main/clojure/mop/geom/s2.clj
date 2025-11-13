(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc     "Geometry utilities for the 2-dimensional sphere, S_2.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}

  (:require [mop.geom.rn :as rn])
  (:import
   [clojure.lang IFn]
   [mop.geom.rn Vector4D]
   [mop.java.geom Point2U]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.apache.commons.geometry.spherical.twod GreatArc GreatCircles Point2S]
   [org.apache.commons.numbers.core Precision]))

(defn ^Vector3D$Unit s2-to-r3 [^Point2S p] (.getVector p))

(let [TWO_PI (double (* 2.0 Math/PI))
      p (Precision/doubleEquivalenceOfEpsilon 1.0e-12)]

  (defn ^Point2S txt-to-s2 [^Vector2D t]
    "Map [0,1]^2 to S2 (azimuth,polar) with wrap-around for coordinates outside [0,1].
    (x,y) -> (2*PI*x,PI(1-y))"
    (Point2S/of (* (double TWO_PI) (.getX t))
                (* Math/PI (- 1.0 (.getY t)))))

  (defn ^Vector3D s2-to-txt [^Point2S p]
    (Vector2D/of (/ (.getAzimuth p) (double TWO_PI))
                 (- 1.0 (/ (.getPolar p) Math/PI))))

  (defn ^Point2S r3-to-s2 [^Vector3D v] (Point2S/from v))

  (defn ^Point2S u2-to-s2 [^Point2U uv] (.toPoint2S uv))

  (defn ^Vector3D u2-to-r3 [^Point2U uv]
    (s2-to-r3 (u2-to-s2 uv)))

  (defn ^Vector2D u2-to-txt [^Point2U uv]
    (Vector2D/of (/ (.getU uv) TWO_PI)
                 (- 1.0 (/ (.getV uv) Math/PI))))

  (defn ^GreatArc arc [^Point2S a ^Point2S b]
    (GreatCircles/arcFromPoints a b p))

  (defn ^Point2S intersect [^GreatArc ab ^GreatArc cd]
    "See https://observablehq.com/@fil/spherical-intersection"
    (let [a (.getStartPoint ab)
          b (.getEndPoint ab)
          c (.getStartPoint cd)
          d (.getEndPoint cd)]
      (cond
        (or (.eq a c p) (.eq a d p)) a
        (or (.eq b c p) (.eq b d p)) b
        :else
        (let [av (s2-to-r3 a)
              bv (s2-to-r3 b)
              cv (s2-to-r3 c)
              dv (s2-to-r3 d)
              axb (.cross av bv)
              cxd (.cross cv dv)
              axbxcxd (.cross axb cxd)
              norm2 (.normSq axbxcxd)]
          (if (< norm2 1.0e-30)
            nil ;; coplanar singularity, undefined intersection
            (let [axbxcxd (.normalize axbxcxd)
                  ab0 (.dot axbxcxd (.cross axb av))
                  ab1 (.dot axbxcxd (.cross axb bv))
                  cd0 (.dot axbxcxd (.cross cxd cv))
                  cd1 (.dot axbxcxd (.cross cxd dv))]
              (if (or (and (> ab0 0) (< ab1 0) (> cd0 0) (< cd1 0))
                      (and (> ab0 0) (.eq axbxcxd av p))
                      (and (< ab1 0) (.eq axbxcxd bv p))
                      (and (> cd0 0) (.eq axbxcxd cv p))
                      (and (< cd1 0) (.eq axbxcxd dv p)))
                (Point2S/from axbxcxd)
                ;; else check antipode
                (let [axbxcxd (.negate axbxcxd)
                      ab0 (- ab0)
                      ab1 (- ab1)
                      cd0 (- cd0)
                      cd1 (- cd1)]
                  (if (or (and (> ab0 0) (< ab1 0) (> cd0 0) (< cd1 0))
                          (and (> ab0 0) (.eq axbxcxd av p))
                          (and (< ab1 0) (.eq axbxcxd bv p))
                          (and (> cd0 0) (.eq axbxcxd cv p))
                          (and (< cd1 0) (.eq axbxcxd dv p)))
                    (Point2S/from axbxcxd)
                    ;; else no intersection
                    nil)))))))))

  (defn ^Point2S intersect-dateline [^GreatArc ab]
    "Does the arc cross azimuth=0/2PI,polar in [0,PI]?
    See https://observablehq.com/@fil/spherical-intersection"
    (let [a (.getStartPoint ab)
          b (.getEndPoint ab)]
      (cond
        (or (.eq a Point2S/PLUS_K p) (.eq a Point2S/MINUS_K p)) a
        (or (.eq b Point2S/PLUS_K p) (.eq b Point2S/MINUS_K p)) b
        :else
        (let [av (s2-to-r3 a)
              bv (s2-to-r3 b)
              cv (s2-to-r3 Point2S/PLUS_K)
              dv (s2-to-r3 Point2S/MINUS_K)
              axb (.cross av bv)
              cxd (s2-to-r3 Point2S/PLUS_J)
              axbxcxd (.cross axb cxd)
              norm2 (.normSq axbxcxd)]
          (if (< norm2 1.0e-30)
            nil ;; coplanar singularity, undefined intersection
            (let [axbxcxd (.normalize axbxcxd)
                  ab0 (.dot axbxcxd (.cross axb av))
                  ab1 (.dot axbxcxd (.cross axb bv))
                  cd0 (.dot axbxcxd (.cross cxd cv))
                  cd1 (.dot axbxcxd (.cross cxd dv))]
              (if (or (and (> ab0 0) (< ab1 0) (> cd0 0) (< cd1 0))
                      (and (> ab0 0) (.eq axbxcxd av p))
                      (and (< ab1 0) (.eq axbxcxd bv p))
                      (and (> cd0 0) (.eq axbxcxd cv p))
                      (and (< cd1 0) (.eq axbxcxd dv p)))
                (Point2S/from axbxcxd)
                ;; else check antipode
                (let [axbxcxd (.negate axbxcxd)
                      ab0 (- ab0)
                      ab1 (- ab1)
                      cd0 (- cd0)
                      cd1 (- cd1)]
                  (if (or (and (> ab0 0) (< ab1 0) (> cd0 0) (< cd1 0))
                          (and (> ab0 0) (.eq axbxcxd av p))
                          (and (< ab1 0) (.eq axbxcxd bv p))
                          (and (> cd0 0) (.eq axbxcxd cv p))
                          (and (< cd1 0) (.eq axbxcxd dv p)))
                    (Point2S/from axbxcxd)
                    ;; else no intersection
                    nil)))))))))
  )
;;----------------------------------------------------------------

(defmethod rn/coordinates Point2S [^Point2S v]
  [(.getAzimuth v) (.getPolar v)])


(defn ^Point2S point
  ([^Vector3D v] (Point2S/from v))
  ([^double azimuth ^double polar]
   (Point2S/of azimuth polar)))

;;----------------------------------------------------------------

(deftype R3Embedding
  [^Vector3D center
   ^Double radius])

(defn ^R3Embedding r3-embedding [^Vector3D center
                                 ^Double radius]
  (R3Embedding. center radius))

(defmethod rn/transform
  [R3Embedding Point2S]
  [^R3Embedding s ^Point2S p]
  (.add ^Vector (.center s) (.radius s) (s2-to-r3 p)))

;;----------------------------------------------------------------
;; For mapping to eg texture image coordinates.
;; South pole goes to (0,0) and (width,0);
;; north to (0,height) and (width, height)

(deftype EquirectangularEmbedding
  [^double width
   ^double height])

(defn ^EquirectangularEmbedding equirectangular-embedding [w h]
  (EquirectangularEmbedding. w h))

(let [TWO_PI (* 2.0 Math/PI)]
  (defmethod rn/transform
    [EquirectangularEmbedding Point2S]
    [^EquirectangularEmbedding s ^Point2S p]
    (let [x (* (.width s) (/ (.getAzimuth p) TWO_PI))
          y (* (.height s) (/ (.getPolar p) Math/PI))]
      (Vector2D/of x y))))

;;----------------------------------------------------------------
;; debug coloring
;; TODO: need 4d [0,1]^4 domain for output

(defn ^Vector4D s2-to-rgba [^Point2S p]
  (let [^Vector3D v (s2-to-r3 p)]
    (rn/vector (abs (.getX v))
               (abs (.getY v))
               (abs (.getZ v))
               1.0)))

(defmethod rn/transform
  [IFn Point2S]
  [^IFn f ^Point2S p]
  (f p))

;;----------------------------------------------------------------
