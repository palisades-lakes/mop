(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\earth\textures.clj
;;----------------------------------------------------------------
(ns mop.scripts.earth.textures
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-02-14"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageiox :as iiox]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
(defn resize [input]
  (println)
  (debug/echo input)
  (image/write-metadata-markdown input)
  (let [[reader image] (iiox/read input)
        ^IIOImage resized (iiox/subsample image 16384)]
    (when resized
      (let [rendered (.getRenderedImage resized)
            w (.getWidth rendered)
            h (.getHeight rendered)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (iiox/write reader resized output)
        (image/write-metadata-markdown output)))))
;;---------------------------------------------------------------------
(doseq [input (remove #(.startsWith (mci/prefix %) "usgs")
                      (image/image-file-seq (io/file "images/earth")))]
  (resize input))
;;---------------------------------------------------------------------
