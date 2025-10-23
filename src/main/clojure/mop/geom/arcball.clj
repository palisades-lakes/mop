(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.arcball

  {:doc "Arcball UI utilities, independent of window/rendering systems."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-22"}

  (:import
   [java.lang Math]
   [org.apache.commons.geometry.euclidean.threed Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
   [org.apache.commons.geometry.euclidean.twod Vector2D]))

;;----------------------------------------------------------------
;; encapsulate state needed for arcball rotation controller.
;; TODO: could do this as a 2d affine transform

(defrecord Arcball
  [^Vector2D center ;; in window coordinates
   ^double scale ;; 1/window radius
   ^Vector3D$Unit p-origin ;; start pt on sphere
   ^QuaternionRotation q-origin]) ;; start rotation

(defn ^Arcball ball

  ([^long ww ^long wh ^Vector3D$Unit p0 ^QuaternionRotation q0]
   (let [cx (* ww 0.5)
         cy (* wh 0.5)]
     (Arcball. (Vector2D/of cx cy) (/ 1.0 (min cx cy)) p0 q0)))

  ([^long ww ^long wh]
   (ball
    ww wh
    (Vector3D$Unit/from 0.0 0.0 1.0)
    (QuaternionRotation/identity))))

;;----------------------------------------------------------------
;; TODO: assuming window coordinates have origin in upper left corner,
;; with y increasing downwards, as in GLFW.
;; Is this always true? Probably not.
;;
;; TODO: should support off-center and non-unit spheres, for
;; translation and zoom.

(defn ^Vector2D window-to-arcball [^Arcball ball ^Vector2D window-xy]
  "Convert window coordinates to normalized arcball coordinates,
  relative to a unit circle centered in the window."
  (let [^Vector2D c (:center ball)
        s (double (:scale ball))
        ax (* s (- (.getX window-xy) (.getX c)))
        ay (* s (- (.getY c) (.getY window-xy)))]
    (Vector2D/of ax ay)))

(defn ^Vector3D$Unit arcball-to-sphere-pt [^Vector2D axy]
  "Arcball 'projection' onto unit sphere.
  Follow a negative Z ray from the arcball <code>axy</code> pt into the scene.
  If within the unit circle, return the intersection with the (front half)
  of the unit sphere. If outside, project on the z=0 plane, and then to the unit
  circle resulting from intersecting the z=0 plane with the unit sphere."
  (let [r2 (.normSq axy)
        x (.getX axy)
        y (.getY axy)]
    (if (> r2 1.0)
      ;; -z ray misses unit sphere
      (Vector3D$Unit/from x y 0.0)
      ;; -z ray hits unit sphere
      (Vector3D$Unit/from x y (Math/sqrt (- 1.0 r2))))))

(defn ^Vector3D$Unit window-to-sphere-pt [^Arcball ball
                                          ^Vector2D window-xy]
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
  (arcball-to-sphere-pt
   (window-to-arcball ball window-xy)))

;;----------------------------------------------------------------

(defn ^Arcball update-sphere-pt-origin [^Arcball ball
                                        ^Vector2D window-xy]
  (assoc ball :p-origin (window-to-sphere-pt ball window-xy)))

(defn ^QuaternionRotation current-q [^Arcball ball
                                     ^Vector2D window-xy]
  (let [p (window-to-sphere-pt ball window-xy)
        q (QuaternionRotation/createVectorRotation (:p-origin ball) p)]
    (.multiply q (:q-origin ball))))

(defn ^Arcball update-q-origin [^Arcball ball
                                ^Vector2D window-xy]
  (assoc ball :q-origin (current-q ball window-xy)))

;;----------------------------------------------------------------

