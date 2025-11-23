(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.commons.io

  "Things that ought to be in <code>clojure.java.io</code>, and don't have an
  obvious place elsewhere in mop."

  {:author  "palisades dot lakes at gmail dot com"
   :version "2025-11-23"}

  (:require [clojure.string :as s])
  (:import [java.io File]))

;;----------------------------------------------------------------

(defn prefix ^String [^File f]
  (let [filename (.getName f)
        i (s/last-index-of filename ".")]
    (if (nil? i)
      filename
      (subs filename 0 i))))

(defn extension ^String [^File f]
  (let [filename (.getName f)
        i (s/last-index-of filename ".")
        ^String ext (if (nil? i)
                      ""
                      (subs filename (inc (long i))))]
    (if (>= 5 (.length ext) 2) ext "")))

