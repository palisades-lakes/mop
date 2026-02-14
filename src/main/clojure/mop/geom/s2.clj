(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc "Geometry utilities for the 2-dimensional sphere, S_2.

  Hide 3rd party library is used, if any.

  NOTE: commons geometry seems to de-dupe instances of
  <code>Point2S</code>, a possible performance hit."

   :author  "palisades dot lakes at gmail dot com"
   :version "2026-02-14"}

  (:require
   [mop.commons.debug :as debug]
   [mop.commons.string :as mcs]
   [mop.geom.rn :as rn]
   [mop.geom.space :as space])

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

(defmethod mcs/simple-string Point2S [^Point2S p]
  (str "s2[" (.getAzimuth p) "," (.getPolar p) "]"))

(defmethod mcs/simple-string GreatArc [^GreatArc a]
  (str "arc[" (mcs/simple-string (.getStartPoint a))
       "->" (mcs/simple-string (.getEndPoint a)) "]"))

;;----------------------------------------------------------------

(defn ^Vector3D$Unit s2-to-r3 [^Point2S p] (.getVector p))

(defn ^Point2S r3-to-s2 [^Vector3D v] (Point2S/from v))

(defn ^Point2S u2-to-s2 [^Point2U uv] (.toPoint2S uv))

(defn ^Point2U s2-to-u2 [^Point2S uv] (Point2U/of uv))

(defn ^Vector3D u2-to-r3 [^Point2U uv]
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
                 (/ (.getV uv) Math/PI)))

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
          ;(debug/echo (mcs/simple-string candidate-s2))
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
    (assert (not (nil? from)) (.toString from))
    (assert (not (nil? to)) (.toString to))
    (let [from-azimuth (.getAzimuth from)
          to-azimuth (.getAzimuth to)
          from-polar (.getPolar from)
          to-polar (.getPolar to)]
      (cond (or
             ;; exclude polar edges
             (.eq precision from-polar 0.0)
             (.eq precision to-polar 0.0)
             (.eq precision from-polar Math/PI)
             (.eq precision to-polar Math/PI)
             ;; longitudinal arc, can't cross dateline
             (.eq precision from-azimuth to-azimuth)
             ;; zero length arc
             (.eq from to precision))
            nil
            ;; endpoint on dateline
            (or (.eq precision from-azimuth 0.0) (.eq precision from-azimuth TWO_PI))
            from
            (or (.eq precision to-azimuth 0.0)  (.eq precision to-azimuth TWO_PI))
            to
            :else
            (let [from-r3 (s2-to-r3 from)
                  to-r3 (s2-to-r3 to)
                  normal (.cross from-r3 to-r3)
                  from-normal (.cross normal from-r3)
                  to-normal  (.cross normal to-r3)
                  candidate (.normalize (.cross normal (s2-to-r3 Point2S/PLUS_J)))]
              (or (check-candidate candidate from-normal to-normal)
                  (check-candidate (.multiply candidate -1)
                                   from-normal
                                   to-normal)))))))

;;----------------------------------------------------------------

(defmethod rn/coordinates Point2S [^Point2S v]
  [(.getAzimuth v) (.getPolar v)])

(defn ^Point2S point
  ([^Vector3D v] (Point2S/from v))
  ([^double azimuth ^double polar]
   (Point2S/of azimuth polar)
   ;; force azimuth to zero if polar is...
   ;; TODO: is this a good idea?
   #_(cond (zero? polar) Point2S/PLUS_K
           (== Math/PI polar) Point2S/MINUS_K
           :else (Point2S/of azimuth polar)))
  )

(defn ^double polar [^Point2S p] (.getPolar p))
(defn ^double azimuth [^Point2S p] (.getAzimuth p))

;;----------------------------------------------------------------
;; TODO: defmulti in geom.space?

(defn signed-area [^Point2S a ^Point2S b ^Point2S c]
  "https://www.johndcook.com/blog/2021/11/29/area-of-spherical-triangle/"

  (let [^Vector3D$Unit a (s2-to-r3 a)
        ^Vector3D$Unit b (s2-to-r3 b)
        ^Vector3D$Unit c (s2-to-r3 c)]
    (* 2
       (Math/atan
        (/ (.dot a (.cross b c))
           (+ 1 (.dot a b) (.dot b c) (.dot a c)))))))
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

