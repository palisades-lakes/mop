(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------
(ns mop.test.cmplx.complex
  ^{:author "palisades dot lakes at gmail dot com"
    :date   "2025-11-18"
    :doc    "Tests for mop.cmplx.complex."}
  (:require [clojure.test :as t]
            [mop.cmplx.complex :as cmplx]))

;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.cmplx.complex compile clojure:test
;;------------------------------------------------------------------------------

(t/deftest circular-invariance
  (let [a (cmplx/simplex "a")
        b (cmplx/simplex "b")
        c (cmplx/simplex "c")
        abc (cmplx/simplex a b c)
        abc1 (cmplx/simplex a b c)
        bca (cmplx/simplex b c a)
        cab (cmplx/simplex c a b)
        cba (cmplx/simplex c b a)
        ]
  (t/testing
     (t/is (cmplx/equivalent abc abc1))
     (t/is (cmplx/equivalent abc bca))
     (t/is (cmplx/equivalent abc cab))
     (t/is (not (cmplx/equivalent abc cba))
     ))))

