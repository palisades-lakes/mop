(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\f32tiff.clj
;;----------------------------------------------------------------
(ns mop.image.f32tiff
  {:doc
   "Read and write 32bit Float grayscale TIFFs."
   :author  "palisades dot lakes at gmail dot com"
   :version "2025-11-25"}

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
   [java.util List]
   [org.apache.commons.imaging FormatCompliance]
   [org.apache.commons.imaging.bytesource ByteSource]
   [org.apache.commons.imaging.formats.tiff
    AbstractTiffElement$DataElement AbstractTiffImageData AbstractTiffImageData$Data AbstractTiffImageData$Strips AbstractTiffRasterData TiffContents TiffDirectory TiffImagingParameters TiffReader]
   [org.apache.commons.imaging.formats.tiff.constants TiffTagConstants]
   [org.apache.commons.imaging.formats.tiff.photometricinterpreters.floatingpoint
    PaletteEntryForRange PaletteEntryForValue PhotometricInterpreterFloat]
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
        nColsOfBlocks  (quot (+ width nColsInBlock -1) nColsInBlock)
        nRowsOfBlocks (quot (+ height nRowsInBlock 1) nRowsInBlock)
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
;; NOTE: Tile format not supported by commons imaging as of 20o25-11-25
(defn- writeFileF32 [^floats f width height nRowsInBlock nColsInBlock
                     ^ByteOrder byteOrder
                     ^File outputFile]
  (let [^objects blocks (getBytesForOutput32 f width height nRowsInBlock nColsInBlock byteOrder)
        nBytesInBlock (* (int nRowsInBlock) (int nColsInBlock) (int 4))
        ^TiffOutputSet outputSet (TiffOutputSet. byteOrder)
        ^TiffOutputDirectory outDir (.addRootDirectory outputSet)
        ^objects imageData (make-array AbstractTiffElement$DataElement (alength blocks))
        ^AbstractTiffImageData abstractTiffImageData (AbstractTiffImageData$Strips. imageData nRowsInBlock)]
    (.add outDir TiffTagConstants/TIFF_TAG_IMAGE_WIDTH (int-array 1 width))
    (.add outDir TiffTagConstants/TIFF_TAG_IMAGE_LENGTH (int-array 1  height))
    (.add outDir TiffTagConstants/TIFF_TAG_SAMPLE_FORMAT (short-array 1 (short TiffTagConstants/SAMPLE_FORMAT_VALUE_IEEE_FLOATING_POINT)))
    (.add outDir TiffTagConstants/TIFF_TAG_SAMPLES_PER_PIXEL (short 1))
    (.add outDir TiffTagConstants/TIFF_TAG_BITS_PER_SAMPLE (short-array 1 (short 32)))
    (.add outDir TiffTagConstants/TIFF_TAG_PHOTOMETRIC_INTERPRETATION (short TiffTagConstants/PHOTOMETRIC_INTERPRETATION_VALUE_BLACK_IS_ZERO))
    (.add outDir TiffTagConstants/TIFF_TAG_COMPRESSION (short TiffTagConstants/COMPRESSION_VALUE_UNCOMPRESSED))
    (.add outDir TiffTagConstants/TIFF_TAG_PLANAR_CONFIGURATION (short TiffTagConstants/PLANAR_CONFIGURATION_VALUE_CHUNKY))
    (.add outDir TiffTagConstants/TIFF_TAG_ROWS_PER_STRIP (int-array 1 2))
    (.add outDir TiffTagConstants/TIFF_TAG_STRIP_BYTE_COUNTS (int-array 1 nBytesInBlock))
    (dotimes [i (alength blocks)]
      (let [^bytes block-i (aget blocks i)]
        (aset imageData i (AbstractTiffImageData$Data. 0 (alength block-i) block-i))))
    (.setTiffImageData outDir abstractTiffImageData)
    (with-open [os (BufferedOutputStream. (io/output-stream outputFile))]
      ;;TODO: lossless!
      (let [^TiffImageWriterLossy writer (TiffImageWriterLossy. byteOrder)]
        (.write writer os outputSet)))))
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
    {:photometricInterpreter pInterp :byteOrder byteOrder} ))

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
(defn tiff-fp-read-write [input ^double pmin ^double pmax ^double pmissing]
  (let [input (io/file input)
        output (mci/append-to-filename input "-imaging")
        {:keys [^PhotometricInterpreterFloat _photometricInterpreter
                ^ByteOrder byteOrder]} (readAndInterpretTIFF input pmin pmax pmissing)
        ;minVal (.getMinFound photometricInterpreter)
        ;maxVal (.getMaxFound photometricInterpreter)
        ^AbstractTiffRasterData fullRaster (readRasterFromTIFF input (ming/tiff-imaging-parameters))
        w (.getWidth fullRaster)
        h (.getHeight fullRaster)
        nRowsInBlock (int 1)
        nColsInBlock w]
    (println)
    (debug/echo input)
    ;(debug/echo minVal maxVal)
    ;(assert (<= pmin minVal maxVal pmax))
    (debug/echo (.getWidth fullRaster) (.getHeight fullRaster))
    (pp/pprint fullRaster)
    (image/write-metadata-markdown input)
    (writeFileF32 (.getData fullRaster) w h nRowsInBlock nColsInBlock byteOrder output)))
;;-------------------------------------------------------------

#_(tiff-fp-read-write "images/usgs/USGS_13_n38w077_dir5.tiff" -2.0 62.0 -99999.0)
;; TODO: what's the real range for this file?
(tiff-fp-read-write "images/moon/ldem_4.tif" -20.0 20.0 9999.0)
