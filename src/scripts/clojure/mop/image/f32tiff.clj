(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\f32tiff.clj
;;----------------------------------------------------------------
(ns mop.image.f32tiff
  {:doc
   "Read and write 32bit Float grayscale TIFFs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.image.imaging :as ming]
   [mop.image.util :as image])
  (:import
   [java.awt Color]
   [java.awt.image BufferedImage]
   [java.io File]
   [java.nio ByteOrder]
   [java.util List]
   [org.apache.commons.imaging FormatCompliance]
   [org.apache.commons.imaging.bytesource ByteSource]
   [org.apache.commons.imaging.formats.tiff
    AbstractTiffRasterData TiffContents TiffDirectory TiffImagingParameters TiffReader]
   [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
    PaletteEntryForRange PaletteEntryForValue PhotometricInterpreterFloat]))
;;-------------------------------------------------------------
(defn-  ^TiffDirectory directory-with-floating-point-raster [^TiffContents contents]
  (let [ ^TiffDirectory directory (.get (.directories contents) 0)]
    (assert (.hasTiffFloatingPointRasterData directory))
    directory))
;;-------------------------------------------------------------
;; see
;; https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/formats/tiff/TiffFloatingPointReadTest.java

(defn- ^PhotometricInterpreterFloat readAndInterpretTIFF [^File target ^double f0 ^double f1 ^double fNot]
  "Reads a TIFF file using a PhotometricInterpreter with entries for the specified range of values and an arbitrary no-data value. If the image is
   successfully read, the interpreter instance will be returned.

   @param target the specified TIFF file
   @param f0     the expected minimum bound or lower
   @param f1     the expected maximum bound or higher
   @param fNot   an arbitrary non-data value or NaN
   @return if successful, a valid photometric interpreter.
   @throws ImagingException in the event of an unsupported or malformed file data element.
   @throws IOException      in the event of an I/O error
  "
  (let [^ByteSource byteSource (ByteSource/file target)
        ^TiffReader tiffReader (TiffReader. true)
        ^TiffContents contents (.readDirectories tiffReader byteSource true (FormatCompliance/getDefault))
        ^ByteOrder byteOrder (.getByteOrder tiffReader)
        ^TiffDirectory directory (directory-with-floating-point-raster contents)
        ^List pList [(PaletteEntryForValue. fNot Color/red)
                     (PaletteEntryForRange. f0 f1 Color/black Color/white)]
        ^PhotometricInterpreterFloat pInterp (PhotometricInterpreterFloat. pList)
        ^TiffImagingParameters params (ming/tiff-imaging-parameters pInterp)
        ;; reading the image data modifies params!!!
        ^BufferedImage bImage (.getTiffImage directory byteOrder params)]
    (assert (not (nil? bImage)))
    pInterp))

;;-------------------------------------------------------------

(defn- ^AbstractTiffRasterData readRasterFromTIFF
  ([^File target ^TiffImagingParameters params]
   "Reads the floating-point content from a TIFF file.

  @param target the specified TIFF file
  @param params an optional map of parameters for reading.
  @return if successful, a valid raster data instance
  @throws ImagingException in the event of an unsupported or malformed file data element.
  @throws IOException      in the event of an I/O error
 "
   (let [^ByteSource byteSource (ByteSource/file target)
         ^TiffReader tiffReader (TiffReader. true)
         ^TiffContents contents (.readDirectories tiffReader byteSource true (FormatCompliance/getDefault))
         ^TiffDirectory directory (.get (.directories contents) 0)]
     (.getRasterData directory params)))

  ([target] (readRasterFromTIFF (io/file target)))
  )

;;-------------------------------------------------------------
(defn tiff-fp-read [input ^double pmin ^double pmax ^double pmissing]
  (let [input (io/file input)
        ;output (mci/append-to-filename input "-imaging")
        ^PhotometricInterpreterFloat pInterp (readAndInterpretTIFF input pmin pmax pmissing)
        minVal (.getMinFound pInterp)
        maxVal (.getMaxFound pInterp)
        ^AbstractTiffRasterData fullRaster (readRasterFromTIFF input (ming/tiff-imaging-parameters))]
    (println)
    (debug/echo input)
    (debug/echo minVal maxVal)
    (assert (<= pmin minVal maxVal pmax))
    (debug/echo (.getWidth fullRaster) (.getHeight fullRaster))
    (pp/pprint fullRaster)
    (image/write-metadata-markdown input)
    #_(image/write-metadata-markdown output)))
;;-------------------------------------------------------------

(tiff-fp-read "images/usgs/USGS_13_n38w077_dir5.tiff" -2.0 62.0 -99999.0)
;; TODO: what's the real range for this file?
(tiff-fp-read "images/moon/ldem_4.tif" -20.0 20.0 9999.0)
