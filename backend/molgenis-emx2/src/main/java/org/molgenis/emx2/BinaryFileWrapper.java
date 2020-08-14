package org.molgenis.emx2;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;

public class BinaryFileWrapper implements Binary {
  File file;

  public BinaryFileWrapper(File file) {
    this.file = file;
  }

  @Override
  public String getMimeType() {
    FileNameMap fileNameMap = URLConnection.getFileNameMap();
    return fileNameMap.getContentTypeFor(file.getName());
  }

  @Override
  public String getExtension() {
    if (file.getName().endsWith("tar.gz")) {
      return "tar.gz";
    }
    if (file.getName().contains(".")) {
      return file.getName().substring(file.getName().lastIndexOf('.') + 1);
    }
    return "";
  }

  @Override
  public long getSize() {
    return file.length();
  }

  @Override
  public byte[] getContents() {
    try {
      return Files.readAllBytes(file.toPath());
    } catch (Exception e) {
      throw new MolgenisException("file error", e);
    }
  }
}
