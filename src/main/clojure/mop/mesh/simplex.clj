(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.mesh.simplex
  {:doc "(Abstract) Simplicial complexes."

   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-26"})
;;---------------------------------------------------------------
;; TODO: move these to Java to get better control over construction?
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
  (toString [_] (str "ZeroSimplex(" counter ")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^ZeroSimplex that))))

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
    (str "OneSimplex(" counter "; " (.counter z0) "," (.counter z1)")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^OneSimplex that)))
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
    (str "TwoSimplex(" counter "; "
         (.counter z0) "," (.counter z1) "," (.counter z2) ")"))
  (hashCode [_] counter)

  Comparable
  (compareTo [_ that] (- counter (.counter ^TwoSimplex that)))
  )

;;---------------------------------------------------------------

(let [counter (atom -1)]
  (defn make
    (^ZeroSimplex []
     (ZeroSimplex. (swap! counter inc)))
    (^OneSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1]
     (OneSimplex. (swap! counter inc) z0 z1))
    (^TwoSimplex [^ZeroSimplex z0
                  ^ZeroSimplex z1
                  ^ZeroSimplex z2]
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
  )

(let [counter (atom -1)]
  (defn make-quad ^Quad [^ZeroSimplex z0
                         ^ZeroSimplex z1
                         ^ZeroSimplex z2
                         ^ZeroSimplex z3]
    (Quad. (swap! counter inc) z0 z1 z2 z3)))
;;---------------------------------------------------------------

