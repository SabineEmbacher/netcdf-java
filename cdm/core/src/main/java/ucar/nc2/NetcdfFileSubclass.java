/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * To allow subclassing of NetcdfFile.
 * Expert use only
 *
 * @author caron
 * @since 10/29/2014
 */
public class NetcdfFileSubclass extends NetcdfFile {

  public NetcdfFileSubclass() {}

  public NetcdfFileSubclass(IOServiceProvider iosp, String location) {
    this.iosp = iosp;
    this.location = location;
  }

  public NetcdfFileSubclass(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask)
      throws IOException {
    super(spi, raf, location, cancelTask);
    if (raf == null) {
      this.location = location;
    }
  }
}
