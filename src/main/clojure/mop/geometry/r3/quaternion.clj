(ns mop.geometry.r3.quaternion

  {:doc     "Quaternions, simple clojure defrecord for now."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-17"}

  (:require [clojure.math :as math]
            [mop.geometry.r3.vector :as v3])

  (:import [mop.geometry.r3.vector Vector]))

;;--------------------------------------------------------------
;; TODO: scalar w plus vector xyz?

(defrecord Quaternion
  [^double w
   ^Vector xyz])

(defn ^double dot [^Quaternion q0
                   ^Quaternion q1]
  (- (* (:w q0) (:w q1)) (v3/dot (:xyz q0) (:xyz q1))))

(defn ^double mul [^Quaternion q0
                   ^Quaternion q1]
  (Quaternion.
   (- (* (:w q0) (:w q1)) (v3/dot (:xyz q0) (:xyz q1)))
   (v3/add (v3/cross (:xyz q0) (:xyz q1))
           (v3/scale (:w q0) (:xyz q1))
           (v3/scale (:w q1) (:xyz q0)))))



