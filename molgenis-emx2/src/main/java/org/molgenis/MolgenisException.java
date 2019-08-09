package org.molgenis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MolgenisException extends Exception {
  private final List<MolgenisExceptionMessage> messages = new ArrayList<>();

  public MolgenisException(Exception e) {
    super(e);
  }

  public MolgenisException(String message) {
    super(message);
  }

  public MolgenisException(String message, Exception e) {
    super(message, e);
  }

  public MolgenisException(String message, List<MolgenisExceptionMessage> messages) {
    super(message + "\nSee getMessages() for list of error messages");
    this.messages.addAll(messages);
  }

  public List<MolgenisExceptionMessage> getMessages() {
    return Collections.unmodifiableList(this.messages);
  }
}
