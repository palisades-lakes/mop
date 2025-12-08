(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.arrays

  "Things that ought to be in <code>clojure.string</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-12-08"}
  (:require [mop.commons.string :as mcs]))

;;----------------------------------------------------------------
;; primitive arrays
;;----------------------------------------------------------------
(def ^Class BooleanArray (let [a (boolean-array 0)] (class a)))
(defn boolean-array? [x] (instance? BooleanArray x))
(def ^Class ByteArray (let [a (byte-array 0)] (class a)))
(defn byte-array? [x] (instance? ByteArray x))
(def ^Class CharArray (let [a (char-array 0)] (class a)))
(defn char-array? [x] (instance? CharArray x))
(def ^Class DoubleArray (let [a (double-array 0)] (class a)))
(defn double-array? [x] (instance? DoubleArray x))
(def ^Class FloatArray (let [a (float-array 0)] (class a)))
(defn float-array? [x] (instance? FloatArray x))
(def ^Class IntArray (let [a (int-array 0)] (class a)))
(defn int-array? [x] (instance? IntArray x))
(def ^Class LongArray (let [a (long-array 0)] (class a)))
(defn long-array? [x] (instance? LongArray x))
(def ^Class ShortArray (let [a (short-array 0)] (class a)))
(defn short-array? [x] (instance? ShortArray x))
;;----------------------------------------------------------------
;; more general arrays
;;----------------------------------------------------------------
(def ^Class ObjectArray (let [a (object-array 0)] (class a)))
(defn object-array? [x] (instance? ObjectArray x))

(defn element-type ^Class [x]
  (let [c (.getClass ^Object x)]
    (assert (.isArray c))
    (.getComponentType c)))

(defn array?
  ([x] (and x (.isArray (.getClass ^Object x))))
  ([x ^Class c]
   (and (array? x)
        (.equals c (element-type x)))))

(defn elements-assignable-from?
  [x ^Class c]
  (and (array? x)
       (.isAssignableFrom (element-type x) c)))
;;----------------------------------------------------------------
(defmethod mcs/simple-string BooleanArray [^booleans x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string ByteArray [^bytes x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string CharArray [^chars x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string DoubleArray [^doubles x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string FloatArray [^floats x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string IntArray [^ints x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string LongArray [^longs x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string ShortArray [^shorts x]
  (java.util.Arrays/toString x))
(defmethod mcs/simple-string ObjectArray [^objects x]
  (java.util.Arrays/toString x))
