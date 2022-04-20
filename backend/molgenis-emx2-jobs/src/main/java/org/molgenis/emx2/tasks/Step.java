package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.StepStatus.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For documenting processes that consist of multiple steps and elements. For example. batch
 * insert/upload tasks
 */
public class Step {
  // for the toString method
  private static ObjectMapper mapper =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
  private Integer index;
  private String description;
  StepStatus status = WAITING;
  private Integer total;
  private long start = System.currentTimeMillis();
  long end;

  public Step(String description) {
    this.description = description;
  }

  public Step(String description, StepStatus status) {
    this(description);
    this.status = status;
  }

  public Integer getIndex() {
    if (RUNNING.equals(status)) return index;
    return null;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public String getDescription() {
    String message = description;
    switch (status) {
      case WAITING:
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
        }
        return message;
      case RUNNING:
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
          if (getIndex() != null && getIndex() > 0) {
            message += " at " + 1000 * getIndex() / getDuration() + " items/sec";
          }
        }
        return message;
      default:
        if (getDuration() > 0) {
          message += " in " + getDuration() + "ms";
          if (getTotal() != null && getTotal() > 0) {
            message += " (" + 1000 * getTotal() / getDuration() + " items/sec)";
          }
        }
        return message;
    }
  }

  public Step setDescription(String description) {
    this.description = description;
    return this;
  }

  public StepStatus getStatus() {
    return status;
  }

  public Integer getTotal() {
    if (COMPLETED.equals(status)) {
      return index;
    }
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public void start() {
    this.start = System.currentTimeMillis();
    this.status = RUNNING;
    this.logger.info(getDescription() + ": started");
  }

  public void complete(String description) {
    this.description = description;
    complete();
  }

  public void complete() {
    this.status = COMPLETED;
    this.end = System.currentTimeMillis();
    this.logger.info(getDescription());
  }

  public void completeWithError(String message) {
    this.complete();
    this.setDescription(message);
    this.status = ERROR;
    logger.error(message);
    throw new MolgenisException(message);
  }

  public long getDuration() {
    if (end == 0) {
      return System.currentTimeMillis() - start;
    } else {
      return end - start;
    }
  }

  public Step setStatus(StepStatus status) {
    this.status = status;
    return this;
  }

  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      // should never happen
      throw new MolgenisException("internal error", e);
    }
  }

  public void skipped(String description) {
    this.skipped();
    this.setDescription(description);
  }

  public void skipped() {
    this.complete();
    this.setStatus(SKIPPED);
  }

  public void error() {
    this.complete();
    this.setStatus(ERROR);
  }

  public void error(String description) {
    this.error();
    this.setDescription(description);
  }
}
