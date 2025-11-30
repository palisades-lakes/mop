
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.image.util

  {:doc     "Image utilities."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-29"}

  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [mop.commons.io :as mci]
            [mop.commons.string :as mcs])
  (:import [com.drew.imaging ImageMetadataReader]
           [com.drew.metadata Directory Metadata Tag]
           [com.drew.metadata.exif ExifIFD0Directory]
           [com.drew.metadata.xmp XmpDirectory]
           [java.awt Image]
           [java.awt.image BufferedImage DataBufferByte RenderedImage]
           [java.io File]
           [java.nio ByteBuffer FloatBuffer IntBuffer]
           [java.util Map$Entry]
           [javax.imageio ImageIO]
           [org.apache.commons.imaging Imaging]
           [org.lwjgl BufferUtils]))

;;----------------------------------------------------------------
(def ^:private image-file-type?
  #{"3fr"                                                   ;; Hasselblad
    #_"3g2"
    #_"3gp"
    "ari"                                                   ;; Arri Alexa
    "arw" "srf" "sr2"                                       ;; Sony
    "bay"                                                   ;; Casio
    "bmp"
    "cri"                                                   ;; Cintel
    "crw" "cr2"                                             ;; Canon
    "cap" "iiq" "eip"                                       ;; Phase One
    "dcs" "dcr" "drf" "k25" "kdc"                           ;; Kodak
    "dng"                                                   ;; Adobe, Leica
    "erf"                                                   ;; Epson
    "fff"                                                   ;; Imacon/Hasselblad raw
    #_"gif"
    "ico"
    "jpeg" "jpg"
    "m4v"
    "mef"                                                   ;; Mamiya
    "mdc"                                                   ;; Minolta, Agfa
    "mos"                                                   ;; Leaf
    #_"mov"
    #_"mp4"
    "mrw"                                                   ;; Minolta, Konica Minolta
    "nef" "nrw"                                             ;; Nikon
    "orf"                                                   ;; Olympus
    "pcx"
    "pef" "ptx"                                             ;; Pentax
    "png"
    "psd"
    "pxn"                                                   ;; Logitech
    "r3d"                                                   ;; RED Digital Cinema
    "raf"                                                   ;; Fuji
    "raw"                                                   ;; Panasonic, Leica
    "rw2"                                                   ;; Panasonic
    "rwl"                                                   ;; Leica
    "rwz"                                                   ;; Rawzor
    "srw"                                                   ;; Samsung
    "tif" "tiff"
    "webp"
    "x3f"                                                   ;; Sigma
    })
;;-------------------------------------------------------------
(defn image-file? [^File f]
  ;; filter out some odd hidden files in recycle bins, etc.
  (and (not (s/starts-with? (.getName f) "$"))
       (image-file-type? (mci/extension f))))
;;----------------------------------------------------------------
(defn image-file-seq

  "Return a <code>seq</code> of all the files, in any folder under
   <code>d</code>, that are accepted by
   <code>image-file?</code>., which at present is just a set of
   known image file endings."

  [^File d]

  (assert (.exists d) (.getPath d))
  (filter image-file? (file-seq d)))
;;-------------------------------------------------------------
;; metadata
;;-------------------------------------------------------------

(defn ^Metadata read-metadata [source]
  (ImageMetadataReader/readMetadata (io/file source)))

;;-------------------------------------------------------------

