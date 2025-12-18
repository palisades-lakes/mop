(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\resize.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.resize
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-18"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageiox :as imageiox]
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
  #_(image/write-metadata-markdown input)
  (let [max-dim 16384
        [reader ^IIOImage image] (imageiox/read input)
        resized (imageiox/subsample image max-dim)]
    (when resized
      (let [[w h] (wh resized)
            output (mci/append-to-filename input (str "-" w "x" h))]
        (imageiox/write reader resized output)
        #_(image/write-metadata-markdown output)
        (debug/echo output)))))
;;---------------------------------------------------------------------
(doseq [input (image/image-file-seq (io/file "images"))]
  (resize input))
;;---------------------------------------------------------------------
