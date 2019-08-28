package org.molgenis.utils;

import java.io.Serializable;

public class MolgenisExceptionMessage implements Serializable {
  private Integer lineNumber;
  private String message;

  public MolgenisExceptionMessage(Integer lineNumber, String message) {
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