(defn write-metadata-markdown
  ([source ^Metadata metadata destination]
   ;; source might be a File or a String
   (let [^File source-file (io/file source)
         ^String fileName (.getName source-file)
         ^String urlName (.toURL (.toURI (.getCanonicalFile source-file)))
         ^ExifIFD0Directory exifIFD0Directory (.getFirstDirectoryOfType metadata ExifIFD0Directory)
         ^String make (if (nil? exifIFD0Directory) "" (.getString exifIFD0Directory ExifIFD0Directory/TAG_MAKE))
         ^String model (if (nil? exifIFD0Directory) "" (.getString exifIFD0Directory ExifIFD0Directory/TAG_MODEL))]
     (with-open [w (io/writer destination)]
       (binding [*out* w]
         (println )
         (println "---")
         (println )
         (printf "# %s - %s%n" make model)
         (println )
         (printf "<a href=\"%s\">%n", urlName)
         (printf "<img src=\"%s\" width=\"300\"/><br/>%n", urlName)
         (println fileName)
         (println "</a>")
         (println )
         (println "|Directory | Tag Id | Tag Name | Extracted Value|")
         (println "|:--------:|-------:|----------|----------------|")
         (doseq [^Directory directory (.getDirectories metadata)]
           (let [^String directoryName  (.getName directory)]
             (doseq [^Tag tag (.getTags directory)]
               (let [^String tagName (.getTagName tag)
                     ^String description (mcs/truncate-string (.getDescription tag))]
                 (printf "|%s|0x%s|%s|%s|%n" directoryName (Integer/toHexString (.getTagType tag))
                         tagName description)
                 (if (instance? XmpDirectory directory)
                   (doseq [^Map$Entry property (.entrySet (.getXmpProperties ^XmpDirectory directory))]
                     (let [^String key (.getKey property)
                           ^String value (mcs/truncate-string (.getValue property))]
                       (printf "|%s||%s|%s|%n" directoryName key value)))))))
           (doseq [^String error (.getErrors directory)] (println "ERROR: " error)))))))

  ([source]
   (let [metadata (read-metadata source)
         file (io/file source)
         folder (.getParentFile file)
         filename (mci/prefix file)]
     (write-metadata-markdown source metadata (io/file folder (str filename ".md"))))))

;;-------------------------------------------------------------
;; TODO: some way to avoid creating intermediate image?

(defn ^BufferedImage to-buffered-image [^Image image ^long image-type]
  (if (instance? BufferedImage image)
    image
    ;; else
    (let [b (BufferedImage. (.getWidth image nil) (.getHeight image nil) image-type)
          g (.createGraphics b)]
      (.drawImage g image 0 0 nil)
      (.dispose g)
      b)))

;;---------------------------------------------------------------

(defn- download [url target]
  (with-open [in (io/input-stream url)
              out (io/output-stream target)]
    (print "Downloading" target "... ")
    (flush)
    (io/copy in out)
    (println "done")))

(defn ^BufferedImage get-image

  ([path] (ImageIO/read (io/file path)))

  #_([path] (Imaging/getBufferedImage (io/file path)))

  ([path remote-url]
   (when (not (.exists (io/file path)))
     (download remote-url path))
   (get-image path)))

;;-----------------------------------------------------------------

(defn- buffered-image-type [^BufferedImage image]
  ;; BufferedImage constructor can't handle TYPE_CUSTOM=0
  (let [type (.getType image)]
    ;; HACKKKKKKKKKKKKKKKKKKKKKKKKK!!!
    (if (== BufferedImage/TYPE_CUSTOM)
      BufferedImage/TYPE_BYTE_GRAY
      ;;else
      type)))

#_(defn ^BufferedImage resize
  ([^BufferedImage image ^long w ^long h ^long hints]
   (let [^RenderedImage resized (.getScaledInstance image w h hints)]
     (to-buffered-image resized (buffered-image-type image))))
  ([^BufferedImage image ^long w ^long h]
   (resize image w h Image/SCALE_DEFAULT)))

#_(defn ^Boolean write-png [^BufferedImage image ^File file ]
  (ImageIO/write image "png" file))

#_(defn ^Boolean write-tif [^BufferedImage image ^File file]
  (ImageIO/write image "tif" file))

;;-------------------------------------------------------------------

(defn ^FloatBuffer float-buffer [^floats data]
  (let [buffer (BufferUtils/createFloatBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn ^IntBuffer int-buffer [^ints data]
  (let [buffer (BufferUtils/createIntBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

(defn ^ByteBuffer byte-buffer [^bytes data]
  (let [buffer (BufferUtils/createByteBuffer (count data))]
    (.put buffer data)
    (.flip buffer)
    buffer))

;;-------------------------------------------------------------------

(defn pixels-as-bytes
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         pixels (.getData ^DataBufferByte (.getDataBuffer (.getRaster image)))]
     [(byte-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-bytes (get-image local-path remote-url))))

(defn pixels-as-ints
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         d (.getNumComponents (.getColorModel image))
         pixels (int-array (* w h d))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [(int-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-ints (get-image local-path remote-url))))

;; TODO: assuming only one band?
(defn pixels-as-floats
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         pixels (float-array (* w h))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [(float-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-floats (get-image local-path remote-url))))