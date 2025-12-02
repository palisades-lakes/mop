(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\tiffTilesF32ReadWriteRead.clj
;;----------------------------------------------------------------
(ns mop.image.tiffTilesF32ReadWriteRead
  {:doc
   "Read, write, read, and check for consistency.
   Start with 32bit Float grayscale TIFFs with Tiles"
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-01"}

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
#_(defn assert-array-equals? [^floats a ^floats b]
  (assert (== (alength a) (alength b)))
  (dotimes [i (alength a)]
    (let [ai (float (aget a i))
          bi (float (aget b i))]
      (assert (= ai bi)
              (str "element " i " differs:  " ai " != " bi))
      (assert (== (Float/floatToRawIntBits ai) (Float/floatToRawIntBits bi))
              (str "element " i " differs:  " ai " != " bi))
      (assert (== ai bi)
              (str "element " i " differs:  " ai " != " bi))
      )))
#_(defn find-element [^floats a]
  (loop [i (dec (alength a))]
    (when (<= 0 i)
      (let [ai (float (aget a i))]
        (if (< Float/MIN_VALUE ai Float/MAX_VALUE)
          (println i ":" ai)
          (recur (dec i)))))))
;;-------------------------------------------------------------
(defn tiff-f32-read-tiles-write-strips-read [input]
  (debug/echo input)
  (let [input (io/file input)
        output (mci/append-to-filename input "-strips")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in :byteOrder
         ^TiffImageMetadata metadata-in :metadata} (imaging/read-tiff-f32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        ^floats pixels-in (.getData raster-in)
        ^ImageInfo info-in (Imaging/getImageInfo input)]
    (debug/echo output)
    #_(debug/echo metadata-in)
    (imaging/write-tiff-f32-strips pixels-in w h byteOrder-in metadata-in output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out :byteOrder
           ^TiffImageMetadata metadata-out :metadata}
          (imaging/read-tiff-f32 output)
          ^floats pixels-out (.getData raster-out)
          ^ImageInfo info-out (Imaging/getImageInfo output)
          ]
      (image/write-metadata-markdown output)
      (assert (= byteOrder-in byteOrder-out))
      (assert (imaging/equal-ImageInfo? info-in info-out))
      (assert (imaging/equal-ImageMetadata? metadata-in metadata-out)
              (str metadata-in \newline metadata-out))
      (assert (== (alength pixels-in) (alength pixels-out)))
      (assert (Arrays/equals ^floats pixels-in ^floats pixels-out))
      )))
;;-------------------------------------------------------------

;; TODO: what's the real range for this file?
#_(tiff-f32-read-tiles-write-strips-read "images/usgs/USGS_13_n38w077_dir5.tiff")
(dorun
 (map tiff-f32-read-tiles-write-strips-read
      ["images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_bed.tif"
       "images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_geoid.tif"
       "images/earth/ETOPO/ETOPO_2022_v1_60s_N90W180_surface.tif"]))
