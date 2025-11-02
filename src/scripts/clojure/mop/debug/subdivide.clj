(set! *warn-on-reflection* true)
;;(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\debug\subdivide.clj
;;----------------------------------------------------------------
(ns mop.debug.subdivide
  {:doc     "check subdivision"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-01"}

  (:require
   [mop.cmplx.complex :as cmplx]
   [mop.commons.debug :as debug]))

;;-------------------------------------------------------------
(let [z0 (cmplx/simplex)
      z1 (cmplx/simplex)
      z2 (cmplx/simplex)
      z3 (cmplx/simplex)
      z4 (cmplx/simplex)
      z5 (cmplx/simplex)
      q0321 (cmplx/quad z0 z3 z2 z1)
      q1452 (cmplx/quad z1 z4 z5 z2)
      pair (cmplx/quad-complex [q0321 q1452])
      {child :child parents :parents} (cmplx/convex-subdivision-4 pair)]
  (debug/echo pair)
  (debug/echo child)
  (debug/echo parents))


