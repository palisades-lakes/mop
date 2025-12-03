(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\scripts\image\tiff\f32\tilesToCompressedStrips.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.imaging.tiff.f32.tilesToCompressedStrips
  {:doc
   "Read, write, read, and check for consistency.
   Start with 32bit Float grayscale TIFFs with Tiles"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-02"}

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
(defn tiff-f32-read-tiles-write-strips-read [input]
  (debug/echo input)
  (let [input (io/file input)
        output (mci/append-to-filename input "-cstrips")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in :byteOrder
         ^TiffImageMetadata metadata-in :metadata} (imaging/read-tiff-f32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        ^floats pixels-in (.getData raster-in)
        ^ImageInfo info-in (Imaging/getImageInfo input)]
    (debug/echo output)
    (debug/echo metadata-in)
    (imaging/write-tiff-f32-strips pixels-in w h true byteOrder-in metadata-in output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out :byteOrder
           ^TiffImageMetadata metadata-out :metadata} (imaging/read-tiff-f32 output)
          ^floats pixels-out (.getData raster-out)
          ^ImageInfo info-out (Imaging/getImageInfo output)
          ]
      (debug/echo metadata-out)
      (image/write-metadata-markdown output)
      (assert (= byteOrder-in byteOrder-out))
      (assert (imaging/equal-ImageInfo? info-in info-out))
      (assert (imaging/equal-ImageMetadata? metadata-in metadata-out)
              (str metadata-in \newline metadata-out))
      (assert (== (alength pixels-in) (alength pixels-out)))
      (assert (Arrays/equals ^floats pixels-in ^floats pixels-out))
      )))
;;-------------------------------------------------------------

(dorun
 (map tiff-f32-read-tiles-write-strips-read
      [
      ;"images/earth/usgs/USGS_13_n38w077_dir5.tiff"
      ; "images/earth/ETOPO/ETOPO_2022_v1_60s_PNW_bed.tiff"
       "images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_bed.tif"
       ; "images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_geoid.tif"
      ; "images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_surface.tif"
      ]))
