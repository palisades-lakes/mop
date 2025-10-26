(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.mesh.simplex
  {:doc "(Abstract) Simplicial complexes."

   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-26"})

;; AKA 'Vertex'.
;; The basic atom of identity used to build
;; abstract simplicial and quad complexes,
;; and embedded meshes from them.
;; Most code uses <code>int</code>s as a low overhead substitute,
;; but this is dangerous with multiple meshes that may share
;;  vertices, edges, etc.

(defrecord ZeroSimplex
  [] ;; no fields for now
  :load-ns true
  Object
  (toString [this]
    (str
     (.getSimpleName (.getClass this))
     "@"
     (Integer/toHexString (.hashCode this))))

  )