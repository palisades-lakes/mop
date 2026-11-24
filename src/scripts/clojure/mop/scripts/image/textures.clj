(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\textures.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.textures
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-11-22"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;----------------------------------------------------------------------
(defn wh [^IIOImage image]
  (if (.hasRaster image)
    [(.getWidth (.getRaster image)) (.getHeight (.getRaster image))]
    [(.getWidth (.getRenderedImage image)) (.getHeight (.getRenderedImage image))]))
;;---------------------------------------------------------------------
(defn resize [input]
  (println)
  (debug/echo input)
  (image/write-metadata-markdown input)
  (let [max-dim 16384
        [reader ^IIOImage image] (imageio/read input)
        resized (imageio/subsample image max-dim)]
    (when resized
      (let [[w h] (wh resized)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (debug/echo output)
        (imageio/write reader resized output)
        (image/write-metadata-markdown output)))))
;;---------------------------------------------------------------------
(doseq [input
        (image/image-file-seq (io/file "images/moon"))
        ]
  (resize input))
;;---------------------------------------------------------------------
