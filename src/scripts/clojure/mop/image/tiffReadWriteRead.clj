(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\tiffReadWriteRead.clj
;;----------------------------------------------------------------
(ns mop.image.tiffReadWriteRead
  {:doc
   "Read, write, read, and check for consistency.
   Start with 32bit Float grayscale TIFFs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-01"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.io :as mci]
   [mop.image.imaging :as imaging])
  (:import
   [java.nio ByteOrder]
   [java.util Arrays]
   [org.apache.commons.imaging
    ImageInfo Imaging]
   [org.apache.commons.imaging.formats.tiff
    AbstractTiffRasterData TiffImageMetadata]))
;;-------------------------------------------------------------
(defn read-write-read-tiff-f32-strips [input]
  (let [input (io/file input)
        output (mci/append-to-filename input "-test")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in :byteOrder
         ^TiffImageMetadata metadata-in :metadata} (imaging/read-tiff-f32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        ;nRowsInBlock (int 1)
        ;nColsInBlock w
        ^floats pixels-in (.getData raster-in)
        ^ImageInfo info-in (Imaging/getImageInfo input)]
    ;(debug/echo metadata-in)
    (imaging/write-tiff-f32-strips pixels-in w h byteOrder-in metadata-in output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out :byteOrder
           ^TiffImageMetadata metadata-out :metadata}
          (imaging/read-tiff-f32 output)
          ^floats pixels-out (.getData raster-out)
          ^ImageInfo info-out (Imaging/getImageInfo output)]
      ;(debug/echo metadata-out)
      (assert (= byteOrder-in byteOrder-out))
      (assert (imaging/equal-ImageInfo? info-in info-out))
      (assert (imaging/equal-ImageMetadata? metadata-in metadata-out)
              (str metadata-in \newline metadata-out))
      (assert (Arrays/equals pixels-in pixels-out))
      ;; TODO: test metadata consistency
      )))
;;-------------------------------------------------------------

;; TODO: what's the real range for this file?
#_(read-write-read-tiff-f32-strips "src/test/resources/images/ldem_4.tif")
(read-write-read-tiff-f32-strips "images/usgs/USGS_13_n38w077_dir5.tiff")
#_(read-write-read-tiff-f32-strips "images/earth/ETOPO_2022_v1_60s_N90W180_geoid.tif")
