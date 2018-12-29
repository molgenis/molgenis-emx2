package org.molgenis.emx2.io;

import java.io.Serializable;

public class MolgenisReaderMessage implements Serializable {
  private Integer lineNumber;
  private String message;

  public MolgenisReaderMessage(Integer lineNumber, String message) {
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
