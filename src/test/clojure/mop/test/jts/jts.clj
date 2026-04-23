(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;------------------------------------------------------------------------------

(ns ^{:author "palisades dot lakes at gmail dot com"
      :date   "2026-04-22"
      :doc    "Tests for mop.jts.jts"}

  mop.test.jts.jts

  (:require
   [clojure.test :as t]
   [mop.jts.jts :as jts])
  (:import
   [org.locationtech.jts.geom GeometryFactory]
   [org.locationtech.jts.geom.impl PackedCoordinateSequenceFactory]))
;;------------------------------------------------------------------------------
;; mvn -Dtest=mop.test.jts.jts clojure:test
;;------------------------------------------------------------------------------

(t/deftest unique-pts
  (t/testing
   "Extraction of unique points from general <code>Geometry</code>."
    (let [tolerance 1.0e-6
          gfactory (GeometryFactory.)
          cfactory (PackedCoordinateSequenceFactory.)
          coords (double-array [10.0 13.0
                                0.0 0.0
                                3.0 -3.0
                                1.0e-7 1.0e-7
                                -5.0 1.0])
          coords (.create cfactory coords 2)
          raw (.createMultiPoint gfactory coords)
          cooked (jts/unique-points raw tolerance)]
      ;(println "raw:\n" (jts/wkt-string raw) )
      ;(println "cooked:\n " (jts/wkt-string cooked) )
      (t/is (== 5 (.getNumGeometries raw)))
      (t/is (== 4 (.getNumGeometries cooked)))
      )))

;;------------------------------------------------------------------------------
