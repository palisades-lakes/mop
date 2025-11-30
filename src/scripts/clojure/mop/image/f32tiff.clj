(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\f32tiff.clj
;;----------------------------------------------------------------
(ns mop.image.f32tiff
  {:doc
   "Read and write 32bit Float grayscale TIFFs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-29"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imaging :as imaging]
   [mop.image.util :as image])
  (:import
   [java.nio ByteOrder]
   [java.util Arrays]
   [org.apache.commons.imaging
    ImageInfo Imaging]
   [org.apache.commons.imaging.formats.tiff
    AbstractTiffRasterData TiffImageMetadata]))
;;-------------------------------------------------------------
(defn read-write-TiffF32 [input]
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {:keys [^AbstractTiffRasterData raster
                ^ByteOrder byteOrder
                ^TiffImageMetadata metadata]} (imaging/readTiffF32 input)
        w (.getWidth raster)
        h (.getHeight raster)
        nRowsInBlock (int 1)
        nColsInBlock w]
    (println)
    (debug/echo input)
    (image/write-metadata-markdown input)
    (imaging/writeTiffF32 (.getData raster) w h nRowsInBlock nColsInBlock byteOrder metadata output)
    (image/write-metadata-markdown output)))
;;-------------------------------------------------------------
(defn read-write-read-TiffF32 [input]
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in :byteOrder
         ^TiffImageMetadata metadata-in :metadata} (imaging/readTiffF32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        nRowsInBlock (int 1)
        nColsInBlock w
        ^floats pixels-in (.getData raster-in)
        ^ImageInfo info-in (Imaging/getImageInfo input)]
    (debug/echo metadata-in)
    (imaging/writeTiffF32 pixels-in w h nRowsInBlock nColsInBlock byteOrder-in metadata-in output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out :byteOrder
           ^TiffImageMetadata metadata-out :metadata}
          (imaging/readTiffF32 output)
          ^floats pixels-out (.getData raster-out)
          ^ImageInfo info-out (Imaging/getImageInfo output)]
      (debug/echo metadata-out)
      (assert (= byteOrder-in byteOrder-out))
      (assert (Arrays/equals pixels-in pixels-out))
      (assert (imaging/equal-ImageInfo? info-in info-out))
      (assert (imaging/equal-ImageMetadata? metadata-in metadata-out))
      ;; TODO: test metadata consistency
      )))
;;-------------------------------------------------------------

#_(read-write-TiffF32 "images/usgs/USGS_13_n38w077_dir5.tiff")
;; TODO: what's the real range for this file?
(read-write-read-TiffF32 "images/moon/ldem_4.tif")
#_(read-write-TiffF32 "images/moon/ldem_4.tif")
;;(tiff-fp-read-write "images/earth/ETOPO_2022_v1_60s_N90W180_geoid.tif")
