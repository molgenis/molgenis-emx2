package org.molgenis;

public class MolgenisException extends Exception {

  public MolgenisException(Exception e) {
    super(e);
  }

  public MolgenisException(String message) {
    super(message);
  }

  public MolgenisException(String message, Exception e) {
    super(message, e);
  }
}
