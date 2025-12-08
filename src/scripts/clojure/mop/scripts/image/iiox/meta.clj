(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\iiox\roundtrip.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.iiox.meta
  {:doc
   "Explore image metadata."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-08"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.image.util :as image])
  (:import
   [java.util Arrays]
   [javax.imageio ImageIO ImageReadParam ImageReader]))
;;---------------------------------------------------------------------
(defn read-for-meta [input]
  (println)
  (println input)
  (image/write-metadata-markdown input)
  (with-open [iis (ImageIO/createImageInputStream (io/file input))]
  (let [readers  (iterator-seq (ImageIO/getImageReaders iis))
        ^ImageReader reader (first readers)
        _(.setInput reader iis)
        ^ImageReadParam read-params (.getDefaultReadParam reader)
        image (.readAll reader 0 read-params)
         metadata (.getMetadata image)
        format-names (seq (Arrays/asList (.getMetadataFormatNames metadata)))
        ;formats (map #(.getMetadataFormat metadata ^String %) format-names)
        extra-format-names (.getExtraMetadataFormatNames metadata)
        extra-format-names (if extra-format-names (Arrays/asList extra-format-names) [])
        ;extra-formats (map #(.getMetadataFormat metadata ^String %) extra-format-names)
        ;formats (concat formats extra-formats)
        format-names (concat format-names extra-format-names)
        dom-trees (map #(.getAsTree metadata ^String %) format-names)
        trees (map xml-seq dom-trees)
        ]
    #_(debug/echo readers)
   #_ (debug/echo (.isReadOnly metadata))
    #_(debug/echo (.isStandardMetadataFormatSupported metadata))
    #_(debug/echo (.getMetadataFormat metadata (.getNativeMetadataFormatName metadata)))
    ;; controllers are nil and unable to find any implementations
    ;(debug/echo (.getController metadata))
    ;(debug/echo (.getDefaultController metadata))
    ;(debug/echo (.hasController metadata))
    ;(debug/echo formats)
    (debug/echo format-names)
    (debug/echo dom-trees)
    (doseq [tree trees] (pp/pprint tree))

    )))
;;;---------------------------------------------------------------------
(doseq [input [
               "images/imageio/USGS_13_n38w077_dir5.tiff"
               "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
               "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/imageio/eo_base_2020_clean_geo.tif"
               "images/imageio/gebco_08_rev_elev_21600x10800.png"
               "images/imageio/ldem_4.tif"
               "images/imageio/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (read-for-meta input))
;;---------------------------------------------------------------------
