(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc     "Geometry utilities for the 2-dimensional sphere, S_2.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-05"}

  (:require [mop.geom.rn :as rn])
  (:import
   [clojure.lang IFn]
   [mop.geom.rn Vector4D]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D]
   [org.apache.commons.geometry.euclidean.twod Vector2D]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;----------------------------------------------------------------

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
      (println "xyz" (rn/coordinates (.getVector p)))
      (println "st" x y)
      (Vector2D/of x y))))

;;----------------------------------------------------------------
;; debug coloring
;; TODO: need 4d [0,1]^4 domain for output

(defn ^Vector4D rgba [^Point2S p]
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
