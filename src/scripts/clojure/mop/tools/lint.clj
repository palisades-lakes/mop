;;----------------------------------------------------------------
;; clj src\scripts\clojure\mop\tools\lint.clj
;;----------------------------------------------------------------
(ns mop.tools.lint
  {:doc "Eastwood clojure lint tool: https://github.com/jonase/eastwood"
   :author "palisades dot lakes at gmail dot com"
   :version "2025-10-12"}

  (:require [eastwood.lint :as e]))

;; Replace the values of :source-paths and :test-paths with whatever
;; is appropriate for your project.  You may omit them, and then the
;; default behavior is to search all directories in your Java
;; classpath, and their subdirectories recursively, for Clojure source
;; files.

(e/with-memoization-bindings
 (e/lint
  {:source-paths ["src/main/clojure"
                  "src/scripts/clojure"]
   :namespaces ['mop.image.util
                'mop.lwjgl.util
                'mop.lwjgl.glfw.util
                'mop.moon
                ]
   :exclude-namespaces ['mop.tools.lint
                        'mop.tools.east]
   :test-paths ["test/main/clojure"]
   :out "lint.txt"
   :debug [:options]
   }))
