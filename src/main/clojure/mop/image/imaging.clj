(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.image.imaging

  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-24"}

  (:require [clojure.java.io :as io])
  (:import [java.awt.image BufferedImage]
           [org.apache.commons.imaging ImageFormat Imaging]
           [org.apache.commons.imaging.formats.tiff
            TiffContents TiffDirectory TiffImagingParameters]
           [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
            PhotometricInterpreterFloat]))

;;-------------------------------------------------------------------

(defn ^ImageFormat guess-format [source]
  (Imaging/guessFormat (io/file source)))

(defn ^BufferedImage read-image [source]
  (Imaging/getBufferedImage (io/file source)))

(defn write-image [^BufferedImage image destination ^ImageFormat format]

  (Imaging/writeImage image (io/file destination) format))

;;-------------------------------------------------------------
(defn ^TiffDirectory force-tiff-directory-with-image-data [^TiffContents contents ^long index]
  (let [^TiffDirectory directory (.get (.directories contents) index)]
    (assert (.hasTiffImageData directory))
    directory))

(defn tiff-imaging-parameters
  ([^PhotometricInterpreterFloat pi]
   (let [^TiffImagingParameters params (TiffImagingParameters.)]
     (.setCustomPhotometricInterpreter params pi)
     params))
  ([] (TiffImagingParameters.)))
