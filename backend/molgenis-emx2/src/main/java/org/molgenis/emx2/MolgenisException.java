package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisExceptionDetail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MolgenisException extends RuntimeException {
  private final String title;
  private final String message;
  private final Class type;
  private final List<MolgenisExceptionDetail> details = new ArrayList<>();

  public MolgenisException(String title, Exception cause) {
    super(cause);
    this.type = getClass();
    this.title = title;
    this.message = cause.getMessage();
  }

  public MolgenisException(String title, String message) {
    this.type = getClass();
    this.title = title;
    this.message = message;
  }

  public MolgenisException(String title, String message, Exception e) {
    super(e);
    this.type = getClass();
    this.title = title;
    this.message = message;
  }

  public MolgenisException(String title, String message, List<MolgenisExceptionDetail> details) {
    super(message + "\nSee getMessages() for list of error messages");
    this.type = getClass();
    this.title = title;
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

  public String getTitle() {
    return title;
  }

  public String toString() {
    return "Title: " + getTitle() + "\nMessage: " + getMessage();
  }
}
