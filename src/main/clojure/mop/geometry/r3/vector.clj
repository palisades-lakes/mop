(ns mop.geometry.r3.vector

  {:doc     "R3 (double) vectors."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-17"}

  (:require [clojure.math :as math]))

;;--------------------------------------------------------------

(defrecord Vector
  [^double x
   ^double y
   ^double z])

(defn ^Vector scale [^double a ^Vector v]
  (Vector. (* a (:x v)) (* a (:y v)) (* a (:z v))))

(defn ^Vector add
  ([^Vector v0 ^Vector v1]
   (Vector. (+ (:x v0) (:x v1))
            (+ (:y v0) (:y v1))
            (+ (:z v0) (:z v1))))
  ([^Vector v0 ^Vector v1 ^Vector v2]
   (Vector. (+ (:x v0) (:x v1) (:x v2))
            (+ (:y v0) (:y v1) (:y v2))
            (+ (:z v0) (:z v1) (:z v2)))))

(defn dot [^Vector v0 ^Vector v1]
  (+ (* (:x v0) (:x v1))
     (* (:y v0) (:y v1))
     (* (:z v0) (:z v1))))

(defn norm2 [^Vector v] (dot v v))
(defn norm [^Vector v] (math/sqrt (norm2 v)))

(defn ^Vector normalize [^Vector v]
  (let [^double n (norm v)]
    (Vector. (/ (:x v) n) (/ (:y v) n) (/ (:z v) n))))

(defn ^Vector cross [^Vector v0 ^Vector v1]
  (Vector. (- (* (:y v0) (:z v1)) (* (:z v0) (:y v1)))
           (- (* (:z v0) (:x v1)) (* (:x v0) (:z v1)))
           (- (* (:x v0) (:y v1)) (* (:y v0) (:x v1)))))