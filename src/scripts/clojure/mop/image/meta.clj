(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\meta.clj
;;----------------------------------------------------------------
(ns mop.image.meta
  {:doc
   "Extract image metadata."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-11-23"}

  (:require
   [clojure.java.io :as io]
   [mop.commons.debug :as debug]
   [mop.commons.io :as mci])
  (:import [com.drew.imaging ImageMetadataReader]
           [com.drew.metadata Directory Metadata Tag]
           [com.drew.metadata.exif ExifIFD0Directory]
           [com.drew.metadata.xmp XmpDirectory]
           [java.io File]
           [java.util Map$Entry]))

;;-------------------------------------------------------------
(defn- ^String truncate
  ([^String s ^long maxlen]
   (if (and s (> (.length s) maxlen))
     (str (.substring s 0 maxlen) "...")
     s))
  ([^String s](truncate s 1024)))
;;-------------------------------------------------------------

(defn ^Metadata read-metadata [source]
  (ImageMetadataReader/readMetadata (io/file source)))

(defn write-metadata-markdown
  ([source ^Metadata metadata destination]
   (debug/echo destination)
   (let [^File source-file (io/file source)
         ^String fileName (.getName source-file)
         ;^String urlName (StringUtil/urlEncode source)
         ^ExifIFD0Directory exifIFD0Directory (.getFirstDirectoryOfType metadata ExifIFD0Directory)
         ^String make (if (nil? exifIFD0Directory) "" (.getString exifIFD0Directory ExifIFD0Directory/TAG_MAKE))
         ^String model (if (nil? exifIFD0Directory) "" (.getString exifIFD0Directory ExifIFD0Directory/TAG_MODEL))]
     (with-open [w (io/writer destination)]
       (binding [*out* w]
         ;(println )
         ;(println "---")
         ;(println )
         (printf "# %s - %s%n" make model)
         ;(println )
         ;(printf "<a href=\"https://raw.githubusercontent.com/drewnoakes/metadata-extractor-images/master/%s\">%n", urlName)
         ;(printf "<img src=\"https://raw.githubusercontent.com/drewnoakes/metadata-extractor-images/master/%s\" width=\"300\"/><br/>%n", urlName)
         (println fileName)
         ;(println "</a>")
         (println )
         (println "|Directory | Tag Id | Tag Name | Extracted Value|")
         (println "|:--------:|-------:|----------|----------------|")
         (doseq [^Directory directory (.getDirectories metadata)]
           (let [^String directoryName  (.getName directory)]
             (doseq [^Tag tag (.getTags directory)]
               (let [^String tagName (.getTagName tag)
                     ^String description (truncate (.getDescription tag))]
                 (printf "|%s|0x%s|%s|%s|%n" directoryName (Integer/toHexString (.getTagType tag))
                         tagName description)
                 (if (instance? XmpDirectory directory)
                   (doseq [^Map$Entry property (.entrySet (.getXmpProperties ^XmpDirectory directory))]
                     (let [^String key (.getKey property)
                           ^String value (truncate (.getValue property))]
                       (printf "|%s||%s|%s|%n" directoryName key value)))))))
           (doseq [^String error (.getErrors directory)] (println "ERROR: " error)))))))

  ([source]
   (let [metadata (read-metadata source)
         file (io/file source)
         folder (.getParentFile file)
         filename (mci/prefix file)]
     (write-metadata-markdown source metadata (io/file folder (str filename ".md"))))))

;;-------------------------------------------------------------
(doseq [source [
                "images/earth/eo_base_2020_clean_geo.tif"
                "images/earth/ETOPO_2022_v1_60s_N90W180_bed.tif"
                "images/earth/ETOPO_2022_v1_60s_N90W180_geoid.tif"
                "images/earth/ETOPO_2022_v1_60s_N90W180_surface.tif"
                "images/earth/gebco_08_rev_bath_21600x10800.png"
                "images/earth/gebco_08_rev_elev_21600x10800.png"
                "images/earth/world.topo.bathy.200412.3x21600x10800.png"
                "images/earth/world.200412.3x21600x10800.png"]]
        (write-metadata-markdown source))
;;-------------------------------------------------------------
