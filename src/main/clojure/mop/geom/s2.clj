(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.s2

  {:doc     "Geometry utilities for the 2-dimensional sphere, S_2.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:require [mop.geom.rn :as rn])
  (:import
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D]
   [org.apache.commons.geometry.spherical.twod Point2S]))

;;----------------------------------------------------------------

(defn ^Point2S point [^Vector3D v] (Point2S/from v))

;;----------------------------------------------------------------

(deftype SphereEmbedding
  [^Vector3D center
   ^Double radius])

(defn ^SphereEmbedding embedding [^Vector3D center
                                  ^Double radius]
  (SphereEmbedding. center radius))

(defmethod rn/transform
  [SphereEmbedding Point2S]
  [^SphereEmbedding s ^Point2S p]
  (.add ^Vector (.center s) (.radius s) (.getVector p)))

;;----------------------------------------------------------------
