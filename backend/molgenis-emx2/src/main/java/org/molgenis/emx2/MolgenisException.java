package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.molgenis.emx2.utils.MolgenisExceptionDetail;

public class MolgenisException extends RuntimeException {
  private final String message;
  private final Class<?> type;
  private final List<MolgenisExceptionDetail> details = new ArrayList<>();

  public MolgenisException(String message, Exception cause) {
    super(cause);
    this.type = getClass();
    if (cause instanceof MolgenisException) {
      this.message = message;
    } else {
      String rootMessage = ExceptionUtils.getRootCauseMessage(cause);
      this.message = message + ": " + rootMessage;
    }
  }

  public MolgenisException(String message) {
    this.type = getClass();
    this.message = message;
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
