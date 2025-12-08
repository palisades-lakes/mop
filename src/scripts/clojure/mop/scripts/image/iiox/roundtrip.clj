(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\iiox\roundtrip.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.iiox.roundtrip
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-08"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage ImageIO ImageReadParam ImageReader ImageWriteParam ImageWriter]
   [javax.imageio.metadata IIOMetadata]
   [javax.imageio.stream ImageInputStream]))
;;---------------------------------------------------------------------
(defn ^[ImageReader IIOImage] read [input]
  (let [^ImageInputStream iis (ImageIO/createImageInputStream (io/file input))
        ^ImageReader reader (first (iterator-seq (ImageIO/getImageReaders iis)))
        ^ImageReadParam read-params (.getDefaultReadParam reader)]
    (.setInput reader iis)
    [reader (.readAll reader 0 read-params)]))
;;----------------------------------------------------------------------
(defn ^[ImageWriter ImageWriteParam] write [^ImageReader reader ^IIOImage image output]
  (let [^ImageWriter writer (ImageIO/getImageWriter reader)
        ^ImageWriteParam write-param (.getDefaultWriteParam writer) ;
        ^IIOMetadata metadata (.getMetadata image)]
    #_(.setCompressionMode write-param ImageWriteParam/MODE_EXPLICIT)
    #_(.setCompressionType write-param "LZW")
    ;(debug/echo (.isCompressionLossless write-param))
    ;(debug/echo (.getCompressionType write-param))
    ;(pp/pprint (.getCompressionTypes write-param))
    (try
      (with-open [ios (ImageIO/createImageOutputStream (io/file output))]
        (.setOutput writer ios)
        (.write writer metadata image write-param))
      (finally (.dispose writer)))))
;;---------------------------------------------------------------------
;; TODO: check metadata
(def suffix "-iiox")
(defn roundtrip [input]
  (println input)
  (image/write-metadata-markdown input)
  (let [input (io/file input)
        output (mci/append-to-filename input suffix)
        [reader-in ^IIOImage image-in] (read input)
        [_writer _writeParam] (write reader-in image-in output)
        rendered-in (.getRenderedImage image-in)
        [_reader-out ^IIOImage image-out] (read output)
        rendered-out (.getRenderedImage image-out)
        ]
    (image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(doseq [input [
               "images/imageio/USGS_13_n38w077_dir5.tiff"
               ;"images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
               ;"images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               ;"images/imageio/eo_base_2020_clean_geo.tif"
               ;"images/imageio/gebco_08_rev_elev_21600x10800.png"
               ;"images/imageio/ldem_4.tif"
               ;"images/imageio/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (roundtrip input))
;;---------------------------------------------------------------------
