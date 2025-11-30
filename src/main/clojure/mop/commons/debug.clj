(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.debug

  "Things that might be in <code>clojure.core</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-11-29"}

  (:require [mop.commons.string :as mcs]))

;;----------------------------------------------------------------
(defmacro equal-values? [p q f]
  `(= (~f ~p) (~f ~q)))

(defmacro assert-equal-values [p q f]
  `(assert (equal-values? p q f)
           (str ~f " differ:" \newline
                (~f ~p) \newline
                (~f ~q))))
;;----------------------------------------------------------------


(defmacro echo
  "Print the expressions followed by their values.
   Useful for quick logging."
  [& exps]
  (let [ppe (map print-str exps)
        s (gensym "s")
        e (gensym "e")
        v (gensym "v")]
    `(let [~s (mapv (fn [~e ~v]
                      (str ~e \newline
                           (mcs/simple-string ~v)))
                    [~@ppe]
                    [~@exps])]
       (doall (map println ~s))
       (flush))))

;;----------------------------------------------------------------
