(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\geotools\roundtrip.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.geotools.roundtrip
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-06"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [javax.imageio IIOImage ImageIO ImageReadParam ImageReader ImageWriteParam ImageWriter]
   [javax.imageio.metadata IIOMetadata]
   [javax.imageio.stream ImageInputStream ImageOutputStream]
   [org.geotools.image.io ImageIOExt]))
;;---------------------------------------------------------------------
(defn ^[ImageReader IIOImage] read [input]
  (let [^ImageInputStream iis (ImageIOExt/createImageInputStream (io/file input))
        ^ImageReader reader (ImageIOExt/getImageioReader iis)
        ^ImageReadParam params (.getDefaultReadParam reader)]
    (.setInput reader iis)

    #_(when (instance? TIFFImageReadParam params)
        (debug/echo (.getAllowedTagSets ^TIFFImageReadParam params))
        (.setReadUnknownTags ^TIFFImageReadParam params true))
    [reader (.readAll reader 0 params)]))
;;----------------------------------------------------------------------
(defn ^[ImageWriter ImageWriteParam] write [^ImageReader reader ^IIOImage image output]
  (let [^ImageWriter writer (ImageIO/getImageWriter reader)
        ^ImageWriteParam write-param (.getDefaultWriteParam writer);
        ^IIOMetadata metadata (.getMetadata image)
        ^ImageOutputStream ios (ImageIO/createImageOutputStream (io/file output))]
    (.setOutput writer ios)
    (.write writer metadata image write-param)
    [writer write-param]))
;;---------------------------------------------------------------------
;; TODO: check metadata
(def suffix "-gtr" )
(defn roundtrip [input]
  (println input)
  (let [output (mci/append-to-filename input suffix)
        [reader-in ^IIOImage image-in] (read input)
        [_writer _writeParam] (write reader-in image-in output)
        rendered-in (.getRenderedImage image-in)
        [_reader-out ^IIOImage image-out] (read output)
        rendered-out (.getRenderedImage image-out)
        ]
    ;(image/write-metadata-markdown input)
    ;(image/write-metadata-markdown output)
    (assert (image/equals? rendered-in rendered-out))))
;;---------------------------------------------------------------------
(doseq [input [
               #_"images/geotools/eo_base_2020_clean_geo.tif"
               "images/geotools/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/geotools/ETOPO_2022_v1_60s_PNW_bed.tif"
               "images/geotools/gebco_08_rev_elev_21600x10800.png"
               "images/geotools/ldem_4.tif"
               "images/geotools/USGS_13_n38w077_dir5.tiff"
               "images/geotools/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (roundtrip input))
;;---------------------------------------------------------------------