;----------------------------------------------------------------

(defn- ^Point2S s2-midpoint
  ([^Point2S p0 ^Point2S p1]
   ;; midpoint of geodesic arc from p0 to p1
   (assert (> (.distance p0 p1) 1.0e-7)
           (str "distance=" (.distance p0 p1)))
   (.getMidPoint
    (GreatCircles/arcFromPoints
     p0 p1 (Precision/doubleEquivalenceOfEpsilon 1e-12))))

  ([^Point2S p0 ^Point2S p1 ^Point2S p2 ^Point2S p3]
   "As far as I know, this isn't exactly any common centroid definition."
   (let [m01 (s2-midpoint p0 p1)
         m23 (s2-midpoint p2 p3)
         m12 (s2-midpoint p1 p2)
         m30 (s2-midpoint p3 p0)
         ^Point2S m0123 (s2-midpoint m01 m23)
         ^Point2S m1230 (s2-midpoint m12 m30)]
     ;; should these be equal, ie, a singular arc?
     (if (<= (.distance m0123 m1230) 1.0e-6)
       m0123
       (s2-midpoint m0123 m1230))))
  )

(defmethod space/midpoint Point2S [^Point2S p0 & points]
  (case (count points)
    0 p0
    1 (s2-midpoint p0 (first points))
    3 (let [[^Point2S p1 ^Point2S p2 ^Point2S p3] points]
        (s2-midpoint p0 p1 p2 p3))
    ;; else
    (throw
     (UnsupportedOperationException.
      (str "Can't compute spherical-midpoint of "
           (count points) " points.")))))

;;---------------------------------------------------------------

(defn- ^Point2U u2-midpoint
  ([^Point2U uv0 ^Point2U uv1]
   ;; midpoint of geodesic arc from uv0 to uv1
   ;; rotate about polar axis to avoid singularity
   (let [u0 (.getU uv0)
         u1 (.getU uv1)
         u (Math/min u0 u1)
         p0 (Point2S/of (- u0 u) (.getV uv0))
         p1 (Point2S/of (- u1 u) (.getV uv1))
         mid (.getMidPoint
              (GreatCircles/arcFromPoints
               p0 p1 (Precision/doubleEquivalenceOfEpsilon 1e-12)))]
     (Point2U/of (+ u (.getAzimuth mid)) (.getPolar mid))))

  ([^Point2U p0 ^Point2U p1 ^Point2U p2 ^Point2U p3]
   "As far as I know, this isn't exactly any common centroid definition."
   (let [m01 (u2-midpoint p0 p1)
         m23 (u2-midpoint p2 p3)
         m12 (u2-midpoint p1 p2)
         m30 (u2-midpoint p3 p0)
         ^Point2U m0123 (u2-midpoint m01 m23)
         ^Point2U m1230 (u2-midpoint m12 m30)]
     ;; should these be equal, ie, a singular arc?
     (if (<= (.distance m0123 m1230) 1.0e-6)
       m0123
       (u2-midpoint m0123 m1230))))
  )

;;---------------------------------------------------------------

(defmethod space/midpoint Point2U [^Point2U p0 & points]
  (case (count points)
    0 p0
    1 (u2-midpoint p0 (first points))
    3 (let [[^Point2S p1 ^Point2S p2 ^Point2S p3] points]
        (u2-midpoint p0 p1 p2 p3))
    ;; else
    (throw
     (UnsupportedOperationException.
      (str "Can't compute u2-midpoint of " (count points) " points.")))))

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

(defn ^Vector4D u2-to-rgba [^Point2U p]
  (let [^Vector3D v (u2-to-r3 p)]
    (rn/vector (abs (.getX v))
               (abs (.getY v))
               (abs (.getZ v))
               1.0)))

(defmethod rn/transform
  [IFn Point2S]
  [^IFn f ^Point2S p]
  (f p))

;;----------------------------------------------------------------
