(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\imageio\resize.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imageio.resize
  {:doc
   "Resize images."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
;; TODO: check metadata
(defn resize [input]
  (println)
  (debug/echo input)
  (let [[reader ^IIOImage image] (imageio/read input)
        ^IIOImage resized (imageio/reduce-iioimage image 16384)
        rendered (.getRenderedImage resized)
        w (.getWidth rendered)
        h (.getHeight rendered)
        output (mci/append-to-filename input (str "-" w "x" h))]
    (imageio/write reader resized output)
    (image/write-metadata-markdown input)
    (image/write-metadata-markdown output)))
;;---------------------------------------------------------------------
(doseq [input (remove #(.endsWith (mci/prefix %) "-imageio")
                      (image/image-file-seq (io/file "images/imageio")))]
  (resize input))
;;---------------------------------------------------------------------
