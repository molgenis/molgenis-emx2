package org.molgenis.emx2.utils;

import java.io.Serializable;

public class MolgenisExceptionDetail implements Serializable {
  private Integer lineNumber;
  private String message;

  public MolgenisExceptionDetail(Integer lineNumber, String message) {
    this.lineNumber = lineNumber;
    this.message = message;
  }

  @Override
  public String toString() {
    return "Error on lineNumber " + lineNumber + ": " + message;
  }

  public Integer getLineNumber() {
    return lineNumber;
  }

  public String getMessage() {
    return message;
  }
}
