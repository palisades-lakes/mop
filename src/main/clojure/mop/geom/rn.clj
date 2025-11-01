(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.geom.rn

  {:doc "Geometry utilities for Rn, especially R2 abd R3.
  Hide 3rd party library is used, if any."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:refer-clojure :exclude [vector])

  (:import
   [clojure.lang ISeq]
   [org.apache.commons.geometry.core Vector]
   [org.apache.commons.geometry.euclidean.threed Vector3D Vector3D$Unit]
   [org.apache.commons.geometry.euclidean.threed.rotation QuaternionRotation]
   [org.apache.commons.geometry.euclidean.twod Vector2D Vector2D$Unit]
   [org.apache.commons.numbers.quaternion Quaternion]))

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
   (Vector3D/of (double x) (double y) (double z))))

;;----------------------------------------------------------------

(defmulti coordinates
          "Return a sequence of <code>double</code> coordinates.
          At present: xy, xyz, xyzw, ..."
          class)

(defmethod coordinates Vector2D [^Vector2D v]
  [(.getX v) (.getY v)])

(defmethod coordinates Vector3D [^Vector3D v]
  [(.getX v) (.getY v) (.getZ v)])

;; returning xyzw to match GLSL and usual (?) homogeneous pt representation
;; TODO: swizzle functions for coordinates, eg, (xyzw q) and (wxyz q)
(defmethod coordinates Quaternion [^Quaternion q]
  [(.getX q) (.getY q) (.getZ q) (.getW q)])

;; returning xyzw to match GLSL and usual (?) homogeneous pt representation
;; TODO: swizzle functions for coordinates, eg, (xyzw q) and (wxyz q)
(defmethod coordinates QuaternionRotation [^QuaternionRotation qr]
  (coordinates (.getQuaternion qr)))

(defmethod coordinates ISeq [^ISeq vectors]
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

(defmethod transform
  [Number Vector]
  [^Number f ^Vector x]
  (.multiply x f))

(defmethod transform
  [QuaternionRotation Vector3D]
  [^QuaternionRotation f ^Vector3D x]
  (.apply f x))

;;----------------------------------------------------------------

