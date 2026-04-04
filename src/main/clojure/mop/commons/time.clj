(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.time

  "Things that might be in <code>clojure.core</code>, and don't have an
  obvious place elsewhere in Mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2026-04-04"}

  (:refer-clojure :exclude [time])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))
;;----------------------------------------------------------------
;; timing
;;----------------------------------------------------------------
;; like clojure.core.time, prefixes results with a message
(defmacro time
  "Evaluates expr and prints the time it took.
   Returns the value of expr."
  ([msg expr]
   `(let [start# (System/nanoTime)
          ret# ~expr
          end# (System/nanoTime)
          msec# (/ (Math/round (/ (double (- end# start#))
                                  10000.0))
                   100.0)]
      (println ~msg (float msec#) "ms")
      ret#))
  ([expr] `(time (str (quote ~@expr)) ~expr)))
;; like clojure.core.time, but reports results rounded to seconds
;; and minutes
(defmacro seconds
  "Evaluates expr and prints the time it took.
  Returns the value of expr."
  ([msg & exprs]
   (let [expr `(do ~@exprs)]
     `(let [
            ^DateTimeFormatter fmt#
            (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
        (println ~msg (.format fmt# (LocalDateTime/now)))
        (let [start# (System/nanoTime)
              ret# ~expr
              end# (System/nanoTime)
              msec# (/ (double (- end# start#)) 1000000.0)
              sec# (/ msec# 1000.0)
              min# (/ sec# 60.0)]
          (println ~msg (.format fmt# (LocalDateTime/now))
                   (str "(" (int (Math/round msec#)) "ms)"
                        " ("(int (Math/round min#))  "m) "
                        (int (Math/round sec#)) "s"))
          ret#))))
  ([exprs] `(seconds "" ~@exprs)))
;;---------------------------------------------------------------------
