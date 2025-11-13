(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc     "Geometry utilities for the 2-dimensional sphere, S_2.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-12"}

  (:require [mop.geom.rn :as rn])
  (:import
   [clojure.lang IFn]
   [mop.geom.rn Vector4D]
   [mop.java.geom Point2U]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;----------------------------------------------------------------

(defmethod rn/coordinates Point2S [^Point2S v]
  [(.getAzimuth v) (.getPolar v)])


(defn ^Point2S point [^Vector3D v] (Point2S/from v))

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
  (.add ^Vector (.center s) (.radius s) (.getVector p)))

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
      #_(println "xyz" (rn/coordinates (.getVector p)))
      #_(println "st" x y)
      (Vector2D/of x y))))

;;----------------------------------------------------------------
;; debug coloring
;; TODO: need 4d [0,1]^4 domain for output

(defn ^Vector4D s2-to-rgba [^Point2S p]
  (let [^Vector3D v (.getVector p)]
    (rn/vector (abs (.getX v))
               (abs (.getY v))
               (abs (.getZ v))
               1.0)))

(defmethod rn/transform
  [IFn Point2S]
  [^IFn f ^Point2S p]
  (f p))

;;----------------------------------------------------------------
(let [TWO_PI (double (* 2.0 Math/PI))]

  (defn ^Point2S txt-to-s2 [^Vector2D t]
    "Map [0,1]^2 to S2 (azimuth,polar) with wrap-around for coordinates outside [0,1].
    (x,y) -> (2*PI*x,PI(1-y))"
    (Point2S/of (* (double TWO_PI) (.getX t))
                (* Math/PI (- 1.0 (.getY t)))))

  (defn ^Vector3D s2-to-txt [^Point2S p]
    (Vector2D/of (/ (.getAzimuth p) (double TWO_PI))
                 (- 1.0 (/ (.getPolar p) Math/PI))))

  (defn ^Vector3D$Unit s2-to-r3 [^Point2S p] (.getVector p))

  (defn ^Point2S r3-to-s2 [^Vector3D v] (Point2S/from v))

  (defn ^Point2S u2-to-s2 [^Point2U uv] (.toPoint2S uv))

  (defn ^Vector3D u2-to-r3 [^Point2U uv]
    (.getVector (.toPoint2S uv)))

  (defn ^Vector2D u2-to-txt [^Point2U uv]
    (Vector2D/of (/ (.getU uv) TWO_PI)
                 (- 1.0 (/ (.getV uv) Math/PI))))
  )

