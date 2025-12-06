(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\imageio\readWrite.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imageio.roundtrip
  {:doc
   "Work out idempotent image read-write roundtrips, for at least NASA and NOAA tiffs and pngs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio]
   [mop.image.util :as image])
  (:import
   [javax.imageio ImageIO]))
;---------------------------------------------------------------------
;; TODO: check metadata
(defn roundtrip [input]
  (println)
  (debug/echo input)
  (let [output (mci/append-to-filename input (str "-imageio"))
        [reader image] (imageio/read input)
        [image _writer _writeParam] (imageio/write reader image output)
        image-in (.getRenderedImage image)
        image-out (ImageIO/read output)]
    (image/write-metadata-markdown input)
    (image/write-metadata-markdown output)
    (assert (image/equals? image-in image-out))))
;;---------------------------------------------------------------------
(doseq [input
        (remove #(.endsWith (mci/prefix %) "ImageReader")
                (image/image-file-seq (io/file "images/imageio")))
        ]
  (roundtrip input))
;;---------------------------------------------------------------------
