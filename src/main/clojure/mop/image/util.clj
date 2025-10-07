(ns mop.image.util

  {:doc "Image utilities."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-07"}

  (:require [clojure.java.io :as io])
  )

(defn download [url target]
  (with-open [in (io/input-stream url)
              out (io/output-stream target)]
    (print "Downloading" target "... ")
    (flush)
    (io/copy in out)
    (println "done")))
