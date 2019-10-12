package org.molgenis.emx2.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** compatible with https://tools.ietf.org/html/rfc7807 */
public class MolgenisException extends RuntimeException {
  private final String title;
  private final String detail;
  private final String type;

  private final List<MolgenisExceptionMessage> messages = new ArrayList<>();

  public MolgenisException(Exception e) {

    this(null, null, null, e);
  }

  public MolgenisException(String type, String title, String detail, Exception cause) {
    super(cause);
    this.type = type;
    this.title = title;
    this.detail = detail;
  }

  public MolgenisException(String type, String title, String detail) {
    super();
    this.type = type;
    this.title = title;
    this.detail = detail;
  }

  public MolgenisException(String detail, Exception e) {
    this(null, null, detail, e);
  }

  public MolgenisException(String message, List<MolgenisExceptionMessage> messages) {
    super(message + "\nSee getMessages() for list of error messages");
    this.type = "unknown_type";
    this.title = message;
    this.detail = null;
    this.messages.addAll(messages);
  }

  @Override
  public String getMessage() {
    return toString();
  }

  public List<MolgenisExceptionMessage> getMessages() {
    return Collections.unmodifiableList(this.messages);
  }

  public String getTitle() {
    return title;
  }

  public String getDetail() {
    return detail;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("Type: %s%nTitle: %s%nDetail: %s%n", getType(), getTitle(), getDetail());
  }
}
