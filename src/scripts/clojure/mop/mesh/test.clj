;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\mesh\test.clj
;;----------------------------------------------------------------
(ns mop.mesh.test
  (:require
   [clojure.pprint :as pp])
  (:import
   [mop.mesh.simplex ZeroSimplex])
  )

(pp/pprint (.toString (ZeroSimplex.)))
(pp/pprint (.toString (ZeroSimplex.)))
(println
 (identical? (.toString (ZeroSimplex.))
             (.toString (ZeroSimplex.))))