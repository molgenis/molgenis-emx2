package org.molgenis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** compatible with https://tools.ietf.org/html/rfc7807 */
public class MolgenisException extends Exception {

  private String title;
  private String detail;
  private String type;

  private final List<MolgenisExceptionMessage> messages = new ArrayList<>();

  public MolgenisException(Exception e) {
    super(e);
  }

  public MolgenisException(String message) {
    super(message);
    this.setDetail(message);
  }

  public MolgenisException(String message, Exception e) {
    super(message, e);
    this.setDetail(message);
  }

  public MolgenisException(String message, List<MolgenisExceptionMessage> messages) {
    super(message + "\nSee getMessages() for list of error messages");
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

  protected void setTitle(String title) {
    this.title = title;
  }

  public String getDetail() {
    return detail;
  }

  protected void setDetail(String detail) {
    this.detail = detail;
  }

  public String getType() {
    return type;
  }

  protected void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return String.format("Type: %s%nTitle: %s%nDetail: %s%n", getType(), getTitle(), getDetail());
  }
}
