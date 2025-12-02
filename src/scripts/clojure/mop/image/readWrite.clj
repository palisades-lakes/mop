(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\readWrite.clj
;;----------------------------------------------------------------
(ns mop.image.readWrite
  {:doc
   "Work out idempotent image read-write roundtrips, for at least NASA and NOAA tiffs and pngs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-30"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imaging :as ming]
   [mop.image.util :as image])
  (:import [java.awt.image BufferedImage]
           [java.io File]
           [javax.imageio ImageIO]
           [org.apache.commons.imaging FormatCompliance ImageFormats Imaging]
           [org.apache.commons.imaging.bytesource ByteSource]
           [org.apache.commons.imaging.formats.tiff
            TiffContents TiffDirectory TiffImageParser TiffImagingParameters TiffReader]
           [org.apache.commons.imaging.formats.tiff.constants TiffTagConstants]
           [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
            PhotometricInterpreterFloat]))
;;-------------------------------------------------------------
(defn- ^TiffDirectory force-tiff-directory-with-image-data [^TiffContents contents ^long index]
  (let [^TiffDirectory directory (.get (.directories contents) index)]
    (assert (.hasTiffImageData directory))
    directory))
;;-------------------------------------------------------------
;; see https://commons.apache.org/proper/commons-imaging/xref-test/org/apache/commons/imaging/examples/tiff/ReadAndRenderFloatingPoint.html#ReadAndRenderFloatingPoint
;; doesn't preserve float32 grayscale format!
(defn roundtrip-tiff-float32 [target]
  (let [^File input (io/file target)
        ^File output (mci/replace-extension (mci/append-to-filename input "-tif") "tif")
        ^ByteSource byteSource (ByteSource/file input)
        ^TiffReader tiffReader (TiffReader. true)
        ^TiffContents contents (.readDirectories tiffReader byteSource true (FormatCompliance/getDefault))
        ^TiffDirectory directory (force-tiff-directory-with-image-data contents 0)
        ^shorts sampleFormat (.getFieldValue directory TiffTagConstants/TIFF_TAG_SAMPLE_FORMAT true)
        samplesPerPixel (.getFieldValue directory TiffTagConstants/TIFF_TAG_SAMPLES_PER_PIXEL)
        ^shorts bitsPerPixel (.getFieldValue directory TiffTagConstants/TIFF_TAG_BITS_PER_SAMPLE true)]
    (assert (== (aget sampleFormat 0) TiffTagConstants/SAMPLE_FORMAT_VALUE_IEEE_FLOATING_POINT))
    ;; debug output
    (print "Bits per pixel: ")
    (dotimes [i samplesPerPixel] (printf "%s%d" (if (> i 0) ", " "") (aget bitsPerPixel i)))
    (println)
    (let [^PhotometricInterpreterFloat pi (PhotometricInterpreterFloat. (float 0.0) (float 1.0))
          ^TiffImagingParameters params (ming/tiff-imaging-parameters pi)
          ;; modifies params (!!!) to get min and max values
          ^BufferedImage _bImage (.getTiffImage directory params)
          maxValue (.getMaxFound pi)
          minValue (.getMinFound pi)
          ^PhotometricInterpreterFloat grayScale (PhotometricInterpreterFloat. minValue maxValue)
          ^TiffImagingParameters params (ming/tiff-imaging-parameters grayScale)
          ;; need to oread a 2nd time with correct min/max
          ^BufferedImage bImage (.getTiffImage directory params)
          ^TiffImageParser parser (TiffImageParser.)]
      (with-open [os (io/output-stream output)]
        (.writeImage parser bImage os params))
      #_(Imaging/writeImage bImage (io/file output) ImageFormats/TIFF)
      #_(ImageIO/write bImage "TIFF" output)
      (image/write-metadata-markdown output))))
;;-------------------------------------------------------------
(defn roundtrip [input]
  (let [output (mci/append-to-filename input "-imaging")
        image (ming/read-image input)
        format (ming/guess-format input)]
    (image/write-metadata-markdown input)
    (ming/write-image image output format)
    (image/write-metadata-markdown output)))
;;-------------------------------------------------------------

(roundtrip-tiff-float32 "images/ldem_4.tif")
(debug/echo ImageFormats/TIFF)
(debug/echo (class ImageFormats/TIFF))

#_(doseq [input ["images/ldem_4.tif"
                 #_"images/lroc_color_poles_2k.tif"]]
    (roundtrip input))

;;-------------------------------------------------------------
