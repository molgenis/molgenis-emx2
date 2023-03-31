package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.molgenis.emx2.utils.MolgenisExceptionDetail;

public class MolgenisException extends RuntimeException {
  private final String message;
  private final Class<?> type;
  private final List<MolgenisExceptionDetail> details = new ArrayList<>();

  public MolgenisException(String message, Exception cause) {
    super(cause);
    this.type = getClass();
    this.message = message + ": " + cause.getMessage();
  }

  public MolgenisException(String message) {
    this.type = getClass();
    this.message = message;
  }

  @Deprecated
  public MolgenisException(String title, String message, Exception e) {
    super(e);
    this.type = getClass();
    this.message = title + ": " + message;
  }

  @Deprecated
  public MolgenisException(String title, String message, List<MolgenisExceptionDetail> details) {
    super(message + "\nSee getMessages() for list of error messages");
    this.type = getClass();
    this.message = title + ": " + message;
    this.details.addAll(details);
  }

  public List<MolgenisExceptionDetail> getDetails() {
    return Collections.unmodifiableList(this.details);
  }

  @Override
  public String getMessage() {
    return message;
  }

  public String getType() {
    if (type != null) return type.getSimpleName();
    return null;
  }

  @Override
  public String toString() {
    return getMessage();
  }
}
