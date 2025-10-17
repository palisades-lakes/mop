(ns mop.geometry.r3.arcball

  {:doc
   "Arcball UI calculations.
    See Shoemake 1992
    ARCBALL: A user interface for specifying three-dimensional
    orientation using a mouse."

   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-17"}

  (:require [clojure.math :as math]
            [mop.geometry.r3.vector :as v3]
            [mop.geometry.r3.quaternion :as q3])

  (:import [mop.geometry.r3.vector Vector]
           [mop.geometry.r3.quaternion Quaternion]
           [mop.geometry.r3.matrix Matrix]))

;;--------------------------------------------------F--------
;; TODO: screen and center are 2d

(defn ^Vector sphere-pt [^Vector screen
                         ^Vector center
                         ^double radius]
  "Convert a mouse point on the screen to a point on the sphere."
  (let [px (/ (- (:x screen) (:x center)) radius)
        py (/ (- (:y screen) (:y center)) radius)
        r (+ (* px px) (* py py))]
    (if (> r 1.0)
      (let [s (/ 1.0 (math/sqrt r))]
        (Vector. (* s px) (* s py) 0.0)
        (Vector. px py (Math/sqrt (- 1.0 r)))))))

;; TODO: p0 and p1 are actually sphere points

(defn ^Quaternion incremental-quaternion [^Vector p0 ^Vector p1]
  "Quaternion corresponding to minimal rotation
  from sphere point p0 to p1."
  (Quaternion. (v3/dot p0 p1) (v3/cross p0 p1)))
