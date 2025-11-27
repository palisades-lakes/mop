(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\f32tiff.clj
;;----------------------------------------------------------------
(ns mop.image.f32tiff
  {:doc
   "Read and write 32bit Float grayscale TIFFs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-27"}

  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci]
   [mop.image.imaging :as ming]
   [mop.image.util :as image])
  (:import
   [java.awt Color]
   [java.awt.image BufferedImage]
   [java.io BufferedOutputStream File]
   [java.nio ByteOrder]
   [java.util Arrays]
   [org.apache.commons.imaging FormatCompliance Imaging]
   [org.apache.commons.imaging.bytesource ByteSource]
   [org.apache.commons.imaging.formats.tiff
    AbstractTiffElement$DataElement AbstractTiffImageData AbstractTiffImageData$Data AbstractTiffImageData$Strips
    AbstractTiffRasterData TiffContents TiffDirectory TiffImageMetadata TiffImagingParameters TiffReader]
   [org.apache.commons.imaging.formats.tiff.constants TiffTagConstants]
   [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
    PaletteEntryForRange PaletteEntryForValue PhotometricInterpreterFloat]
   [org.apache.commons.imaging.formats.tiff.taginfos TagInfo]
   [org.apache.commons.imaging.formats.tiff.write TiffImageWriterLossy TiffOutputDirectory TiffOutputSet]))
;;-------------------------------------------------------------
;; see
;; https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/formats/tiff/TiffFloatingPointRoundTripTest.java
;;
;; (.getName (class (make-array Byte/TYPE 0 0))) -> "[[B"
;; ^{:tag (class (make-array Byte/TYPE 0 0))}
;; ^"[[B"

(defn- getBytesForOutput32  [^floats f width height nRowsInBlock nColsInBlock
                             ^ByteOrder byteOrder]
  "Gets the bytes for output for a 32 bit floating point format.
  Note that this method operates over 'blocks' of data which may represent either TIFF Strips
  or Tiles. When processing strips, there is always one column of blocks
  and each strip is exactly the full width of the image. When processing tiles,
  there may be one or more columns of blocks and the block coverage may extend beyond
  both the last row and last column.

  @param f            an array of the grid of output values in row major order
  @param width        the width of the overall image
  @param height       the height of the overall image
  @param nRowsInBlock the number of rows in the Strip or Tile
  @param nColsInBlock the number of columns in the Strip or Tile
  @param byteOrder    little-endian or big-endian
  "
  (let [width (int width)
        height (int height)
        nRowsInBlock (int nRowsInBlock)
        nColsInBlock (int nColsInBlock)
        nColsOfBlocks (quot (+ width nColsInBlock -1) nColsInBlock)
        nRowsOfBlocks (quot (+ height nRowsInBlock -1) nRowsInBlock)
        bytesPerPixel 4
        nBlocks (* nRowsOfBlocks nColsOfBlocks)
        nBytesInBlock (* bytesPerPixel nRowsInBlock nColsInBlock)
        ^"[[B" blocks (make-array Byte/TYPE nBlocks nBytesInBlock)]
    (debug/echo nRowsInBlock nColsInBlock nColsOfBlocks nRowsOfBlocks nBlocks)
    (dotimes [i height]
      (let [blockRow (quot i nRowsInBlock)
            rowInBlock (- i (* blockRow nRowsInBlock))
            blockOffset (* rowInBlock nColsInBlock)]
        (dotimes [j width]
          (let [sample (Float/floatToRawIntBits (aget f (+ (* i width) j)))
                blockCol (quot j nColsInBlock)
                colInBlock (- j (* blockCol nColsInBlock))
                index (+ blockOffset colInBlock)
                offset (* index bytesPerPixel)
                ^bytes b (aget blocks (+ (* blockRow nColsOfBlocks) blockCol))]
            (if (= byteOrder ByteOrder/LITTLE_ENDIAN)
              (do
                (aset-byte b    offset    (byte (bit-and                           sample     0xff)))
                (aset-byte b (+ offset 1) (byte (bit-and (unsigned-bit-shift-right sample  8) 0xff)))
                (aset-byte b (+ offset 2) (byte (bit-and (unsigned-bit-shift-right sample 16) 0xff)))
                (aset-byte b (+ offset 3) (byte (bit-and (unsigned-bit-shift-right sample 24) 0xff))))
              ;; else
              (do
                (aset-byte b    offset    (byte (bit-and (unsigned-bit-shift-right sample 24) 0xff)))
                (aset-byte b (+ offset 1) (byte (bit-and (unsigned-bit-shift-right sample 16) 0xff)))
                (aset-byte b (+ offset 2) (byte (bit-and (unsigned-bit-shift-right sample  8) 0xff)))
                (aset-byte b (+ offset 3) (byte (bit-and                           sample     0xff)))))))))
    blocks))

