(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc "Geometry utilities for the 2-dimensional sphere, S_2.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-13"}

  (:require [mop.commons.debug :as debug]
            [mop.geom.rn :as rn])
  (:import
   [clojure.lang IFn]
   [mop.geom.rn Vector4D]
   [mop.java.geom Point2U]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.apache.commons.geometry.spherical.twod GreatArc GreatCircles Point2S]
   [org.apache.commons.numbers.core Precision]))
;;----------------------------------------------------------------

(defmethod debug/simple-string Point2S [^Point2S p]
  (str "s2[" (.getAzimuth p) "," (.getPolar p) "]"))

(defmethod debug/simple-string GreatArc [^GreatArc a]
  (str "arc[" (debug/simple-string (.getStartPoint a))
       "->" (debug/simple-string (.getEndPoint a)) "]"))

;;----------------------------------------------------------------

(defn ^Vector3D$Unit s2-to-r3 [^Point2S p] (.getVector p))

(defn ^Point2S r3-to-s2 [^Vector3D v] (Point2S/from v))

(defn ^Point2S u2-to-s2 [^Point2U uv] (.toPoint2S uv))


#_(defn ^Vector3D u2-to-r3 [^Point2U uv]
    (s2-to-r3 (u2-to-s2 uv)))

(let [TWO_PI (double (* 2.0 Math/PI))
      precision (Precision/doubleEquivalenceOfEpsilon 1.0e-12)]

  (defn ^GreatArc arc [^Point2S a ^Point2S b]
    (GreatCircles/arcFromPoints a b precision))

  (defn ^Point2S txt-to-s2 [^Vector2D t]
    "Map [0,1]^2 to S2 (azimuth,polar) with wrap-around for coordinates outside [0,1].
    (x,y) -> (2*PI*x,PI(1-y))"
    (Point2S/of (* (double TWO_PI) (.getX t))
                (* Math/PI (- 1.0 (.getY t)))))

  (defn ^Vector3D s2-to-txt [^Point2S p]
    (Vector2D/of (/ (.getAzimuth p) (double TWO_PI))
                 (- 1.0 (/ (.getPolar p) Math/PI))))

  (defn ^Vector2D u2-to-txt [^Point2U uv]
    (Vector2D/of (/ (.getU uv) TWO_PI)
                 (- 1.0 (/ (.getV uv) Math/PI))))

  (defn- ^Point2S check-candidate [^Vector3D candidate
                                   ^Vector3D from-normal
                                   ^Vector3D to-normal]
    "Return <code>(r3-to-s2 candidate)</code> if it is a valid intersection;
    otherwise <code>nil</code>."
    (let [^Point2S candidate-s2 (r3-to-s2 candidate)
          candidate-azimuth (.getAzimuth candidate-s2)]
      (if (and (or ;; candidate on dateline
                (.eq precision candidate-azimuth 0.0)
                (.eq precision candidate-azimuth TWO_PI))
               ;; candidate on from-to arc
               (> (.dot candidate from-normal) 0.0)
               (< (.dot candidate to-normal) 0.0))
        candidate-s2
        (do
          ;(debug/echo (debug/simple-string candidate-s2))
          ;  (debug/echo (.eq precision candidate-azimuth 0.0))
          ;  (debug/echo (.eq precision candidate-azimuth TWO_PI))
          ;  (debug/echo (.dot candidate from-normal))
          ;  (debug/echo (.dot candidate to-normal))
          ;  (println)
            nil)
        )))

  (defn ^Point2S dateline-crossing [^Point2S from ^Point2S to]
    "Does the arc cross azimuth=0/2PI,polar in [0,PI]?
    Ignore endpoints on dateline.
    See https://observablehq.com/@fil/spherical-intersection"
    (let [from-azimuth (.getAzimuth from)
          to-azimuth (.getAzimuth to)
          from-polar (.getPolar from)
          to-polar (.getPolar to)]
      (if (or ;; exclude polar edges
           (.eq precision from-polar 0.0)
           (.eq precision to-polar 0.0)
           ;; longitudinal arc, can't cross dateline
           (.eq precision from-azimuth to-azimuth)
           (.eq from to precision) ;; zero length arc
           ;; endpoint on dateline, only want interior crossings (?)
           (.eq precision from-azimuth 0.0)
           (.eq precision to-azimuth 0.0)
           (.eq precision from-azimuth TWO_PI)
           (.eq precision to-azimuth TWO_PI))
        (do
          ;(println (debug/simple-string from) "->" (debug/simple-string to))
          ;(println "endpoint on dateline")
          nil)
        (let [from-r3 (s2-to-r3 from)
              to-r3 (s2-to-r3 to)
              normal (.cross from-r3 to-r3)
              from-normal (.cross normal from-r3)
              to-normal  (.cross normal to-r3)
              candidate (.normalize (.cross normal (s2-to-r3 Point2S/PLUS_J)))]
          (or (check-candidate candidate from-normal to-normal)
              (check-candidate (.multiply candidate -1) from-normal to-normal)))))))

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
