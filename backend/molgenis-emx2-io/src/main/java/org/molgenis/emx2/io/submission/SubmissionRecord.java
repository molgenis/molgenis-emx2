package org.molgenis.emx2.io.submission;

import static org.molgenis.emx2.io.submission.SubmissionService.*;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.molgenis.emx2.*;

/* we use ActiveRecord pattern */
public class SubmissionRecord {
  private static Base64.Encoder base64 = Base64.getEncoder();
  private String id;
  private String targetSchema;
  private List<String> targetTables = new ArrayList<>();
  private String targetIdentifiers;
  private SubmissionStatus status = SubmissionStatus.INITIALIZING;
  private LocalDateTime created = LocalDateTime.now();
  private LocalDateTime changed = LocalDateTime.now();

  public enum SubmissionStatus {
    INITIALIZING,
    DRAFT,
    ERROR,
    MERGED
  }

  public SubmissionRecord() {
    // some reasonable id generator :-)
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(System.currentTimeMillis());
    this.id = base64.encodeToString(buffer.array()).replace("=", "");
  }

  public SubmissionRecord(Row row) {
    this.fromRow(row);
  }

  public String getSchema() {
    return "Submit_" + getId();
  }

  public String getId() {
    return id;
  }

  public SubmissionRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getTargetSchema() {
    return targetSchema;
  }

  public SubmissionRecord setTargetSchema(String targetSchema) {
    this.targetSchema = targetSchema;
    return this;
  }

  public List<String> getTargetTables() {
    return targetTables;
  }

  public SubmissionRecord setTargetTables(List<String> targetTables) {
    this.targetTables = targetTables;
    return this;
  }

  public String getTargetIdentifiers() {
    return targetIdentifiers;
  }

  public SubmissionRecord setTargetIdentifiers(String targetIdentifiers) {
    this.targetIdentifiers = targetIdentifiers;
    return this;
  }

  public SubmissionStatus getStatus() {
    return status;
  }

  public SubmissionRecord setStatus(SubmissionStatus status) {
    this.status = status;
    return this;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public SubmissionRecord setCreated(LocalDateTime created) {
    this.created = created;
    return this;
  }

  public LocalDateTime getChanged() {
    return changed;
  }

  public SubmissionRecord setChanged(LocalDateTime changed) {
    this.changed = changed;
    return this;
  }

  // todo, in future we can generate these code
  private void fromRow(Row row) {
    this.id = row.getString(ID);
    this.status = SubmissionStatus.valueOf(row.getString(STATUS));
    this.targetSchema = row.getString(TARGET_SCHEMA);
    if (row.getString(TARGET_TABLES) != null) {
      this.targetTables = new ArrayList<>(List.of(row.getStringArray(TARGET_TABLES)));
    }
    this.created = row.getDateTime(CREATED);
    this.changed = row.getDateTime(CHANGED);
  }
}
