/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.unidata.geoloc.projection;

import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Grib 1 projection 10 and Grib 2 projection 1.
 * The Rotated Latitude Longitude projection algorithms that are coded
 * here were given by Tor Christian Bekkvik <torc@cmr.no>. The rotated
 * lat/lon projection coordinates are defined in the grid file that
 * need to be converted back to unrotated lat/lon projection coordinates
 * before they can be displayed. The X/Y axis only makes sense in the rotated
 * projection.
 *
 * @author rkambic
 * @author Tor Christian Bekkvik <torc@cmr.no>
 * @since Nov 11, 2008
 */

public class RotatedLatLon extends ProjectionImpl {
  public static final String GRID_MAPPING_NAME = "rotated_latlon_grib";
  public static final String GRID_SOUTH_POLE_LONGITUDE = "grid_south_pole_longitude";
  public static final String GRID_SOUTH_POLE_LATITUDE = "grid_south_pole_latitude";
  public static final String GRID_SOUTH_POLE_ANGLE = "grid_south_pole_angle";

  private static boolean show = false;

  private final double lonpole; // Longitude of south pole
  private final double latpole; // Latitude of south pole
  private final double polerotate; // Angle of south pole  rotation

  private double cosDlat;
  private double sinDlat;

  /**
   * Default Constructor, needed for beans.
   */
  public RotatedLatLon() {
    this(0.0, 0.0, 0.0);
  }

  /**
   * Constructor.
   *
   * @param southPoleLat   in degrees
   * @param southPoleLon   in degrees
   * @param southPoleAngle in degrees
   */
  public RotatedLatLon(double southPoleLat, double southPoleLon, double southPoleAngle) {
    super("RotatedLatLon", false);

    this.latpole = southPoleLat;
    this.lonpole = southPoleLon;
    this.polerotate = southPoleAngle;
    double dlat_rad = Math.toRadians(latpole - (-90));
    sinDlat = Math.sin(dlat_rad);
    cosDlat = Math.cos(dlat_rad);

    addParameter(CF.GRID_MAPPING_NAME, GRID_MAPPING_NAME);
    addParameter(GRID_SOUTH_POLE_LATITUDE, southPoleLat);
    addParameter(GRID_SOUTH_POLE_LONGITUDE, southPoleLon);
    addParameter(GRID_SOUTH_POLE_ANGLE, southPoleAngle);
  }

  @Override
  public ProjectionImpl constructCopy() {
    ProjectionImpl result =  new RotatedLatLon(latpole, lonpole, polerotate);
    result.setDefaultMapArea(defaultMapArea);
    return result;
  }

  /**
   * returns constructor params as a String
   *
   * @return String
   */
  public String paramsToString() {
    return " southPoleLat =" + latpole + " southPoleLon =" + lonpole + " southPoleAngle =" + polerotate;
  }