;;-------------------------------------------------------------
(defn- set-tag [^TiffOutputDirectory dir
                ^TagInfo tag
                value]
  (debug/echo value)
  (.removeField dir tag)
  (.add dir tag value))

;;-------------------------------------------------------------
;; NOTE: Tile format not supported by commons imaging as of 2025-11-25
;; TODO: get nRowsInBLock nColsInBlock from input file, when not resizing

(defn- writeTiffF32 [^floats f width height nRowsInBlock nColsInBlock
                     ^ByteOrder byteOrder
                     ^TiffImageMetadata metadata
                     ^File outputFile]
  (debug/echo (class metadata) metadata)
  (debug/echo (.getDirectories metadata))
  (debug/echo (.getItems metadata))
  (let [^objects blocks (getBytesForOutput32 f width height nRowsInBlock nColsInBlock byteOrder)
        nBytesInBlock (* (int nRowsInBlock) (int nColsInBlock) (int 4))
        ^TiffOutputSet outputSet (.getOutputSet metadata)
        ^TiffOutputDirectory outDir (.getOrCreateRootDirectory outputSet)
        ^objects imageData (make-array AbstractTiffElement$DataElement (alength blocks))
        ^AbstractTiffImageData abstractTiffImageData (AbstractTiffImageData$Strips. imageData nRowsInBlock)]
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_IMAGE_WIDTH (int-array 1 width))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_IMAGE_LENGTH (int-array 1  height))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_SAMPLE_FORMAT (short-array 1 (short TiffTagConstants/SAMPLE_FORMAT_VALUE_IEEE_FLOATING_POINT)))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_SAMPLES_PER_PIXEL (short 1))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_BITS_PER_SAMPLE (short-array 1 (short 32)))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_PHOTOMETRIC_INTERPRETATION (short TiffTagConstants/PHOTOMETRIC_INTERPRETATION_VALUE_BLACK_IS_ZERO))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_COMPRESSION (short TiffTagConstants/COMPRESSION_VALUE_UNCOMPRESSED))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_PLANAR_CONFIGURATION (short TiffTagConstants/PLANAR_CONFIGURATION_VALUE_CHUNKY))
    ;(set-tag outDir TiffTagConstants/TIFF_TAG_ROWS_PER_STRIP (int-array 1 1))
    (set-tag outDir TiffTagConstants/TIFF_TAG_STRIP_BYTE_COUNTS (int-array 1 nBytesInBlock))

    (set-tag outDir TiffTagConstants/TIFF_TAG_DOCUMENT_NAME (into-array String [(str outputFile)]))

    (debug/echo (alength blocks))
    (dotimes [i (alength blocks)]
      (let [^bytes block-i (aget blocks i)]
        (aset imageData i (AbstractTiffImageData$Data. 0 (alength block-i) block-i))))
    (.setTiffImageData outDir abstractTiffImageData)
    (with-open [os (BufferedOutputStream. (io/output-stream outputFile))]
      ;;TODO: lossless!?
      (let [^TiffImageWriterLossy writer (TiffImageWriterLossy. byteOrder)]
        (.write writer os outputSet)))))
;;-------------------------------------------------------------
(defn-  ^TiffDirectory directory-with-floating-point-raster [^TiffContents contents]
  (let [ ^TiffDirectory directory (.get (.directories contents) 0)]
    (assert (.hasTiffFloatingPointRasterData directory))
    directory))
