(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\zlib2lzw.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.zlib2lzw
  {:doc
   "ImageIO, ImageIO-ext have problems with zlib tiffs. Convert to LZW."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-10"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.imageio :as imageio])
  (:import
   [javax.imageio IIOImage]))
;;---------------------------------------------------------------------
(def suffix "-lzw")
(defn roundtrip [input]
  (println input)
  (let [input (io/file input)
        output (mci/append-to-filename input suffix)
        [reader-in ^IIOImage image-in] (imageio/read input)]
    (imageio/write reader-in image-in output)))
;;---------------------------------------------------------------------
(doseq [input [
               "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               ]]
  (roundtrip input))
;;---------------------------------------------------------------------
