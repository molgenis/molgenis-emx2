package org.molgenis.emx2.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** compatible with https://tools.ietf.org/html/rfc7807 */
public class MolgenisException extends RuntimeException {
  private final String message;
  private final Class type;
  private final List<MolgenisExceptionDetail> details = new ArrayList<>();

  public MolgenisException(Exception cause) {
    super(cause);
    this.type = getClass();
    this.message = cause.getMessage();
  }

  public MolgenisException(String message) {
    this.type = getClass();
    this.message = message;
  }

  public MolgenisException(String message, Exception e) {
    super(e);
    this.type = getClass();
    this.message = message;
  }

  public MolgenisException(String message, List<MolgenisExceptionDetail> details) {
    super(message + "\nSee getMessages() for list of error messages");
    this.type = getClass();
    this.message = message;
    this.details.addAll(details);
  }

  public List<MolgenisExceptionDetail> getDetails() {
    return Collections.unmodifiableList(this.details);
  }

  public String getMessage() {
    return message;
  }

  public String getType() {
    if (type != null) return type.getSimpleName();
    return null;
  }
}
