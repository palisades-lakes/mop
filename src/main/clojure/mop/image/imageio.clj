(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;---------------------------------------------------------------------
(ns mop.image.imageio

  {:doc     "Image utilities related to javax.imageio."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-10"}
  (:refer-clojure :exclude [read reduce])
  (:require [clojure.java.io :as io]
            [mop.image.util :as image])
  (:import
   [it.geosolutions.imageio.plugins.tiff BaselineTIFFTagSet TIFFField]
   [it.geosolutions.imageioimpl.plugins.tiff TIFFImageMetadata]
   [java.awt Graphics2D RenderingHints]
   [java.awt.image BufferedImage RenderedImage]
   [javax.imageio IIOImage ImageIO ImageReader ImageTypeSpecifier]))
;;----------------------------------------------------------------------
(defmethod image/equals? [IIOImage IIOImage] [^IIOImage a ^IIOImage b]
  ;; TODO: compare metadata, etc.
  (image/equals? (.getRenderedImage a) (.getRenderedImage b)))

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

(defn set-tiff-predictor [^TIFFImageMetadata metadata ^long predictor]
  (let [base (BaselineTIFFTagSet/getInstance)
        field (TIFFField. (.getTag base BaselineTIFFTagSet/TAG_PREDICTOR) (int predictor))
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

(defn set-tiff-compression [^TIFFImageMetadata metadata ^long compression]
  (let [base (BaselineTIFFTagSet/getInstance)
        field (TIFFField. (.getTag base BaselineTIFFTagSet/TAG_COMPRESSION) compression)
        root (.getRootIFD metadata)]
    (.addTIFFField root field)))

;;----------------------------------------------------------------------
;; TODO: handle multi-image, multi-thumbnail cases?
;; TODO: what if there is more than one reader?
;; TODO: custom ImageReadParams?
;; TODO: can we .close the input stream if the reader is returned?
(defn ^[ImageReader IIOImage] read [input]
  (with-open [iis (ImageIO/createImageInputStream (io/file input))]
    (let [^ImageReader reader (first (iterator-seq (ImageIO/getImageReaders iis)))
          params (.getDefaultReadParam reader)]
      (.setInput reader iis)
      [reader (.readAll reader 0 params)])))
;;----------------------------------------------------------------------
;; TODO: do we need to return writer, write-param?
;; TODO: dispose of writer, other cleanup...
(defn write [^ImageReader reader ^IIOImage image output]
  (let [output (io/file output)
        writer (ImageIO/getImageWriter reader)
        write-param (.getDefaultWriteParam writer);
        metadata (.getMetadata image)]
    ;; As of 2025-12-10, imageio-ext 2.0.1 can't write ZLIB compression?
    ;; use LZW as a work-around
    ;; seems to force PREDICTOR_NONE with LZW, creating approx 2x larger files than ZLIB input
    ;; TODO: more experiments to find better compression
    ;; TODO: keep checking whether imageio-ext is really necessary
    (when (and (instance? TIFFImageMetadata metadata)
               (= BaselineTIFFTagSet/COMPRESSION_ZLIB (tiff-compression metadata)))
      (set-tiff-compression metadata BaselineTIFFTagSet/COMPRESSION_LZW)
      ;(set-tiff-predictor metadata predictor)
      ;(debug/echo (tiff-predictor metadata))
      ;(debug/echo (.getCompressionType compressor))
      ;(.setCompressionMode ^TIFFImageWriteParam write-param ImageWriteParam/MODE_EXPLICIT);
      ;(.setTIFFCompressor ^TIFFImageWriteParam write-param compressor)
      ;(.setCompressionType ^TIFFImageWriteParam write-param (.getCompressionType compressor))
      )
    (try
      (with-open [ios (ImageIO/createImageOutputStream output)]
        (.setOutput writer ios)
        (.write writer metadata image write-param))
      (finally
        (.dispose writer)))))
;;----------------------------------------------------------------------
;; see https://web.archive.org/web/20070515094604/https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
;; need to use ImageTypeSpecifier to handle input images with TYPE_CUSTOM

(defn ^RenderedImage resize-rendered-image

  ([^RenderedImage image ^long w ^long h ^Object hint]
   (println w h)
   (let [type-specifier (ImageTypeSpecifier/createFromRenderedImage image)
         ^BufferedImage resized (.createBufferedImage type-specifier w h)
         ^Graphics2D g2 (.createGraphics resized)]
     (println type-specifier)
     (.setRenderingHint g2 RenderingHints/KEY_INTERPOLATION hint)
     (.drawImage g2 image 0 0 w h nil)
     #_(.dispose g2)
     resized))

  ([^RenderedImage image ^long w ^long h]
   (resize-rendered-image image w h RenderingHints/VALUE_INTERPOLATION_BILINEAR)))

;;----------------------------------------------------------------------
(defn ^RenderedImage reduce-rendered-image

  "Return an image that has no dimension larger that max-dimension.
  If small enough, return the input."

  ([^RenderedImage image ^long max-dimension ^Object hint]
   (let [w (.getWidth image)
         h (.getHeight image)
         s (/ (double max-dimension) (Math/max w h))
         sw (long (* s w))
         sh (long (* s h))]
     (if (or (< sw w) (< sh h))
       (resize-rendered-image image sw sh hint)
       image)))

  ([^RenderedImage image ^long max-dimension]
   (reduce-rendered-image image max-dimension RenderingHints/VALUE_INTERPOLATION_BILINEAR)))
;;----------------------------------------------------------------------
(defn ^IIOImage resize-iioimage

  ([^IIOImage image ^long w ^long h ^Object hint]
   (IIOImage. (resize-rendered-image (.getRenderedImage image) w h hint)
              (.getThumbnails image) (.getMetadata image)))

  ([^IIOImage image ^long w ^long h]
   (resize-iioimage image w h RenderingHints/VALUE_INTERPOLATION_BILINEAR)))
;;----------------------------------------------------------------------
(defn ^IIOImage reduce-iioimage

  "Return an image that has no dimension larger that max-dimension.
  If small enough, return the input."

  (^IIOImage [^IIOImage image ^long max-dimension ^Object hint]
   (let [rendered (.getRenderedImage image)
         w (.getWidth rendered)
         h (.getHeight rendered)
         s (/ (double max-dimension) (Math/max w h))
         sw (long (* s w))
         sh (long (* s h))]
     (if (or (< sw w) (< sh h))
       (resize-iioimage image sw sh hint)
       image)))


  (^IIOImage [^IIOImage image ^long max-dimension]
   (reduce-iioimage image max-dimension RenderingHints/VALUE_INTERPOLATION_BILINEAR)))
;;----------------------------------------------------------------------
