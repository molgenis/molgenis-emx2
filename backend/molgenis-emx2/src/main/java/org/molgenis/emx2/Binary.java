package org.molgenis.emx2;

import java.io.IOException;

public interface Binary {

  String getMimeType();

  String getExtension();

  long getSize();

  byte[] getContents() throws IOException;
}
