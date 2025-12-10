(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\image\metaMarkdown.clj
;;----------------------------------------------------------------
(ns mop.scripts.image.metaMarkdown
  {:doc
   "Extract image metadata."
   :author "palisades dot lakes at gmail dot com"
   :version "2025-111-05"}

  (:require
   [clojure.java.io :as io]
   [mop.image.util :as image]))

;;-------------------------------------------------------------

(doseq [source (image/image-file-seq (io/file "images"))]
  (image/write-metadata-markdown source))

;;-------------------------------------------------------------
