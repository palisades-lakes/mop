(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\iiox\roundtrip.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.iiox.roundtrip
  {:doc
   "Work out idempotent image read-write roundtrips."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-09"}
  (:refer-clojure :exclude [read reduce])
  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.util :as image])
  (:import
   [it.geosolutions.imageio.plugins.tiff BaselineTIFFTagSet TIFFField]
   [it.geosolutions.imageioimpl.plugins.tiff TIFFImageMetadata]
   [javax.imageio IIOImage ImageIO ImageReadParam ImageReader ImageWriteParam ImageWriter]
   [javax.imageio.metadata IIOMetadata]
   [javax.imageio.stream ImageInputStream]))
;;---------------------------------------------------------------------

#_(defn- tiff-predictor-name ^String [^long code]
    (cond
      (== code BaselineTIFFTagSet/PREDICTOR_NONE) "none"
      (== code BaselineTIFFTagSet/PREDICTOR_HORIZONTAL_DIFFERENCING) "HORIZONTAL_DIFFERENCING"
      (== code BaselineTIFFTagSet/PREDICTOR_FLOATING_POINT) "FLOATING_POINT"
      :else
      (throw (IllegalArgumentException.
              (str "Unrecognized predictor code: " code)))))

(defn tiff-predictor ^long [^TIFFImageMetadata metadata]
  (let [root (.getRootIFD metadata)
        field (.getTIFFField root BaselineTIFFTagSet/TAG_PREDICTOR)]
    (if field
      (.getAsLong field 0)
      BaselineTIFFTagSet/PREDICTOR_NONE)))

(defn set-tiff-predictor ^long [^TIFFImageMetadata metadata ^long predictor]
  (let [base (BaselineTIFFTagSet/getInstance)
        field (TIFFField. (.getTag base BaselineTIFFTagSet/TAG_PREDICTOR) predictor)
        root (.getRootIFD metadata)]
    (.addTIFFField root field)))

;;---------------------------------------------------------------------
#_(defn- tiff-compression-name ^String [^long code]
    (cond
      (== code BaselineTIFFTagSet/COMPRESSION_NONE) "none"
      (== code BaselineTIFFTagSet/COMPRESSION_CCITT_RLE) "CCITT_RLE"
      (== code BaselineTIFFTagSet/COMPRESSION_CCITT_T_4) "CCITT_T_4"
      (== code BaselineTIFFTagSet/COMPRESSION_CCITT_T_6) "CCITT_T_6"
      (== code BaselineTIFFTagSet/COMPRESSION_LZW) "LZW"
      (== code BaselineTIFFTagSet/COMPRESSION_OLD_JPEG) "OLD_JPEG"
      (== code BaselineTIFFTagSet/COMPRESSION_JPEG) "JPEG"
      (== code BaselineTIFFTagSet/COMPRESSION_ZLIB) "ZLIB"
      (== code BaselineTIFFTagSet/COMPRESSION_PACKBITS) "PACKBITS"
      (== code BaselineTIFFTagSet/COMPRESSION_DEFLATE)"DEFLATE"
      :else
      (throw (IllegalArgumentException.
              (str "Unrecognized compression code: " code)))))

(defn tiff-compression ^long [^TIFFImageMetadata metadata]
  (let [root (.getRootIFD metadata)
        field (.getTIFFField root BaselineTIFFTagSet/TAG_COMPRESSION)]
    (if field
      (.getAsLong field 0)
      BaselineTIFFTagSet/COMPRESSION_NONE)))

(defn _set-tiff-compression ^long [^TIFFImageMetadata metadata ^long compression]
  (let [base (BaselineTIFFTagSet/getInstance)
        field (TIFFField. (.getTag base BaselineTIFFTagSet/TAG_COMPRESSION) compression)
        root (.getRootIFD metadata)]
    (.addTIFFField root field)))

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
    ;; imageio-ext can't write ZLIB compressions with FLOATING_POINT predictor
    ;; try HORIZONTAL_DIFFERENCING instead
    (when (and (instance? TIFFImageMetadata metadata)
               (= BaselineTIFFTagSet/COMPRESSION_ZLIB (tiff-compression metadata))
               (= BaselineTIFFTagSet/PREDICTOR_FLOATING_POINT (tiff-predictor metadata)))
      (set-tiff-predictor metadata BaselineTIFFTagSet/PREDICTOR_HORIZONTAL_DIFFERENCING))
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
               "images/imageio/ETOPO_2022_v1_60s_PNW_bed.tiff"
               "images/imageio/ETOPO_2022_v1_60s_N90W180_bed.tif"
               "images/imageio/eo_base_2020_clean_geo.tif"
               "images/imageio/gebco_08_rev_elev_21600x10800.png"
               "images/imageio/ldem_4.tif"
               "images/imageio/world.topo.bathy.200412.3x5400x2700.png"
               ]]
  (roundtrip input))
;;---------------------------------------------------------------------
