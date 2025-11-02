(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\subdivide.clj
;;----------------------------------------------------------------
(ns mop.debug.subdivide
  {:doc     "check subdivision"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-02"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.commons.debug :as debug]
   [mop.geom.mesh :as mesh]))

;;-------------------------------------------------------------
;; TODO: convert to unit test, check singular cases.

(let [z0 (cmplx/simplex)
      z1 (cmplx/simplex)
      z2 (cmplx/simplex)
      z3 (cmplx/simplex)
      z4 (cmplx/simplex)
      z5 (cmplx/simplex)
      q0321 (cmplx/quad z0 z1 z2 z3)
      q1452 (cmplx/quad z1 z4 z5 z2)
      pair (cmplx/quad-complex [q0321 q1452])
      {child :child parent :parent} (cmplx/subdivide-4 pair)
      mesh (mesh/standard-quad-sphere)
      ]
  (debug/echo pair)
  (debug/echo child)
  (debug/echo parent)
  (debug/echo mesh)
  (debug/echo (cmplx/subdivide-4 mesh)))


