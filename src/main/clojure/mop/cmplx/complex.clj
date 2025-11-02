(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.cmplx.complex
  {:doc     "(Abstract) simplicial and cell complexes."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}
  (:require [clojure.set :as set]
            [mop.commons.debug :as debug])
  (:import [clojure.lang ISeq]))
;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
;;---------------------------------------------------------------
;; TODO: order matters for orientation, but also want to avoid
;; singularities: what class should <code>zeros</code> be?

(definterface Cell (vertices []))

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
  (vertices [this] [this])
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
  (vertices [_] [z0 z1])
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
  (vertices [_] [z0 z1 z2]))

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn simplex
    (^ZeroSimplex [] (ZeroSimplex. (swap! counter inc)))
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
;; Abstract 2d simplicial complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

(deftype SimplicialComplex2D
  [^ISeq vertices
   ^ISeq faces]
  :load-ns true

  #_Object
  #_(toString [_]
              (str "SimplicialCmplx2D(" counter "; "
                   (.counter z0) ","
                   (.counter z1) ","
                   (.counter z2) ","
                   (.counter z3) ")"))
  #_(hashCode [this] (System/identityHashCode this))
  #_(equals [this that] (identical? this that))
  )

;;---------------------------------------------------------------

(defmethod debug/simple-string SimplicialComplex2D [^SimplicialComplex2D this]
  (str "SimplicialCmplx2D["
       (apply print-str
              (map #(str \newline " " (debug/simple-string %))
                   (.faces this)))
       "]"))

;;---------------------------------------------------------------
;; TODO: enforce orientedness?

(defn make-simplicial-complex-2d ^SimplicialComplex2D [faces]
  "Accumulate the vertices from the faces, and sort."
  (let [vertices (sort (into #{} (flatten (map #(.vertices ^Cell %) faces))))]
    (SimplicialComplex2D. vertices faces)))

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
  (vertices [_] [z0 z1 z2 z3])
  )

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn quad ^Quad [^ZeroSimplex z0
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
;; Abstract 2d quadrilateral cell complex.
;; <code>deftype</code over <code>defrecord</code> to avoid
;; clojure generated equals and hashCode --- want identity
;; based equality and corresponding hash codes.
;; TODO: check whether this is really necessary
;; TODO: immutable internal collections

(deftype QuadComplex
  [^ISeq vertices
   ^ISeq faces]
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
                   (.faces this)))
       "]"))

;;---------------------------------------------------------------
;; TODO: enforce orientedness?

(defn quad-complex ^QuadComplex [faces]
  "Accumulate the vertices from the faces, and sort."
  (let [vertices (sort (into #{} (flatten (map #(.vertices ^Cell %) faces))))]
    (QuadComplex. vertices faces)))

;;---------------------------------------------------------------
;; TODO: how to ensure that embeddings don't turn the cube inside out?

(defn ^QuadComplex quad-cube []
  "Return an oriented quad complex with 6 faces, topologically
  equivalent to a sphere or cube surface."
  (let [z0 (simplex)
        z1 (simplex)
        z2 (simplex)
        z3 (simplex)
        z4 (simplex)
        z5 (simplex)
        z6 (simplex)
        z7 (simplex)
        q0321 (quad z0 z3 z2 z1)
        q4567 (quad z4 z5 z6 z7)
        q0473 (quad z0 z4 z7 z3)
        q5126 (quad z5 z1 z2 z6)
        q2376 (quad z2 z3 z7 z6)
        q0154 (quad z0 z1 z5 z4)]
    (quad-complex [q0321 q4567 q0473 q5126 q2376 q0154])))

;;---------------------------------------------------------------

(defmulti convex-subdivision-4
          "Return a child cell complex with each face of the parent
          subdivided into 4, splitting faces and edges evenly.
          Also return a map from the new vertices to their parent edge or face,
          to permit computing the child embedding from an embedding of the parent,
          where the child vertex position is some centroid
          of the parent edge or face.
          Returned value looks like
          <code>{:child new-complex :parents {v0 edge0 ... vi facei ...}}"
          class)

(defmethod convex-subdivision-4 QuadComplex [^QuadComplex qc]
  (loop [faces (.faces qc)
         child-faces []
         edge-children {}
         face-children {}]
    (if (empty? faces)
      {:child (quad-complex child-faces)
       :parents (merge (set/map-invert edge-children)
                       (set/map-invert face-children))}
      ;; else
      (let [^Quad face (first faces)
            ^ZeroSimplex z0 (.z0 face)
            ^ZeroSimplex z1 (.z1 face)
            ^ZeroSimplex z2 (.z2 face)
            ^ZeroSimplex z3 (.z3 face)
            ^ZeroSimplex z01 (simplex)
            ^ZeroSimplex z12 (simplex)
            ^ZeroSimplex z23 (simplex)
            ^ZeroSimplex z30 (simplex)
            ^ZeroSimplex z0123 (simplex)
            e01 (sort [z0 z1])
            e12 (sort [z1 z2])
            e23 (sort [z2 z3])
            e30 (sort [z3 z0])]
        (recur (rest faces)
               (concat child-faces [(quad z30 z0 z01 z0123)
                                    (quad z01 z1 z12 z0123)
                                    (quad z12 z2 z23 z0123)
                                    (quad z23 z3 z30 z0123)])
               (merge edge-children {e01 z01 e12 z12 e23 z23 e30 z30})
               (assoc face-children face z0123))))))

;;---------------------------------------------------------------


