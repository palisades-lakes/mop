(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.arcball

  {:doc     "Arcball UI utilities, independent of window/rendering systems."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-26"}

  (:require [mop.geom.rn :as rn])
  (:import [java.lang Math]))

;;----------------------------------------------------------------
;; encapsulate state needed for arcball rotation controller.
;; TODO: could do this as a 2d affine transform

(defrecord Arcball
  [center ;; in window coordinates
   ^double scale ;; 1/window radius
   p-origin ;; start pt on sphere
   q-origin]) ;; start rotation

(defn ^Arcball ball

  ([^long ww ^long wh p0 q0]
   (let [cx (* ww 0.5)
         cy (* wh 0.5)]
     (Arcball. (rn/vector cx cy) (/ 1.0 (min cx cy)) p0 q0)))

  ([^long ww ^long wh]
   (ball
    ww wh
    (rn/unit-vector 0.0 0.0 1.0)
    (rn/quaternion-identity))))

;;----------------------------------------------------------------
;; TODO: assuming window coordinates have origin in upper left corner,
;; with y increasing downwards, as in GLFW.
;; Is this always true? Probably not.
;;
;; TODO: should support off-center and non-unit spheres, for
;; translation and zoom.

(defn window-to-arcball [^Arcball ball window-xy]
  "Convert window coordinates to normalized arcball coordinates,
  relative to a unit circle centered in the window."
  (let [c (:center ball)
        cx (double (rn/coordinate c 0))
        cy (double (rn/coordinate c 1))
        s (double (:scale ball))
        ww (double (rn/coordinate window-xy 0))
        wh (double (rn/coordinate window-xy 1))
        ax (* s (- ww cx))
        ay (* s (- cy wh))]
    (rn/vector ax ay)))

(defn arcball-to-sphere-pt [axy]
  "Arcball 'projection' onto unit sphere.
  Follow a negative Z ray from the arcball <code>axy</code> pt into the scene.
  If within the unit circle, return the intersection with the (front half)
  of the unit sphere. If outside, project on the z=0 plane, and then to the unit
  circle resulting from intersecting the z=0 plane with the unit sphere."
  (let [r2 (double (rn/norm2 axy))
        [^double x ^double y] (rn/coordinates axy)]
    (if (> r2 1.0)
      ;; -z ray misses unit sphere
      (rn/unit-vector x y 0.0)
      ;; -z ray hits unit sphere
      (rn/unit-vector x y (Math/sqrt (- 1.0 r2))))))

(defn window-to-sphere-pt [^Arcball ball window-xy]
  "Arcball 'projection' onto unit sphere.
  Follow a negative Z ray from the <code>window-xy</code> pt
  into the scene.
  First convert to normalized arcball coordinates.
  If within the unit circle,
  return the intersection with the (front half)
  of the unit sphere.
  If outside, project on the z=0 plane, and then to the unit
  circle resulting from intersecting the z=0 plane
  with the unit sphere."
  (arcball-to-sphere-pt (window-to-arcball ball window-xy)))

;;----------------------------------------------------------------

(defn ^Arcball update-sphere-pt-origin [^Arcball ball window-xy]
  (assoc ball :p-origin (window-to-sphere-pt ball window-xy)))

(defn current-q [^Arcball ball window-xy]
  (let [p (window-to-sphere-pt ball window-xy)
        q (rn/quaternion-from-to (:p-origin ball) p)]
    (rn/quaternion-compose q (:q-origin ball))))

(defn ^Arcball update-q-origin [^Arcball ball
                                window-xy]
  (assoc ball :q-origin (current-q ball window-xy)))

;;----------------------------------------------------------------

