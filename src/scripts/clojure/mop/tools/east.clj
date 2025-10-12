;;----------------------------------------------------------------
;; clj-east src\scripts\clojure\mop\tools\east.clj > east.txt
;;----------------------------------------------------------------
(ns mop.tools.east
  {:doc "Eastwood clojure lint tool: https://github.com/jonase/eastwood"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-12"}

  (:require [eastwood.lint :as e]))

;; Replace the values of :source-paths and :test-paths with whatever
;; is appropriate for your project.  You may omit them, and then the
;; default behavior is to search all directories in your Java
;; classpath, and their subdirectories recursively, for Clojure source
;; files.

(e/eastwood
 {:source-paths ["src/main/clojure"
                 "src/scripts/clojure"]
  :test-paths ["test/main/clojure"]
  :namespaces ['mop.image.util
               'mop.lwjgl.util
               'mop.lwjgl.glfw.util]
  :exclude-namespaces ['mop.tools.lint
                       'mop.tools.east
                       'mop.moon
                       ]
  ;;:out "east.txt"
  ;;:debug [:all]
  })

