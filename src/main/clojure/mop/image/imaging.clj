(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.image.imaging

  {:doc     "Image utilities related to Apache Commons Imaging."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-12-02"}

  (:require [clojure.java.io :as io]
            [mop.commons.debug :as debug])
  (:import [java.awt Color]
           [java.awt.image BufferedImage]
           [java.io BufferedOutputStream File]
           [java.nio ByteOrder]
           [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util Map]
           [org.apache.commons.imaging FormatCompliance ImageFormat ImageInfo Imaging]
           [org.apache.commons.imaging.bytesource ByteSource]
           [org.apache.commons.imaging.common GenericImageMetadata GenericImageMetadata$GenericImageMetadataItem]
           [org.apache.commons.imaging.formats.tiff
            AbstractTiffElement$DataElement
            AbstractTiffImageData AbstractTiffImageData$Data AbstractTiffImageData$Strips
            TiffContents TiffDirectory TiffImageMetadata TiffImagingParameters TiffReader]
           [org.apache.commons.imaging.formats.tiff.constants TiffTagConstants]
           [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
            PaletteEntryForRange PaletteEntryForValue PhotometricInterpreterFloat]
           [org.apache.commons.imaging.formats.tiff.taginfos TagInfo]
           [org.apache.commons.imaging.formats.tiff.write TiffImageWriterLossy TiffOutputDirectory TiffOutputSet]))

;;-------------------------------------------------------------
;; TIFF
;;-------------------------------------------------------------
(defn tiff-imaging-parameters
  ([^PhotometricInterpreterFloat pi]
   (let [^TiffImagingParameters params (TiffImagingParameters.)]
     (.setCustomPhotometricInterpreter params pi)
     params))
  ([] (TiffImagingParameters.)))
;;-------------------------------------------------------------
(defn- ^TiffDirectory directory-with-floating-point-raster [^TiffContents contents]
  (let [ ^TiffDirectory directory (.get (.directories contents) 0)]
    (assert (.hasTiffFloatingPointRasterData directory))
    directory))
;;-------------------------------------------------------------
(defn ^PhotometricInterpreterFloat photometric-interpreter-grayscale-f32
  ([^double min ^double max ^double missing]
   (PhotometricInterpreterFloat.
    [(PaletteEntryForValue. (float missing) Color/red)
     (PaletteEntryForRange. (float min) (float max) Color/black Color/white)]))
  ([]
   ;; TODO: will infinities cause problems later?
   (photometric-interpreter-grayscale-f32
    -1.0 1.0 Double/NaN
    ;Double/NEGATIVE_INFINITY Double/POSITIVE_INFINITY Double/NaN
    )))
;;-------------------------------------------------------------
(defn imaging-parameters-grayscale-f32
  ([^PhotometricInterpreterFloat pi] (tiff-imaging-parameters pi))
  ([] (imaging-parameters-grayscale-f32 (photometric-interpreter-grayscale-f32))))
;;-------------------------------------------------------------
;; TODO: create an iterator or convert to HashMap?
;; TODO: Reflection to compare all fields, regardless of code changes?
;; TODO: move to test code?
(defn equal-ImageInfo? [^ImageInfo a ^ImageInfo b]
  (and
   (debug/equal-values? a b '.getFormatDetails)
   (debug/equal-values? a b '.getBitsPerPixel)
   (debug/equal-values? a b '.getComments)
   (debug/equal-values? a b '.getFormat)
   (debug/equal-values? a b '.getFormatName)
   (debug/equal-values? a b '.getHeight)
   (debug/equal-values? a b '.getMimeType)
   (debug/equal-values? a b '.getNumberOfImages)
   (debug/equal-values? a b '.getPhysicalHeightDpi)
   (debug/equal-values? a b '.getPhysicalHeightInch)
   (debug/equal-values? a b '.getPhysicalWidthDpi)
   (debug/equal-values? a b '.getPhysicalWidthInch)
   (debug/equal-values? a b '.getWidth)
   (debug/equal-values? a b '.getFormat)
   (debug/equal-values? a b '.isProgressive)
   (debug/equal-values? a b '.isTransparent)
   (debug/equal-values? a b '.usesPalette)
   (debug/equal-values? a b '.getColorType)
   (debug/equal-values? a b '.getCompressionAlgorithm)))
;;-------------------------------------------------------------
(defn- to-hash [^GenericImageMetadata meta]
  (into {} (map (fn [^GenericImageMetadata$GenericImageMetadataItem item]
                  [(.getKeyword item) (.getText item)])
                (.getItems meta))))
;;-------------------------------------------------------------
;; "PreviewImageStart" should be "StripOffsets", fixed in future commons imaging release
;; "StripOffsets" depends on the order (meta)data items are written to the file,
;; not consistent across libraries.
;; "DocumentName", "DateTime", etc., are expected to be different.
;; TODO: chose ignore fields depending on context, ie strips->strips vs tiles->strips vs tiles->tiles,
;; as well as other image formats...
(def strips-ignore #{
                     "Compression" "Predictor"
                     "DateTime"
                     "DocumentName"
                     "Software"
                     "PreviewImageLength" "PreviewImageStart"
                     "RowsPerStrip"
                     "StripByteCounts"
                     "StripOffsets"
                     ;; assuming write strips only
                     "TileByteCounts" "TileOffsets" "TileLength" "TileWidth"
                     })

(def tiles-ignore #{
                    ;;"Compression" "Predictor"
                    "DateTime"
                    "DocumentName"
                    "Software"
                    "PreviewImageLength" "PreviewImageStart"
                    "RowsPerStrip"
                    "StripByteCounts"
                    "StripOffsets"
                    })

;;-------------------------------------------------------------
(defn equal-ImageMetadata?
  ([^GenericImageMetadata a ^GenericImageMetadata b ignore]
   (let [^Map a (to-hash a)
         ^Map b (to-hash b)]
     (every? (fn [^String k]
               (let [val (or (ignore k) (= (get a k) (get b k)))]
                 (when-not val (println k " differs:" \newline (get a k) \newline (get b k)))
                 val))
             (into #{} (concat (keys a) (keys b))))))
  ([^GenericImageMetadata a ^GenericImageMetadata b]
   (equal-ImageMetadata? a b strips-ignore)))
;;-------------------------------------------------------------
;; see
;; https://github.com/apache/commons-blob/master/src/test/java/org/apache/commons/formats/tiff/TiffFloatingPointReadTest.java
;; TODO: best way to return all data, metadata from tiff?

(defn ^PhotometricInterpreterFloat read-tiff-f32 [^File target]
  (let [^ByteSource byteSource (ByteSource/file target)
        ^TiffReader tiffReader (TiffReader. true)
        ^TiffImagingParameters params (imaging-parameters-grayscale-f32)
        ^FormatCompliance compliance (FormatCompliance/getDefault)
        ^TiffContents contents (.readContents tiffReader byteSource params compliance)
        ^ByteOrder byteOrder (.getByteOrder tiffReader)
        ^TiffDirectory directory (directory-with-floating-point-raster contents)]
    {:byteOrder byteOrder
     :raster (.getRasterData directory params)
     :metadata (Imaging/getMetadata target)}))

;;-------------------------------------------------------------
;; see
;; https://github.com/apache/commons-blob/master/src/test/java/org/apache/commons/formats/tiff/TiffFloatingPointRoundTripTest.java
;;
;; (.getName (class (make-array Byte/TYPE 0 0))) -> "[[B"

(defn- output-bytes-f32-strips  [^floats f width height nRowsInBlock nColsInBlock
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
;; TODO: defmulti to get rid of reflection warning
(set! *warn-on-reflection* false)
(defn- set-tag [^TiffOutputDirectory dir
                ^TagInfo tag
                value]
  (.removeField dir tag)
  (.add dir tag value))
(set! *warn-on-reflection* true)
;;-------------------------------------------------------------
;; Whole image width strips, one pixel high

(defn write-tiff-f32-strips
  ([^floats f
    width height
    compression
    ^ByteOrder byteOrder
    ^TiffImageMetadata metadata
    ^File outputFile]
   (let [^objects blocks (output-bytes-f32-strips f width height 1 width byteOrder)
         nBytesInBlock (* (int width) (int 4))
         ^TiffOutputSet outputSet (.getOutputSet metadata)
         ^TiffOutputDirectory outDir (.getOrCreateRootDirectory outputSet)
         ^objects imageElements (make-array AbstractTiffElement$DataElement (alength blocks))
         ^AbstractTiffImageData imageData (AbstractTiffImageData$Strips. imageElements 1)]
     (dotimes [i (alength blocks)]
       (let [^bytes block-i (aget blocks i)]
         (aset imageElements i (AbstractTiffImageData$Data. 0 (alength block-i) block-i))))
     (.setTiffImageData outDir imageData)
     (set-tag outDir TiffTagConstants/TIFF_TAG_IMAGE_WIDTH (int-array 1 width))
     (set-tag outDir TiffTagConstants/TIFF_TAG_IMAGE_LENGTH (int-array 1  height))
     (.removeField outDir TiffTagConstants/TIFF_TAG_TILE_WIDTH)
     (.removeField outDir TiffTagConstants/TIFF_TAG_TILE_LENGTH)
     (.removeField outDir TiffTagConstants/TIFF_TAG_TILE_OFFSETS)
     (.removeField outDir TiffTagConstants/TIFF_TAG_TILE_BYTE_COUNTS)
     (when (= compression TiffTagConstants/COMPRESSION_VALUE_UNCOMPRESSED)
       (set-tag outDir TiffTagConstants/TIFF_TAG_COMPRESSION (short TiffTagConstants/COMPRESSION_VALUE_UNCOMPRESSED))
       (set-tag outDir TiffTagConstants/TIFF_TAG_PREDICTOR (short TiffTagConstants/PREDICTOR_VALUE_NONE)))
     (set-tag outDir TiffTagConstants/TIFF_TAG_ROWS_PER_STRIP (int-array 1 1))
     (set-tag outDir TiffTagConstants/TIFF_TAG_STRIP_BYTE_COUNTS (int-array 1 nBytesInBlock))
     (set-tag outDir TiffTagConstants/TIFF_TAG_SAMPLE_FORMAT (short-array 1 (short TiffTagConstants/SAMPLE_FORMAT_VALUE_IEEE_FLOATING_POINT)))
     (set-tag outDir TiffTagConstants/TIFF_TAG_SAMPLES_PER_PIXEL (short 1))
     (set-tag outDir TiffTagConstants/TIFF_TAG_BITS_PER_SAMPLE (short-array 1 (short 32)))
     (set-tag outDir TiffTagConstants/TIFF_TAG_PHOTOMETRIC_INTERPRETATION (short TiffTagConstants/PHOTOMETRIC_INTERPRETATION_VALUE_BLACK_IS_ZERO))
     (set-tag outDir TiffTagConstants/TIFF_TAG_PLANAR_CONFIGURATION (short TiffTagConstants/PLANAR_CONFIGURATION_VALUE_CHUNKY))
     ;; TODO: read maven group/artifact ids properties from jar file?
     (set-tag outDir TiffTagConstants/TIFF_TAG_SOFTWARE (into-array String ["palisades-lakes mop"]))
     (set-tag outDir TiffTagConstants/TIFF_TAG_DOCUMENT_NAME (into-array String [(str outputFile)]))
     (let [formatter (DateTimeFormatter/ofPattern "yyyy:MM:dd HH:mm:ss")]
       (set-tag outDir TiffTagConstants/TIFF_TAG_DATE_TIME
                (into-array String [(.format formatter (LocalDateTime/now))])))
     (debug/echo (.description outDir))
     (with-open [os (BufferedOutputStream. (io/output-stream outputFile))]
       ;;TODO: TiffImageWriterLossless!?
       (let [^TiffImageWriterLossy writer (TiffImageWriterLossy. byteOrder)]
         (.write writer os outputSet)))))

  ([^floats f width height ^ByteOrder byteOrder ^TiffImageMetadata metadata ^File outputFile]
   (write-tiff-f32-strips f width height TiffTagConstants/COMPRESSION_VALUE_UNCOMPRESSED
                          byteOrder metadata outputFile)))
;;-------------------------------------------------------------------

(defn ^ImageFormat guess-format [source]
  (Imaging/guessFormat (io/file source)))

(defn ^BufferedImage read-image [source]
  (Imaging/getBufferedImage (io/file source)))

(defn write-image [^BufferedImage image destination ^ImageFormat format]

  (Imaging/writeImage image (io/file destination) format))

;;-------------------------------------------------------------------
