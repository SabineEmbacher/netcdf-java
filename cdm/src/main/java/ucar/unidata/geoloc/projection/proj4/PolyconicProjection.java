/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 *
 * Bernhard Jenny, 19 September 2010: fixed spherical inverse.

 * @author ghansham@sac.isro.gov.in 1/8/2012
 */
package ucar.unidata.geoloc.projection.proj4;

import java.awt.geom.Point2D;
import java.util.Formatter;

import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

public class PolyconicProjection extends ProjectionImpl {

  private double ml0;
  private double[] en;
  private final static double TOL = 1e-10;
  private final static double CONV = 1e-10;
  private final static int N_ITER = 10;
  private final static int I_ITER = 20;
  private final static double ITOL = 1.e-12;

  //New variables added
  private boolean spherical = true;
  private double projectionLatitude;
  private double projectionLongitude;
  private Earth ellipsoid;
  private double es;
  private double falseEasting;
  private double falseNorthing;
  private double totalScale;

  public PolyconicProjection() {
    this(23.56, 76.54);
  }

  public PolyconicProjection(double lat0, double lon0) {
    this(lat0, lon0, new Earth());
  }

  public PolyconicProjection(double lat0, double lon0, Earth ellipsoid) {
    this(lat0, lon0, 0.0, 0.0, ellipsoid);

  }

