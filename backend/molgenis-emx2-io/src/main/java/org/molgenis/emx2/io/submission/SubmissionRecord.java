package org.molgenis.emx2.io.submission;

import static org.molgenis.emx2.io.submission.SubmissionService.*;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.molgenis.emx2.*;

@Getter
@Setter
@Accessors(chain = true)
/* we use ActiveRecord pattern */
public class SubmissionRecord {
  private static Base64.Encoder base64 = Base64.getEncoder();
  @NonNull private String id;
  @NonNull private String targetSchema;
  @NonNull private List<String> targetTables = new ArrayList<>();
  private String targetIdentifiers;
  @NonNull private SubmissionStatus status = SubmissionStatus.INITIALIZING;
  @NonNull private LocalDateTime created = LocalDateTime.now();
  @NonNull private LocalDateTime changed = LocalDateTime.now();

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