  /**
   * Transform a "real" longitude and latitude into the rotated longitude (X) and
   * rotated latitude (Y).
   */
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint) {
    /*
    Tor's algorithm
    public double[] fwd(double[] lonlat)
      return transform(lonlat, lonpole, polerotate, sinDlat);
    */
    double[] lonlat = new double[2];
    lonlat[0] = latlon.getLongitude();
    lonlat[1] = latlon.getLatitude();

    double[] rlonlat = rotate(lonlat, lonpole, polerotate, sinDlat);
    if (destPoint == null)
      destPoint = new ProjectionPointImpl(rlonlat[0], rlonlat[1]);
    else
      destPoint.setLocation(rlonlat[0], rlonlat[1]);

    if (show)
      System.out.println("LatLon= " + latlon + " proj= " + destPoint);

    return destPoint;
  }

  /**
   * Transform a rotated longitude (X) and rotated latitude (Y) into a "real"
   * longitude-latitude pair.
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint) {
    /*
    Tor's algorithm
    public double[] inv(double[] lonlat)
      return rotate(lonlat, -polerotate, -lonpole, -sinDlat);
    */
    double[] lonlat = new double[2];
    lonlat[0] = ppt.getX();
    lonlat[1] = ppt.getY();

    double[] rlonlat = rotate(lonlat, -polerotate, -lonpole, -sinDlat);
    if (destPoint == null)
      destPoint = new LatLonPointImpl(rlonlat[1], rlonlat[0]);
    else
      destPoint.set(rlonlat[1], rlonlat[0]);

    if (show)
      System.out.println("Proj= " + ppt + " latlon= " + destPoint);
    return destPoint;
  }

  // Tor's tranform algorithm renamed to rotate for clarity
  private double[] rotate(double[] lonlat, double rot1, double rot2, double s) {

    double e = Math.toRadians(lonlat[0] - rot1); //east
    double n = Math.toRadians(lonlat[1]); //north
    double cn = Math.cos(n);
    double x = cn * Math.cos(e);
    double y = cn * Math.sin(e);
    double z = Math.sin(n);
    double x2 = cosDlat * x + s * z;
    double z2 = -s * x + cosDlat * z;
    double R = Math.sqrt(x2 * x2 + y * y);
    double e2 = Math.atan2(y, x2);
    double n2 = Math.atan2(z2, R);
    double rlon = Math.toDegrees(e2 - rot2);
    double rlat =  Math.toDegrees(n2);
    return new double[]{rlon, rlat};

  }

  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RotatedLatLon that = (RotatedLatLon) o;

    if (Double.compare(that.latpole, latpole) != 0) return false;
    if (Double.compare(that.lonpole, lonpole) != 0) return false;
    if (Double.compare(that.polerotate, polerotate) != 0) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = lonpole != +0.0d ? Double.doubleToLongBits(lonpole) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = latpole != +0.0d ? Double.doubleToLongBits(latpole) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = polerotate != +0.0d ? Double.doubleToLongBits(polerotate) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  private static class Test {
    RotatedLatLon rll;
    static PrintStream ps = System.out;

    public Test(double lo, double la, double rot) {
      rll = new RotatedLatLon(la, lo, rot);
      ps.println("lonsp:" + rll.lonpole +
              ", latsp:" + rll.latpole +
              ", rotsp:" + rll.polerotate);
    }

    void pr(double[] pos, double[] pos2, double[] pos3) {
      ps.println(" " + pos[0] + "   " + pos[1]);
      ps.println("    fwd: " + pos2[0] + "   " + pos2[1]);
      ps.println("    inv: " + pos3[0] + "   " + pos3[1]);
    }

    final static double err = 0.0001;

    private double[] test(float lon, float lat) {
      double[] p = {lon, lat};
      double[] p2 = rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      double[] p3 = rll.rotate(p2, -rll.polerotate, -rll.lonpole, -rll.sinDlat);
      assert Math.abs(p[0] - p3[0]) < err;
      assert Math.abs(p[1] - p3[1]) < err;
      pr(p, p2, p3);
      return p2;
    }

    double[] proj(double lon, double lat, boolean fwd) {
      double[] pos = {lon, lat};
      double[] pos2 = fwd ?
              rll.rotate(pos, rll.lonpole, rll.polerotate, rll.sinDlat) :
              rll.rotate(pos, -rll.polerotate, -rll.lonpole, -rll.sinDlat);
      ps.println((fwd ? " fwd" : " inv")
              + " [" + lon + ", " + lat + "] -> " + Arrays.toString(pos2));
      return pos2;
    }
  }


  private static void test() {
    Test tst0 = new Test(0, -25, 0);
    double[] pos = tst0.proj(0, -25, true);
    double[] pos2 = tst0.proj(pos[0], pos[1], false);

    double[] pos3 = tst0.proj(-46.5, -36.5, false);
    double[] pos4 = tst0.proj(46.9, 38.9, false);

    Test t = new Test(0, 90, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Test(0, 0, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Test(10, 50, 25);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = null;
    RotatedLatLon rll = new RotatedLatLon(-50, 10, 20);
    long t0 = System.currentTimeMillis();
    long dt = 0;
    double[] p = {12., 60.};
    int i = 0;
    while (dt < 1000) {
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      rll.rotate(p, rll.lonpole, rll.polerotate, rll.sinDlat);
      i++;
      dt = System.currentTimeMillis() - t0;
    }
    System.out.println("fwd/sec: " + i * 10);

  }

  public static void main(String args[]) {
    test();
  }

}
