;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\cube.clj
;;----------------------------------------------------------------
(ns mop.mesh.cube
  {:author "palisades dot lakes at gmail dot com"
   :version "2025-11-15"}
  (:require
   [mop.commons.debug :as debug]
   [mop.geom.quads :as quads]
   [mop.geom.rn :as rn]
   [mop.cmplx.complex :as cmplx]
   [mop.geom.mesh :as mesh]
   ))

(let [z0 (cmplx/simplex"a")
      z1 (cmplx/simplex"b")
      z2 (cmplx/simplex"c")
      z3 (cmplx/simplex"d")
      z4 (cmplx/simplex"e")
      z5 (cmplx/simplex"f")
      z6 (cmplx/simplex"g")
      z7 (cmplx/simplex"h")
      q0321 (quads/quad z0 z3 z2 z1)
      q4567 (quads/quad z4 z5 z6 z7)
      q0473 (quads/quad z0 z4 z7 z3)
      q5126 (quads/quad z5 z1 z2 z6)
      q2376 (quads/quad z2 z3 z7 z6)
      q0154 (quads/quad z0 z1 z5 z4)
      qcmplx (quads/quad-complex
              [q0321 q4567 q0473 q5126 q2376 q0154])
      p0 (rn/vector -1.0 -1.0 -1.0)
      p1 (rn/vector 1.0 -1.0 -1.0)
      p2 (rn/vector 1.0 1.0 -1.0)
      p3 (rn/vector -1.0 1.0 -1.0)
      p4 (rn/vector -1.0 -1.0 1.0)
      p5 (rn/vector 1.0 -1.0 1.0)
      p6 (rn/vector 1.0 1.0 1.0)
      p7 (rn/vector -1.0 1.0 1.0)
      embedding {z0 p0
                 z1 p1
                 z2 p2
                 z3 p3
                 z4 p4
                 z5 p5
                 z6 p6
                 z7 p7}
      mesh (quads/quad-mesh qcmplx embedding)
      ]
  (debug/echo
   ;;z0 z1 (identical? z0 z1)
   ;;q0321
   ;;embedding
   ;;
   ;; qcmplx
   mesh)
  (mesh/coordinates-and-elements mesh)
  )

