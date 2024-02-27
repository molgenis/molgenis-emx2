package org.molgenis.emx2;

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Files;

public class BinaryFileWrapper implements Binary {
  // we use either file or byte[] for the contents
  File file;
  byte[] contents;
  // these we calculate on construction
  String mimetype;
  String extension = "";
  String fileName = "";
  // this is used to skip file updates
  boolean skip = false;

  public BinaryFileWrapper(boolean skip) {
    this.skip = true;
  }

  public BinaryFileWrapper(File file) {
    this.file = file;
    this.mimetype = URLConnection.getFileNameMap().getContentTypeFor(file.getName());
    this.fileName = file.getName();
    this.extension = deriveExtension(file.getName());
  }

  public BinaryFileWrapper(String contentType, String fileName, byte[] contents) {
    this.contents = contents;
    this.mimetype = contentType;
    this.fileName = fileName;
    this.extension = deriveExtension(fileName);
  }

  private String deriveExtension(String fileName) {
    if (fileName == null) {
      return "";
    }
    if (fileName.endsWith("tar.gz")) {
      return "tar.gz";
    }
    if (fileName.contains(".")) {
      return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    return "";
  }

  @Override
  public String getMimeType() {
    return mimetype;
  }

  @Override
  public String getExtension() {
    return extension;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public long getSize() {
    if (contents != null) {
      return contents.length;
    }
    if (file != null) {
      return file.length();
    }
    return 0;
  }

  @Override
  public byte[] getContents() { // would like to see bytestream here instead of byte[]
    if (contents != null) {
      return contents;
    }
    if (file != null) {
      try {
        return Files.readAllBytes(file.toPath());
      } catch (Exception e) {
        throw new MolgenisException("file error", e);
      }
    }
    return new byte[0];
  }

  public boolean isSkip() {
    return skip;
  }
}
