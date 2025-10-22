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

(defn- ^Vector2D window-to-arcball
  "Convert window coordinates to normalized arcball coordinates,
  relative to a unit circle centered in the window."
  [^Arcball ball ^Vector2D window-xy]
  (let [^Vector2D c (:center ball)
        s (double (:scale ball))
        ax (* s (- (.getX window-xy) (.getX c)))
        ay (* s (- (.getY c) (.getY window-xy)))]
    (Vector2D/of ax ay)))

(defn ^Vector3D$Unit sphere-pt
  [^Arcball ball ^Vector2D window-xy]
  "Arcball 'projection' onto unit sphere.
  Follow a negative Z ray from the arcball xy pt into the scene.
  If within the unit circle, return the intersection with the (front half)
  of the unit sphere. If outside, project on the z=0 plane, and then to the unit
  circle resulting from intersecting the z=0 plane with the unit sphere."
  (let [axy (window-to-arcball ball window-xy)
        r2 (.normSq axy)
        x (.getX axy)
        y (.getY axy)]
    (if (> r2 1.0)
      ;; -z ray hits misses unit sphere
      (Vector3D$Unit/from x y 0.0)
      ;; -z ray hits unit sphere
      (Vector3D$Unit/from x y (Math/sqrt (- 1.0 r2))))))

;;----------------------------------------------------------------

(defn ^Arcball update-sphere-pt-origin [^Arcball ball ^Vector2D window-xy]
  (assoc ball :p-origin  (sphere-pt ball window-xy)))

(defn ^Arcball update-q-origin [^Arcball ball ^Vector2D window-xy]
  (let [pt (sphere-pt ball window-xy)
        dq (QuaternionRotation/createVectorRotation (:p-origin ball) pt)
        q (.multiply ^QuaternionRotation (:q-origin ball) dq)]
    (assoc ball :q-origin q)))

(defn ^QuaternionRotation current-q [^Arcball ball ^Vector2D window-xy]
  (let [pt (sphere-pt ball window-xy)
        dq (QuaternionRotation/createVectorRotation
            (:p-origin ball) pt)]
    (.multiply ^QuaternionRotation (:q-origin ball) dq)))

;;----------------------------------------------------------------

