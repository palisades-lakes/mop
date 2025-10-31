(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.mesh.complex
  {:doc     "(Abstract) simplicial and cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-10-30"}
  (:require [mop.commons.debug :as debug])
  (:import [clojure.lang ISeq]))
;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;;---------------------------------------------------------------
;; TODO: order matters for orientation, but also want to avoid
;; singularities: what class should <code>zeros</code> be?

(definterface Cell (zeros []))

;;---------------------------------------------------------------
;; AKA '(Abstract) Vertex'.
;; The basic unit of identity used to build
;; simplicial and quad complexes, and embedded meshes from them.
;; Most code uses <code>int</code>s as a low overhead substitute,
;; but this is dangerous with multiple meshes that may share
;;  vertices, edges, etc.

(deftype ZeroSimplex
  [^long counter]
  :load-ns true

  Object
  (toString [_] (str "Zero(" counter ")"))
  (hashCode [_] counter)

  ;; ordering will be match order of creation within a thread.
  ;; will be used to identify which point goes with which zero simplex
  ;; in embeddings.
  Comparable
  (compareTo [_ that] (- counter (.counter ^ZeroSimplex that)))

  Cell
  (zeros [this] [this])
  )

;;---------------------------------------------------------------
;; AKA '(Abstract) Edge'.
;; An ordered pair of zero simplexes.

(deftype OneSimplex
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1]
  :load-ns true

  Object
  (toString [_]
    (str "One(" counter "; " (.counter z0) "," (.counter z1)")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^OneSimplex that)))

  Cell
  (zeros [_] [z0 z1])
  )

;;---------------------------------------------------------------
;; AKA '(Abstract) Face/Triangle'.
;; An ordered triple of zero simplexes.

(deftype TwoSimplex
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1
   ^ZeroSimplex z2]
  :load-ns true

  Object
  (toString [_]
    (str "Two(" counter "; "
         (.counter z0) "," (.counter z1) "," (.counter z2) ")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^TwoSimplex that)))

  Cell
  (zeros [_] [z0 z1 z2])
  )

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn make-simplex
    (^ZeroSimplex []
     (ZeroSimplex. (swap! counter inc)))
    (^OneSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1]
     (assert (not= z0 z1))
     (OneSimplex. (swap! counter inc) z0 z1))
    (^TwoSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1
                  ^ZeroSimplex z2]
     (assert (not= z0 z1))
     (assert (not= z0 z2))
     (assert (not= z1 z2))
     (TwoSimplex. (swap! counter inc) z0 z1 z2))))

;;---------------------------------------------------------------
;; Abstract quadrilateral cell.
;; Not a simplex, so maybe should be elsewhere.
;; An ordered quadruple of zero simplexes.

(deftype Quad
  [^long counter
   ^ZeroSimplex z0
   ^ZeroSimplex z1
   ^ZeroSimplex z2
   ^ZeroSimplex z3]
  :load-ns true

  Object
  (toString [_]
    (str "Quad(" counter "; "
         (.counter z0) ","
         (.counter z1) ","
         (.counter z2) ","
         (.counter z3) ")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^Quad that)))

  Cell
  (zeros [_] [z0 z1 z2 z3])
  )

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn make-quad ^Quad [^ZeroSimplex z0
                         ^ZeroSimplex z1
                         ^ZeroSimplex z2
                         ^ZeroSimplex z3]
    (assert (not= z0 z1))
    (assert (not= z0 z2))
    (assert (not= z0 z3))
    (assert (not= z1 z2))
    (assert (not= z1 z3))
    (assert (not= z2 z3))
    (Quad. (swap! counter inc) z0 z1 z2 z3)))

;;---------------------------------------------------------------
;; Abstract quadrilateral cell complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

(deftype QuadComplex
  [^ISeq zeros
   ^ISeq quads]
  :load-ns true

  #_Object
  #_(toString [_]
              (str "Quad(" counter "; "
                   (.counter z0) ","
                   (.counter z1) ","
                   (.counter z2) ","
                   (.counter z3) ")"))
  #_(hashCode [this] (System/identityHashCode this))
  #_(equals [this that] (identical? this that))
  )

;;---------------------------------------------------------------

(defmethod debug/simple-string QuadComplex [^QuadComplex this]
  (str "QuadCmplx["
       (apply print-str
              (map #(str \newline " " (debug/simple-string %))
                   (.quads this)))
       "]"))

;;---------------------------------------------------------------

(defn make-quad-complex ^QuadComplex [quads]
  ;; accumulate the zero simplexes from the quads,
  ;; and sort.
  (let [zeros (sort (into #{} (flatten (map #(.zeros ^Cell %) quads))))]
    (QuadComplex. zeros quads)))

;;---------------------------------------------------------------

