/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this
 * distribution.
 * The Eclipse Public License is available at http://www.eclipse
 * .org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package mop.java.jts;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.LinearComponentExtracter;
import org.locationtech.jts.triangulate.*;
import org.locationtech.jts.triangulate.quadedge.LocateFailureException;
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision;
import org.locationtech.jts.triangulate.quadedge.Vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Try minimal change to JTS code, to specify split finder.
 * <br>
 * A utility class which creates Conforming Delaunay Triangulations from
 * collections of points and linear constraints, and extract the
 * resulting triangulation edges or triangles as geometries.
 *
 * @author Martin Davis
 *
 */
@SuppressWarnings({"unchecked","unused"})
public final class MopConformingDelaunayTriangulationBuilder {
  private Collection siteCoords;
  private Geometry constraintLines;
  private double tolerance = 0.0;
  private QuadEdgeSubdivision subdiv = null;
  private final ConstraintSplitPointFinder _cspf;
  private final ConstraintSplitPointFinder getCspf () {
    return _cspf; }
  private final Map constraintVertexMap = new TreeMap();

  public MopConformingDelaunayTriangulationBuilder
    (final ConstraintSplitPointFinder cspf) {
    super();
    _cspf = cspf; }

  /**
   * Sets the sites (point or vertices) which will be triangulated. All
   * vertices of the given geometry will be used as sites. The site
   * vertices do not have to contain the constraint vertices as well;
   * any site vertices which are identical to a constraint vertex will
   * be removed from the site vertex set.
   *
   * @param geom the geometry from which the sites will be extracted.
   */
  public void setSites (final Geometry geom) {
    siteCoords =
      DelaunayTriangulationBuilder.extractUniqueCoordinates(geom);
  }

  /**
   * Sets the linear constraints to be conformed to. All linear
   * components in the input will be used as constraints. The constraint
   * vertices do not have to be disjoint from the site vertices. The
   * constraints must not contain duplicate segments (up to
   * orientation).
   *
   * @param constraintLines the lines to constraint to
   */
  public void setConstraints (final Geometry constraintLines) {
    this.constraintLines = constraintLines;
  }

  /**
   * Sets the snapping tolerance which will be used to improved the
   * robustness of the triangulation computation. A tolerance of 0.0
   * specifies that no snapping will take place.
   *
   * @param tolerance the tolerance distance to use
   */
  public void setTolerance (final double tolerance) {
    this.tolerance = tolerance;
  }


  private void create () {
    if (subdiv != null) { return; }

    final Envelope siteEnv =
      DelaunayTriangulationBuilder.envelope(siteCoords);

    final List segments;
    if (constraintLines != null) {
      siteEnv.expandToInclude(constraintLines.getEnvelopeInternal());
      createVertices(constraintLines);
      segments = createConstraintSegments(constraintLines);
    }
    else { segments = Collections.emptyList(); }
    final List sites = createSiteVertices(siteCoords);

    final ConformingDelaunayTriangulator
      cdt = new ConformingDelaunayTriangulator(sites, tolerance);

    // see if alternate split point strategy fixes
    // failure to converge problems
    cdt.setSplitPointFinder(getCspf());

    cdt.setConstraints(segments,new ArrayList(constraintVertexMap.values()));

    cdt.formInitialDelaunay();
    try { cdt.enforceConstraints(); }
    catch (final LocateFailureException e) {
      System.err.println("Conforming Delaunay triangulation failed:");
      System.err.println(e.getMessage());
    }
    subdiv = cdt.getSubdivision();
  }

  private List createSiteVertices (final Collection coords) {
    final List verts = new ArrayList();
    for (final Object o : coords) {
      final Coordinate coord = (Coordinate) o;
      if (constraintVertexMap.containsKey(coord)) { continue; }
      verts.add(new ConstraintVertex(coord));
    }
    return verts;
  }

  private void createVertices (final Geometry geom) {
    final Coordinate[] coords = geom.getCoordinates();
    for (final Coordinate coord : coords) {
      final Vertex v = new ConstraintVertex(coord);
      constraintVertexMap.put(coord, v);
    }
  }

  private static List createConstraintSegments (final Geometry geom) {
    final List lines = LinearComponentExtracter.getLines(geom);
    final List constraintSegs = new ArrayList();
    for (final Object o : lines) {
      final LineString line = (LineString) o;
      createConstraintSegments(line, constraintSegs);
    }
    return constraintSegs;
  }

  private static void createConstraintSegments (final LineString line,
                                                final List constraintSegs) {
    final Coordinate[] coords = line.getCoordinates();
    for (int i = 1; i < coords.length; i++) {
      constraintSegs.add(new Segment(coords[i - 1], coords[i]));
    }
  }

  /**
   * Gets the QuadEdgeSubdivision which models the computed
   * triangulation.
   *
   * @return the subdivision containing the triangulation
   */

  public QuadEdgeSubdivision getSubdivision () {
    create();
    return subdiv;
  }

  /**
   * Gets the edges of the computed triangulation as a
   * {@link MultiLineString}.
   *
   * @param geomFact the geometry factory to use to create the output
   *
   * @return the edges of the triangulation
   */
  public Geometry getEdges (final GeometryFactory geomFact) {
    create();
    return subdiv.getEdges(geomFact);
  }

  /**
   * Gets the faces of the computed triangulation as a
   * {@link GeometryCollection} of {@link Polygon}.
   *
   * @param geomFact the geometry factory to use to create the output
   *
   * @return the faces of the triangulation
   */
  public Geometry getTriangles (final GeometryFactory geomFact) {
    create();
    return subdiv.getTriangles(geomFact);
  }

}


