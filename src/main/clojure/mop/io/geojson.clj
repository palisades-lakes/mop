(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)
;;----------------------------------------------------------------
(ns mop.io.geojson
  {:doc
   "Read and write meshes as
   <a href=\"https://datatracker.ietf.org/doc/html/rfc7946\">geojson</a>
   and <a href=\"https://github.com/topojson/topojson/blob/master/README.md\">
   topojson</a>.
   <br>
   See https://macwright.com/2015/03/23/geojson-second-bite
   <br>
   Geojson, and topojson by inheritance, have a significant limitation:
   Coordinates are assumed to be 2D (longitude,latitude) or (easting,northing),
   or 3D (longitude,latitude,altitude)/(easting,northing,altitude).
   (longitude,latitude) are in decimal degrees,
   specifying a point on the surface of the
   <a href=\"https://en.wikipedia.org/wiki/World_Geodetic_System\">WGS84</a>
   ellipsoid.
   <br>
   (easting,northing) are 'projected coordinates',
   presumably euclidean, but unspecified,
   without no unit information,
   and there is no way to indicate what projection was used.
   <br>
   Altitude is specified to be in meters, but whether the altitude is meant to
   be added to the lon,lat ellipsoid surface point
   in the surface normal direction, or radially from the ellipsoid center, or
   something else, is not specified.
   In the projected (easting,northing,altitude) case, this is presumably
   meant to be a point in R3, but there is no information about scaling (x,y)
   versus z.
   <br>
   Line segments are specified to be the sets of points resulting from
   linear (convex) interpolation in (lon,lat) coordinates, rather than
   geodesic great circle arcs.
   I imagine that, in practice, what is actually displayed is linear
   interpolation in whatever the current sphere-to-2d-euclidean projection is.
   The inconsistency in boundary definition means that whether a point
   appears to be inside or outside a polygon will depend on what projection
   is used.
   <br>
   A grosser, but at least more obvious, problem arises when line segments
   cross the longitude = 180 = -180 meridian.
   Geodesic interpolation has no problem with this.
   Interpolation in (lon,lat) has a violent discontinuity when an endpoint at
   (179.99999999,lat) shifts infinitesimally to (-179.99999999,lat).
   The specs solution is to disallow segments that cross the 180 meridian,
   requiring the user to cut all polygons that cross,
   which results in spurious polygon boundaries (usually most obvious in
   eastern Siberia).
   <br>
   It's not clear to me how many uses of geo/topojson actually follow the spec,
   especially given that the spec is not clear about how coordinates
   are to be interpreted.
   And, not only does the spec needlessly exclude general 2d/3d geometry
   applications, it even excludes the same geospatial applications for
   other planets.
   <br>
   My immediate application is possibly multiply wrapped triangulations of
   the earth, and optimizing how they are cut and flattened.
   I'd like to view the resulting meshes using Vega (Lite).
   At some point I will need to violate the spec.
   It remains to be seen whether this will break Vega (Lite).
   I already have a 3D renderer using lwjgl, and my need to write my own
   2D viewer, for speed if nothing else.
   "
   :author  "palisades dot lakes at gmail dot com"
   :version "2026-03-06"}
  (:require [mop.geom.mesh :as mesh])
  (:import [java.awt Polygon]
           [mop.java.cmplx TwoSimplex]
           [mop.java.geom.mesh TriangleMesh]
           [org.apache.commons.geometry.spherical.twod Point2S]))
;;-------------------------------------------------------------------
;; TODO: handle other embedding codomains
(defn coordinates [^Point2S p]
  (let [azimuth (Math/toDegrees (.getAzimuth p))
        lon (if (< azimuth 180) azimuth (- azimuth 360))
        lat (- 90 (Math/toDegrees (.getPolar p)))]
    [lon lat]))

(defn point [^Point2S p]
  {:type "Point" :coordinates (coordinates p)})

;; TODO: ensure the codomain of teh embedding is Point2S
(defn triangle-coordinates [^TwoSimplex face embedding]
  (let [ll0 (coordinates (embedding (.z0 face)))
        ll1 (coordinates (embedding (.z1 face)))
        ll2 (coordinates (embedding (.z2 face)))]
    [[ll0 ll1 ll2 ll0]]))

#_(defn triangle [^TwoSimplex face embedding]
    {:type Polygon :coordinates (triangle-coordinates face embedding)})
;;-------------------------------------------------------------------
(defn geo-edn [^TriangleMesh mesh]
  (let [embedding (mesh/embedding mesh)
        faces (.faces (mesh/cmplx mesh))]
    {:type "FeatureCollection"
     :features
     {:type "Feature"
      :geometry
      {:type        "MultiPolygon"
       :coordinates (mapv #(triangle-coordinates % embedding) faces)}}}))
