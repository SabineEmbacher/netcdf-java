package ucar.nc2.jni.netcdf;

import ucar.nc2.Variable;

/**
 * Pluggable component for deciding how to chunk netcdf-4 variables.
 *
 * @author caron
 * @since 11/14/12
 */
public interface Nc4Chunking {
  public enum Standard { unlimited, grib }

  public boolean isChunked(Variable v);

  public long[] computeChunking(Variable v);

  public int getDeflateLevel(Variable v);

  public boolean isShuffle(Variable v);

}