;;-------------------------------------------------------------
(defn- ^PhotometricInterpreterFloat photometric-interpreter-grayscale-f32
  ([^double min ^double max ^double missing]
   (PhotometricInterpreterFloat.
    [(PaletteEntryForValue. (float missing) Color/red)
     (PaletteEntryForRange. (float min) (float max) Color/black Color/white)]))
  ([]
   ;; TODO: will infinities cause problems later?
   (photometric-interpreter-grayscale-f32
    Double/NEGATIVE_INFINITY Double/POSITIVE_INFINITY Double/NaN)))
;;-------------------------------------------------------------
(defn- imaging-parameters-grayscale-f32
  ([^PhotometricInterpreterFloat pi] (ming/tiff-imaging-parameters pi))
  ([] (imaging-parameters-grayscale-f32 (photometric-interpreter-grayscale-f32))))
;;-------------------------------------------------------------
;; see
;; https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/formats/tiff/TiffFloatingPointReadTest.java

(defn- ^PhotometricInterpreterFloat readTiffF32 [^File target]
  (let [^ByteSource byteSource (ByteSource/file target)
        ^TiffReader tiffReader (TiffReader. true)
        ^TiffContents contents (.readDirectories tiffReader byteSource true (FormatCompliance/getDefault))
        ^ByteOrder byteOrder (.getByteOrder tiffReader)
        ^TiffDirectory directory (directory-with-floating-point-raster contents)
        ^PhotometricInterpreterFloat pi (photometric-interpreter-grayscale-f32)
        ^TiffImagingParameters params (imaging-parameters-grayscale-f32 pi)
        ;; reading the image data modifies params, pi !!!
        ^BufferedImage _image (.getTiffImage directory byteOrder params)
        ]
    (assert (not (nil? _image)))
    ;;TODO: best way to return all data, metadata from tiff?
    {:byteOrder byteOrder
     :raster (.getRasterData directory params)
     :metadata (Imaging/getMetadata target) }  ))

;;-------------------------------------------------------------
(defn read-write-TiffF32 [input]
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {:keys [^AbstractTiffRasterData raster
                ^ByteOrder byteOrder
                ^TiffImageMetadata metadata]} (readTiffF32 input)
        w (.getWidth raster)
        h (.getHeight raster)
        nRowsInBlock (int 1)
        nColsInBlock w]
    (println)
    (debug/echo input)
    (debug/echo (.getWidth raster) (.getHeight raster))
    (pp/pprint raster)
    (image/write-metadata-markdown input)
    (writeTiffF32 (.getData raster) w h nRowsInBlock nColsInBlock byteOrder metadata output)
    (image/write-metadata-markdown output)))
;;-------------------------------------------------------------
(defn read-write-read-TiffF32 [input]
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {^AbstractTiffRasterData raster-in :raster
         ^ByteOrder byteOrder-in :byteOrder
         ^TiffImageMetadata metadata-in :metadata} (readTiffF32 input)
        w (.getWidth raster-in)
        h (.getHeight raster-in)
        nRowsInBlock (int 1)
        nColsInBlock w
        ^floats pixels-in (.getData raster-in)]
    (writeTiffF32 pixels-in w h nRowsInBlock nColsInBlock byteOrder-in metadata-in output)
    (let [{^AbstractTiffRasterData raster-out :raster
           ^ByteOrder byteOrder-out :byteOrder
           ^TiffImageMetadata _metadata-out :metadata}
          (readTiffF32 output)
          ^floats pixels-out (.getData raster-out) ]
      (assert (= byteOrder-in byteOrder-out))
      (assert (Arrays/equals pixels-in pixels-out))
      ;; TODO: test metadata consistency
      )))
;;-------------------------------------------------------------

#_(read-write-TiffF32 "images/usgs/USGS_13_n38w077_dir5.tiff")
;; TODO: what's the real range for this file?
(read-write-read-TiffF32 "images/moon/ldem_4.tif")
#_(read-write-TiffF32 "images/moon/ldem_4.tif")
;;(tiff-fp-read-write "images/earth/ETOPO_2022_v1_60s_N90W180_geoid.tif")
