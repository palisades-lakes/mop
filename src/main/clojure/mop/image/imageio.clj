(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;---------------------------------------------------------------------
(ns mop.image.imageio

  {:doc     "Image utilities related to javax.imageio."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-05"}
  (:refer-clojure :exclude [read reduce])
  (:require [clojure.java.io :as io]
            [mop.commons.debug :as debug])
  (:import
   [java.awt Graphics2D RenderingHints]
   [java.awt.image BufferedImage RenderedImage]
   [javax.imageio IIOImage ImageIO ImageReadParam ImageReader ImageTypeSpecifier ImageWriteParam ImageWriter]
   [javax.imageio.metadata IIOMetadata]
   [javax.imageio.plugins.tiff TIFFImageReadParam]
   [javax.imageio.stream ImageInputStream ImageOutputStream]))
;;----------------------------------------------------------------------
;; TODO: handle multi-image, multi-thumbnail cases?
;; TODO: what if there is more than one reader?
;; TODO: custom ImageReadParams?

(defn ^[ImageReader IIOImage] read [input]
  (let [^ImageInputStream iis (ImageIO/createImageInputStream (io/file input))
        ^ImageReader reader (first (iterator-seq (ImageIO/getImageReaders iis)))
        ^ImageReadParam params (.getDefaultReadParam reader)]
    (.setInput reader iis)

    #_(when (instance? TIFFImageReadParam params)
      (debug/echo (.getAllowedTagSets ^TIFFImageReadParam params))
      (.setReadUnknownTags ^TIFFImageReadParam params true))
    [reader (.readAll reader 0 params)]))
;;----------------------------------------------------------------------
(defn ^[ImageWriter ImageWriteParam] write [^ImageReader reader ^IIOImage image output]
  (let [^ImageWriter writer (ImageIO/getImageWriter reader)
        ^ImageWriteParam write-param (.getDefaultWriteParam writer);
        ^IIOMetadata metadata (.getMetadata image)
        ^ImageOutputStream ios (ImageIO/createImageOutputStream (io/file output))]
    (.setOutput writer ios)
    (.write writer metadata image write-param)
    [writer write-param]))
;;----------------------------------------------------------------------
(defn ^[IIOImage ImageWriter ImageWriteParam] read-write [input output]
  "Primarily for testing."
  (let [[reader image] (read input)
        [writer write-param] (write reader image output)]
    [image writer write-param]))
;;----------------------------------------------------------------------
;; see https://web.archive.org/web/20070515094604/https://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
;; need to use ImageTypeSpecifier to handle input images with TYPE_CUSTOM

(defn ^RenderedImage resize-rendered-image

  ([^RenderedImage image ^long w ^long h ^Object hint]
   (let [type-specifier (ImageTypeSpecifier/createFromRenderedImage image)
         ^BufferedImage resized (.createBufferedImage type-specifier w h)
         ^Graphics2D g2 (.createGraphics resized)]
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
