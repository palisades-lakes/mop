
(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.image.util

  {:doc     "Image utilities."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-07"}

  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [mop.commons.arrays :as mca]
   [mop.commons.io :as mci]
   [mop.commons.string :as mcs])
  (:import
   [clojure.lang IFn]
   [com.drew.imaging ImageMetadataReader]
   [com.drew.metadata Directory Metadata Tag]
   [com.drew.metadata.exif ExifIFD0Directory]
   [com.drew.metadata.xmp XmpDirectory]
   [java.awt Image]
   [java.awt.image
    BufferedImage DataBuffer DataBufferByte DataBufferDouble
    DataBufferFloat DataBufferInt DataBufferShort DataBufferUShort
    Raster RenderedImage SampleModel]
   [java.io File]
   [java.nio ByteBuffer FloatBuffer IntBuffer]
   [java.util Arrays Map$Entry]
   [javax.imageio ImageIO]
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
;; TODO: move to more general file type predicate
(defn image-file?
  ([^File f ^IFn ext-predicate]
   ;; filter out some odd hidden files in recycle bins, etc.
   (and (not (s/starts-with? (.getName f) "$"))
        (ext-predicate (mci/extension f))))
  ([^File f] (image-file? f image-file-type?)))
;;----------------------------------------------------------------
;; TODO: move to more general file-seq?
(defn image-file-seq

  "Return a <code>seq</code> of all the files, in any folder under
   <code>d</code>, that are accepted by
   <code>predicate</code>,
   which defaults to <code>image-file?</code>, which at present is just a Set of
   known image file endings."

  ([^File d ^IFn ext-predicate]
   (assert (.exists d) (.getPath d))
   (filter #(image-file? % ext-predicate) (file-seq d)))

  ([^File d] (image-file-seq d image-file-type?)))
;;---------------------------------------------------------------------
;; predicates and debugging
;;---------------------------------------------------------------------
;; TODO: defmulti? reflection?
(defn get-data [^DataBuffer a]
  (cond (instance? DataBufferByte a) (.getData ^DataBufferByte a)
        (instance? DataBufferFloat a) (.getData ^DataBufferFloat a)
        (instance? DataBufferInt a) (.getData ^DataBufferInt a)
        (instance? DataBufferShort a) (.getData ^DataBufferShort a)
        (instance? DataBufferUShort a) (.getData ^DataBufferUShort a)
        (instance? DataBufferDouble a) (.getData ^DataBufferDouble a)))
;;---------------------------------------------------------------------
;; TODO: move to more generic location?
(defmulti equals?
          "Custom equality predicate."
          (fn [a b] [(class a) (class b)]))

(defmethod equals? [Object Object] [a b] (identical? a b))
(defmethod equals? [mca/BooleanArray mca/BooleanArray] [^booleans a ^booleans b] (Arrays/equals a b))
(defmethod equals? [mca/ByteArray mca/ByteArray] [^bytes a ^bytes b] (Arrays/equals a b))
(defmethod equals? [mca/CharArray mca/CharArray] [^bytes a ^bytes b] (Arrays/equals a b))
(defmethod equals? [mca/ShortArray mca/ShortArray] [^shorts a ^shorts b] (Arrays/equals a b))
(defmethod equals? [mca/IntArray mca/IntArray] [^ints a ^ints b] (Arrays/equals a b))
(defmethod equals? [mca/LongArray mca/LongArray] [^longs a ^longs b] (Arrays/equals a b))
(defmethod equals? [mca/FloatArray mca/FloatArray] [^floats a ^floats b] (Arrays/equals a b))
(defmethod equals? [mca/DoubleArray mca/DoubleArray] [^doubles a ^doubles b] (Arrays/equals a b))

(defmethod equals? [DataBuffer DataBuffer] [^DataBuffer a ^DataBuffer b]
  (equals? (get-data a) (get-data b)))

(defmethod equals? [Raster Raster] [^Raster a ^Raster b]
  (and  (== (.getWidth a) (.getWidth b))
        (== (.getHeight a) (.getHeight b))
        (equals? (.getDataBuffer a) (.getDataBuffer b))))

(defmethod equals? [RenderedImage RenderedImage] [^RenderedImage a ^RenderedImage b]
  ;; TODO: compare more properties, SampleModel, tiles, etc.
  (and  (== (.getWidth a) (.getWidth b))
        (== (.getHeight a) (.getHeight b))
        (equals? (.getData a) (.getData b))))

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

  ([path remote-url]
   (when (not (.exists (io/file path)))
     (download remote-url path))
   (get-image path)))

;;-----------------------------------------------------------------

(defn ^String sample-model-type-name [^SampleModel sample-model]
  (let [type (int (.getDataType sample-model))]
    (cond
      (== type DataBuffer/TYPE_BYTE) "BYTE"
      (== type DataBuffer/TYPE_DOUBLE) "DOUBLE"
      (== type DataBuffer/TYPE_FLOAT) "FLOAT"
      (== type DataBuffer/TYPE_INT) "INT"
      (== type DataBuffer/TYPE_SHORT) "SHORT"
      (== type DataBuffer/TYPE_UNDEFINED) "UNDEFINED"
      (== type DataBuffer/TYPE_USHORT) "USHORT"
      :else (str type))))

(defn ^String raster-data-type-name [^Raster raster]
  (sample-model-type-name (.getSampleModel raster)))

;;-----------------------------------------------------------------

(defn ^String buffered-image-type-name [^BufferedImage image]
  (let [type (int (.getType image))]
    (cond
      (== type BufferedImage/TYPE_3BYTE_BGR) "3BYTE_BGR"
      (== type BufferedImage/TYPE_4BYTE_ABGR) "4BYTE_ABGR"
      (== type BufferedImage/TYPE_4BYTE_ABGR_PRE) "4BYTE_ABGR_PRE"
      (== type BufferedImage/TYPE_BYTE_BINARY) "BYTE_BINARY"
      (== type BufferedImage/TYPE_BYTE_GRAY) "BYTE_GRAY"
      (== type BufferedImage/TYPE_BYTE_INDEXED) "BYTE_INDEXED"
      (== type BufferedImage/TYPE_CUSTOM) "CUSTOM"
      (== type BufferedImage/TYPE_INT_ARGB) "INT_ARGB"
      (== type BufferedImage/TYPE_INT_ARGB_PRE) "INT_ARGB_PRE"
      (== type BufferedImage/TYPE_INT_BGR) "INT_BGR"
      (== type BufferedImage/TYPE_INT_RGB) "INT_RGB"
      (== type BufferedImage/TYPE_USHORT_555_RGB) "USHORT_555_RGB"
      (== type BufferedImage/TYPE_USHORT_565_RGB) "USHORT_565_RGB"
      (== type BufferedImage/TYPE_USHORT_GRAY) "USHORT_GRAY"
      :else (str type))))

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
     [pixels w h] )))

(defn pixels-as-byte-buffer
  ([^BufferedImage image]
   (let [[pixels w h] (pixels-as-bytes image)]
     [(byte-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-byte-buffer (get-image local-path remote-url))))

(defn pixels-as-int-buffer
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         d (.getNumComponents (.getColorModel image))
         pixels (int-array (* w h d))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [(int-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-int-buffer (get-image local-path remote-url))))

(defn pixels-as-floats
  ([^BufferedImage image]
   (let [w (.getWidth image)
         h (.getHeight image)
         pixels (float-array (* w h))]
     (.getPixels (.getRaster image) 0 0 w h pixels)
     [pixels w h] )))

;; TODO: assuming only one band?
(defn pixels-as-float-buffer
  ([^BufferedImage image]
   (let [[pixels w h] (pixels-as-floats image)]
     [(float-buffer pixels) w h] ))
  ([^String local-path ^String remote-url]
   (pixels-as-float-buffer (get-image local-path remote-url))))

