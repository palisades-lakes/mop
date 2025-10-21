(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.util

  {:doc "Geometry utilities"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-21"}

  (:import
   [java.lang Math]
   [org.apache.commons.geometry.euclidean.threed Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.twod Vector2D]))

;;----------------------------------------------------------

(defn ^Vector3D$Unit sphere-pt [^Vector2D screen
                                ^Vector2D center
                                ^double radius]
  "Convert a mouse point on the screen to a point on the unit sphere."
  (let [px (/ (- (.getX screen) (.getX center)) radius)
        py (/ (- (.getY screen) (.getY center)) radius)
        r2 (+ (* px px) (* py py))]
    (if (> r2 1.0)
      (Vector3D$Unit/from px py 0.0)
      (Vector3D$Unit/from px py (Math/sqrt (- 1.0 r2))))))