  public PolyconicProjection(double lat0, double lon0, double falseEasting, double falseNorthing, Earth ellipsoid) {
    super("Polyconic", false);

    //Initialization
    this.projectionLatitude = Math.toRadians(lat0);
    this.projectionLongitude = Math.toRadians(lon0);
    this.ellipsoid = ellipsoid;

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;
    this.es = this.ellipsoid.getEccentricitySquared();
    this.totalScale = this.ellipsoid.getMajor() * .001;

    initialize();

    //Adding parameters
    addParameter(CF.GRID_MAPPING_NAME, name);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);
    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, ellipsoid.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / ellipsoid.getFlattening());
  }

  private Point2D.Double project(double lplam, double lpphi, Point2D.Double out) {
    if (spherical) {
      double cot, E;

      if (Math.abs(lpphi) <= TOL) {
        out.x = lplam;
        out.y = ml0;
      } else {
        cot = 1. / Math.tan(lpphi);
        out.x = Math.sin(E = lplam * Math.sin(lpphi)) * cot;
        out.y = lpphi - projectionLatitude + cot * (1. - Math.cos(E));
      }
    } else {
      double ms, sp, cp;

      if (Math.abs(lpphi) <= TOL) {
        out.x = lplam;
        out.y = -ml0;
      } else {
        sp = Math.sin(lpphi);
        ms = Math.abs(cp = Math.cos(lpphi)) > TOL ? MapMath.msfn(sp, cp, es) / sp : 0.;
        out.x = ms * Math.sin(out.x *= sp);
        out.y = (MapMath.mlfn(lpphi, sp, cp, en) - ml0) + ms * (1. - Math.cos(lplam));
      }
    }
    return out;
  }

  private Point2D.Double projectInverse(double xyx, double xyy, Point2D.Double out) {
    double lpphi;

    if (spherical) {
      double B, dphi, tp;
      int i;

      if (Math.abs(xyy = projectionLatitude + xyy) <= TOL) {
        out.x = xyx;
        out.y = 0.;
      } else {
        lpphi = xyy;
        B = xyx * xyx + xyy * xyy;
        i = N_ITER;
        do {
          tp = Math.tan(lpphi);
          lpphi -= (dphi = (xyy * (lpphi * tp + 1.) - lpphi
                  - .5 * (lpphi * lpphi + B) * tp)
                  / ((lpphi - xyy) / tp - 1.));
        } while (Math.abs(dphi) > CONV && --i > 0);
        if (i == 0) {
          out.x = Double.NaN;
          out.y = Double.NaN;
        }
        out.x = Math.asin(xyx * Math.tan(lpphi)) / Math.sin(lpphi);
        out.y = lpphi;
      }
    } else {
      xyy += ml0;
      if (Math.abs(xyy) <= TOL) {
        out.x = xyx;
        out.y = 0.;
      } else {
        double r, c, sp, cp, s2ph, ml, mlb, mlp, dPhi;
        int i;

        r = xyy * xyy + xyx * xyx;
        for (lpphi = xyy, i = I_ITER; i > 0; --i) {
          sp = Math.sin(lpphi);
          s2ph = sp * (cp = Math.cos(lpphi));
          if (Math.abs(cp) < ITOL) {
            throw new RuntimeException("I");
          }
          c = sp * (mlp = Math.sqrt(1. - es * sp * sp)) / cp;
          ml = MapMath.mlfn(lpphi, sp, cp, en);
          mlb = ml * ml + r;
          mlp = (1.0 / es) / (mlp * mlp * mlp);
          lpphi += (dPhi =
                  (ml + ml + c * mlb - 2. * xyy * (c * ml + 1.)) / (es * s2ph * (mlb - 2. * xyy * ml) / c
                          + 2. * (xyy - ml) * (c * mlp - 1. / s2ph) - mlp - mlp));
          if (Math.abs(dPhi) <= ITOL) {
            break;
          }
        }
        if (i == 0) {
          out.x = Double.NaN;
          out.y = Double.NaN;
        }
        c = Math.sin(lpphi);
        out.x = Math.asin(xyx * Math.tan(lpphi) * Math.sqrt(1. - es * c * c)) / Math.sin(lpphi);
        out.y = lpphi;
      }
    }
    return out;
  }

  public boolean hasInverse() {
    return true;
  }

  private void initialize() {
    if (!spherical) {
      en = MapMath.enfn(es);
      if (en == null) {
        throw new RuntimeException("E");
      }
      ml0 = MapMath.mlfn(projectionLatitude, Math.sin(projectionLatitude), Math.cos(projectionLatitude), en);
    } else {
      ml0 = -projectionLatitude;
    }
  }

  /**
   * Check for equality with the Object in question
   *
   * @param proj object to check
   * @return true if they are equal
   */
  @Override
  public boolean equals(Object proj) {
    if (!(proj instanceof LambertConformalConicEllipse)) {
      return false;
    }

    PolyconicProjection oo = (PolyconicProjection) proj;
    return ((this.getOriginLatitude() == oo.getOriginLatitude())
            && (this.getOriginLongitude() == oo.getOriginLongitude())
            && this.ellipsoid.equals(oo.getEarth()));

  }

  //Beans properties
  public Earth getEarth() {
    return ellipsoid;
  }

  /**
   * Set the origin latitude.
   *
   * @param lat the origin latitude.
   */
  public void setOriginLatitude(double lat) { //suppply in degrees
    projectionLatitude = Math.toRadians(lat);
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLatitude() {
    return Math.toDegrees(projectionLatitude);
  }

  /**
   * Set the origin longitude.
   *
   * @param lon the origin longitude.
   */
  public void setOriginLongitude(double lon) {//suppply in degrees
    projectionLongitude = Math.toRadians(lon);
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLongitude() {
    return Math.toDegrees(projectionLongitude);
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting in km
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Set the false_easting, in km.
   * natural_x_coordinate + false_easting = x coordinate
   *
   * @param falseEasting x offset
   */
  public void setFalseEasting(double falseEasting) {
    this.falseEasting = falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing in km
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  /**
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   *
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
    this.falseNorthing = falseNorthing;
  }

  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Polyconic Projection";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  @Override
  public String paramsToString() {
    Formatter f = new Formatter();
    f.format("origin lat=%f, origin lon=%f earth=%s", Math.toDegrees(projectionLatitude), Math.toDegrees(projectionLongitude), ellipsoid);
    return f.toString();
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    if (ProjectionPointImpl.isInfinite(pt1) || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }


    // opposite signed X values, larger then 20,000 kml... similar to LambertConformal Conic
    return (pt1.getX() * pt2.getX() < 0)
            && (Math.abs(pt1.getX() - pt2.getX()) > 20000.0);


  }

  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latlon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl result) {
    double fromLat = Math.toRadians(latlon.getLatitude());
    double theta = Math.toRadians(latlon.getLongitude());
    if (projectionLongitude != 0 && !Double.isNaN(theta)) {
      theta = MapMath.normalizeLongitude(theta - projectionLongitude);
    }

    Point2D.Double out = new Point2D.Double();
    out = project(theta, fromLat, out);
    result.setLocation(totalScale * out.x + falseEasting, totalScale * out.y + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world, LatLonPointImpl result) {
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesian coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    Point2D.Double dst = projectInverse(fromX, fromY, new Point2D.Double());
    if (dst.x < -Math.PI) {
      dst.x = -Math.PI;
    } else if (dst.x > Math.PI) {
      dst.x = Math.PI;
    }
    if (projectionLongitude != 0 && !Double.isNaN(dst.x)) {
      dst.x = MapMath.normalizeLongitude(dst.x + projectionLongitude);
    }

    result.setLatitude(Math.toDegrees(dst.y));
    result.setLongitude(Math.toDegrees(dst.x));
    return result;
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new PolyconicProjection(getOriginLatitude(), getOriginLongitude(), getFalseEasting(), getFalseNorthing(), getEarth());
    result.setDefaultMapArea(defaultMapArea);
    return result;
  }
}
