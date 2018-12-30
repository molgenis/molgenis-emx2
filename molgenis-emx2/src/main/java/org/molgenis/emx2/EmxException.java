package org.molgenis.emx2;

public class EmxException extends Exception {
  public EmxException(String message) {
    super(message);
  }

  public EmxException(Exception e) {
    super(e);
  }
}
