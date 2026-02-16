(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.rn

  {:doc     "Geometry utilities for Rn, especially R2 abd R3.
  Hide 3rd party library is used, if any."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:refer-clojure :exclude [vector])

  (:require [mop.geom.space :as space])
  (:import
   [java.util List]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Sum Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
   [org.apache.commons.geometry.euclidean.twod Vector2D Vector2D$Sum Vector2D$Unit]
   [org.apache.commons.numbers.quaternion Quaternion]))

;;----------------------------------------------------------------
;; TODO: composition of embedding functions
;;----------------------------------------------------------------

(defn ^Vector add [^Vector v0 ^Vector v1] (.add v0 v1))

(defn ^Vector subtract [^Vector v0 ^Vector v1]
  (.subtract v0 v1))

(defn ^Vector multiply [^Vector v ^double a] (.multiply v a))

(defn ^Vector normalize [^Vector v] (.normalize v))

(defn ^Double norm2 [^Vector v] (.normSq v))

;(defmulti subtract
;          "Difference of 2 vectors.
;          Must be in the same space."
;          [class class])
;
;(defmethod subtract ^Vector2D
;  [Vector2D Vector2D]
;  [^Vector2D v0 ^Vector2D v1]
;  (.subtract v0 v1))

;;----------------------------------------------------------------
;; TODO: fill this in. Homogeneous coordinates? Spacetime? RGBA?

(deftype Vector4D
  [^Vector3D xyz
   ^double w])

;;----------------------------------------------------------------

(defmethod space/midpoint Vector2D [^Vector2D p0 & points]
  (let [sum (Vector2D$Sum/of p0)]
    (dorun (map #(.add sum %) points))
    (multiply (.get sum) (/ 1.0 (inc (count points))))))

(defmethod space/midpoint Vector3D [^Vector3D p0 & points]
  (let [sum (Vector3D$Sum/of p0)]
    (dorun (map #(.add sum %) points))
    (multiply (.get sum) (/ 1.0 (inc (count points))))))

;;----------------------------------------------------------------

(defn ^Double aspect [^Vector2D v]
  "x/y (meant as width/height)"
  (/ (.getX v) (.getY v)))

;;----------------------------------------------------------------

(defn unit-vector
  (^Vector2D$Unit [x y]
   (Vector2D$Unit/of (double x) (double y)))
  (^Vector3D$Unit [x y z]
   (Vector3D$Unit/of (double x) (double y) (double z))))

(defn vector
  (^Vector2D [x y]
   (Vector2D/of (double x) (double y)))
  (^Vector3D [x y z]
   (Vector3D/of (double x) (double y) (double z)))
  (^Vector4D [x y z w]
   (Vector4D. (Vector3D/of (double x) (double y) (double z))
              (double w))))

;;----------------------------------------------------------------

(defmulti coordinates
          "Return a sequence of <code>double</code> coordinates.
          At present: xy, xyz, xyzw, ..."
          class)

(defmethod coordinates Vector2D [^Vector2D v]
  [(.getX v) (.getY v)])

(defmethod coordinates Vector3D [^Vector3D v]
  [(.getX v) (.getY v) (.getZ v)])

(defmethod coordinates Vector4D [^Vector4D v]
  [(.getX ^Vector3D (.xyz v))
   (.getY ^Vector3D (.xyz v))
   (.getZ ^Vector3D (.xyz v))
   (.w v)])

;; returning xyzw to match GLSL and usual (?) homogeneous pt representation
;; TODO: swizzle functions for coordinates, eg, (xyzw q) and (wxyz q)
(defmethod coordinates Quaternion [^Quaternion q]
  [(.getX q) (.getY q) (.getZ q) (.getW q)])

;; returning xyzw to match GLSL and usual (?) homogeneous pt representation
;; TODO: swizzle functions for coordinates, eg, (xyzw q) and (wxyz q)
(defmethod coordinates QuaternionRotation [^QuaternionRotation qr]
  (coordinates (.getQuaternion qr)))

(defmethod coordinates List [^List vectors]
  (flatten (map coordinates vectors)))

;;----------------------------------------------------------------
;; TODO: avoid intermediate sequence, defmulti?

#_(defn ^doubles double-coordinates [g]
    (double-array (coordinates g)))

(defn float-coordinates ^floats [g]
  (float-array (coordinates g)))

;;----------------------------------------------------------------

(defmulti ^Double coordinate
          "Return a <code>double</code> coordinate."
          (fn [v _] (class v)))

(defmethod coordinate Vector2D [^Vector2D v ^long i]
  (case i
    0 (.getX v)
    1 (.getY v)
    ;; default
    (throw (IllegalArgumentException. (str "Invalid coordinate: " i)))))

(defmethod coordinate Vector3D [^Vector3D v ^long i]
  (case i
    0 (.getX v)
    1 (.getY v)
    2 (.getZ v)
    ;; default
    (throw (IllegalArgumentException. (str "Invalid coordinate: " i)))))

(defmethod coordinate Vector4D [^Vector4D v ^long i]
  (case i
    0 (.getX ^Vector3D (.xyz v))
    1 (.getY ^Vector3D (.xyz v))
    2 (.getZ ^Vector3D (.xyz v))
    3 (.w v)
    ;; default
    (throw (IllegalArgumentException. (str "Invalid coordinate: " i)))))

;;----------------------------------------------------------------

(defn ^QuaternionRotation quaternion-compose [^QuaternionRotation q0
                                              ^QuaternionRotation q1]
  (.multiply q0 q1))

(defn ^QuaternionRotation quaternion-identity []
  (QuaternionRotation/identity))

(defn ^QuaternionRotation quaternion-from-to
  [^Vector3D from ^Vector3D to]
  (QuaternionRotation/createVectorRotation from to))

;;----------------------------------------------------------------
;; TODO: support scaling by a primitive.

(defmulti transform
          "Return a geometric object similar to <code>x</code>,
          transformed by <code>f</code>.
          EG, if f is a number, do linear scaling in all dimensions."
          (fn [f x] [(class f) (class x)]))

;; scaling
(defmethod transform
  [Number Vector]
  [^Number f ^Vector x]
  (multiply x f))

;; translation
(defmethod transform
  [Vector Vector]
  [^Vector f ^Vector x]
  (add x f))

(defmethod transform
  [QuaternionRotation Vector3D]
  [^QuaternionRotation f ^Vector3D x]
  (.apply f x))

;;----------------------------------------------------------------
;; TODO: drop (* 0.5 ...) for orientation testing?

(defn signed-area ^double [^Vector2D p0 ^Vector2D p1 ^Vector2D p2]
  "The signed area of the 2d triangle formed by the 3 points.
  https://cp-algorithms.com/geometry/oriented-triangle-area.html"
  (let [x0 (.getX p0)
        y0 (.getY p0)
        x1 (.getX p1)
        y1 (.getY p1)
        x2 (.getX p2)
        y2 (.getY p2)]
    (* 0.5
       (- (* (- x1 x0) (- y2 y1))
          (* (- x2 x1) (- y1 y0))))))

;;----------------------------------------------------------------

