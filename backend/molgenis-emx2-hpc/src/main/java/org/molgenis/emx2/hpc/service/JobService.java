package org.molgenis.emx2.hpc.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job lifecycle management: creation, atomic claiming, and state transitions.
 *
 * <p>Atomic claim: Uses a single SQL UPDATE ... WHERE status='PENDING' to guarantee that only one
 * worker can claim a given job, even under concurrent access. If the UPDATE affects 0 rows the job
 * was already claimed, and we return null (API translates to 409 Conflict).
 *
 * <p>Idempotent transitions: re-posting an identical transition (same from→to, same worker) returns
 * the existing transition. Posting a non-identical transition to a state that already transitioned
 * returns 409.
 */
public class JobService {

  private static final Logger logger = LoggerFactory.getLogger(JobService.class);

  private final TxHelper tx;
  private final String systemSchemaName;

  public JobService(SqlDatabase database, String systemSchemaName) {
    this.tx = new TxHelper(database);
    this.systemSchemaName = systemSchemaName;
  }

  /** Creates a new job in PENDING status. Returns the job ID. */
  public String createJob(
      String processor,
      String profile,
      String parameters,
      String inputs,
      String submitUser,
      Integer timeoutSeconds) {
    String jobId = UUID.randomUUID().toString();
    tx.tx(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);

          // Validate that all input artifacts are COMMITTED
          if (inputs != null && !inputs.isBlank()) {
            List<String> artifactIds = extractArtifactIds(inputs);
            validateArtifactsCommitted(schema, artifactIds);
          }

          Row jobRow =
              row(
                  "id", jobId,
                  "processor", processor,
                  "profile", profile,
                  "parameters", parameters,
                  "status", HpcJobStatus.PENDING.name(),
                  "inputs", inputs,
                  "submit_user", submitUser,
                  "created_at", LocalDateTime.now());
          if (timeoutSeconds != null) {
            jobRow.set("timeout_seconds", timeoutSeconds);
          }
          schema.getTable("HpcJobs").insert(jobRow);

          recordTransition(schema, jobId, null, HpcJobStatus.PENDING, null, "Job created");
        });
    logger.info(
        "Job created: id={} processor={} profile={} user={}",
        jobId,
        processor,
        profile,
        submitUser);
    return jobId;
  }

  /**
   * Atomically claims a PENDING job for a worker. Uses a single SQL UPDATE ... WHERE
   * status='PENDING' to guarantee that only one worker can claim a given job under concurrent
   * access. After the atomic claim succeeds, verifies that the worker has a registered capability
   * matching the job's processor/profile; if not, the claim is rolled back.
   *
   * @return a {@link ClaimResult} indicating success, not-pending, or capability mismatch
   */
  public ClaimResult claimJob(String jobId, String workerId) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);

          // Atomic UPDATE: only succeeds if job is still PENDING
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          org.jooq.Table<?> jobsJooq = table(name(systemSchemaName, "HpcJobs"));
          LocalDateTime now = LocalDateTime.now();

          int affected =
              jooq.update(jobsJooq)
                  .set(field("status"), HpcJobStatus.CLAIMED.name())
                  .set(field("worker_id"), workerId)
                  .set(field("claimed_at"), now)
                  .where(field("id").eq(jobId).and(field("status").eq(HpcJobStatus.PENDING.name())))
                  .execute();

          if (affected == 0) {
            logger.warn(
                "Claim failed for job={} by worker={} (not PENDING or not found)", jobId, workerId);
            return ClaimResult.notPending();
          }

          // Re-fetch the claimed job
          List<Row> rows = schema.getTable("HpcJobs").where(f("id", EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return ClaimResult.notPending();
          }
          Row job = rows.getFirst();

          // Verify worker has a matching capability for this job's processor/profile
          String processor = job.getString("processor");
          String profile = job.getString("profile");
          if (!workerHasCapability(schema, workerId, processor, profile)) {
            // Roll back: set status back to PENDING
            jooq.update(jobsJooq)
                .set(field("status"), HpcJobStatus.PENDING.name())
                .setNull(field("worker_id"))
                .setNull(field("claimed_at"))
                .where(field("id").eq(jobId))
                .execute();
            logger.warn(
                "Claim rejected for job={}: worker {} lacks capability {}/{}",
                jobId,
                workerId,
                processor,
                profile);
            return ClaimResult.capabilityMismatch();
          }

          recordTransition(
              schema,
              jobId,
              HpcJobStatus.PENDING,
              HpcJobStatus.CLAIMED,
              workerId,
              "Claimed by worker " + workerId);

          logger.info("Job claimed: id={} worker={}", jobId, workerId);
          return ClaimResult.success(job);
        });
  }

  /**
   * Checks whether a worker has a registered capability matching the given processor and profile. A
   * job with no profile matches any capability for the processor.
   */
  private static boolean workerHasCapability(
      Schema schema, String workerId, String processor, String profile) {
    List<Row> caps =
        schema
            .getTable("HpcWorkerCapabilities")
            .where(f("worker_id", EQUALS, workerId))
            .where(f("processor", EQUALS, processor))
            .retrieveRows();
    if (caps.isEmpty()) {
      return false;
    }
    // Job with no profile matches any capability for this processor
    if (profile == null || profile.isBlank()) {
      return true;
    }
    return caps.stream().anyMatch(c -> profile.equals(c.getString("profile")));
  }

  /**
   * Transitions a job from its current status to a new status. Validates the transition against the
   * state machine. Returns the updated job row, or null if the transition is invalid.
   */
  public Row transitionJob(
      String jobId,
      HpcJobStatus targetStatus,
      String workerId,
      String detail,
      String slurmJobId,
      String outputArtifactId,
      String logArtifactId) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable("HpcJobs");

          List<Row> rows = jobsTable.where(f("id", EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row job = rows.getFirst();
          HpcJobStatus currentStatus = HpcJobStatus.valueOf(job.getString("status"));

          if (!isAuthorizedTransition(job, targetStatus, workerId)) {
            logger.warn(
                "Rejected transition for job={} {} -> {}: worker {} does not own job (owner={})",
                jobId,
                currentStatus,
                targetStatus,
                workerId,
                job.getString("worker_id"));
            return null;
          }

          // Same-state transition: idempotent retry detection or progress update
          if (currentStatus == targetStatus) {
            if (isIdenticalTransitionRecorded(schema, jobId, targetStatus, workerId, detail)) {
              logger.debug(
                  "Idempotent duplicate transition for job={} status={}", jobId, targetStatus);
              return job;
            }
            if (detail != null && !detail.isBlank()) {
              recordTransition(schema, jobId, currentStatus, targetStatus, workerId, detail);
            }
            return job;
          }

          // Validate transition
          if (!currentStatus.canTransitionTo(targetStatus)) {
            logger.warn(
                "Invalid transition for job={}: {} -> {}", jobId, currentStatus, targetStatus);
            return null;
          }

          // Apply transition
          job.set("status", targetStatus.name());
          if (workerId != null) {
            job.set("worker_id", workerId);
          }
          if (slurmJobId != null) {
            job.set("slurm_job_id", slurmJobId);
          }
          if (outputArtifactId != null) {
            job.set("output_artifact_id", outputArtifactId);
          }
          if (logArtifactId != null) {
            job.set("log_artifact_id", logArtifactId);
          }

          // Set timestamp fields based on target status
          LocalDateTime now = LocalDateTime.now();
          switch (targetStatus) {
            case SUBMITTED -> job.set("submitted_at", now);
            case STARTED -> job.set("started_at", now);
            case COMPLETED, FAILED, CANCELLED -> job.set("completed_at", now);
            default -> {}
          }

          jobsTable.update(job);
          recordTransition(schema, jobId, currentStatus, targetStatus, workerId, detail);
          logger.info(
              "Job transitioned: id={} {} -> {} worker={}",
              jobId,
              currentStatus,
              targetStatus,
              workerId);

          return job;
        });
  }

  private static boolean isAuthorizedTransition(
      Row job, HpcJobStatus targetStatus, String workerId) {
    String assignedWorkerId = job.getString("worker_id");
    if (assignedWorkerId == null || assignedWorkerId.isBlank()) {
      return true;
    }

    // API/UI cancellations are intentionally allowed without worker ownership context.
    if (targetStatus == HpcJobStatus.CANCELLED && (workerId == null || workerId.isBlank())) {
      return true;
    }

    return assignedWorkerId.equals(workerId);
  }

  /**
   * Deletes a job and its transitions. The job MUST be in a terminal state; non-terminal jobs are
   * rejected with a MolgenisException. Returns the job row if deleted, null if not found.
   */
  public Row deleteJob(String jobId) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable("HpcJobs");

          List<Row> rows = jobsTable.where(f("id", EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row job = rows.getFirst();
          HpcJobStatus status = HpcJobStatus.valueOf(job.getString("status"));

          // Reject deletion of non-terminal jobs — caller must cancel first
          if (!status.isTerminal()) {
            throw new MolgenisException(
                "Cannot delete job "
                    + jobId
                    + " in non-terminal status "
                    + status
                    + ". Cancel it first.");
          }

          // Delete transitions first (FK dependency)
          Table transitionsTable = schema.getTable("HpcJobTransitions");
          List<Row> transitions = transitionsTable.where(f("job_id", EQUALS, jobId)).retrieveRows();
          for (Row t : transitions) {
            transitionsTable.delete(t);
          }

          // Delete the job
          jobsTable.delete(job);
          logger.info("Job deleted: id={} status={}", jobId, status);
          return job;
        });
  }

  /** Gets a job by ID. Returns null if not found. */
  public Row getJob(String jobId) {
    return tx.txResult(
        db -> {
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcJobs")
                  .where(f("id", EQUALS, jobId))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
  }

  /**
   * Lists jobs with optional filters, pagination, and sorting. When status is null, defaults to
   * PENDING (backwards compatible with worker polling).
   */
  // TODO: Use DB-level LIMIT/OFFSET when EMX2 Query API supports it
  public List<Row> listJobs(
      String status, String processor, String profile, int limit, int offset) {
    return tx.txResult(
        db -> {
          Table jobsTable = db.getSchema(systemSchemaName).getTable("HpcJobs");

          // Build filter chain
          String filterStatus = (status != null) ? status : HpcJobStatus.PENDING.name();
          var query = jobsTable.where(f("status", f("name", EQUALS, filterStatus)));

          if (processor != null) {
            query = query.where(f("processor", EQUALS, processor));
          }
          if (profile != null) {
            query = query.where(f("profile", EQUALS, profile));
          }

          List<Row> allRows = query.retrieveRows();

          // Apply offset and limit
          int start = Math.min(offset, allRows.size());
          int end = Math.min(start + limit, allRows.size());
          return allRows.subList(start, end);
        });
  }

  /** Counts jobs matching the given filters. Used for pagination metadata. */
  public int countJobs(String status, String processor, String profile) {
    return tx.txResult(
        db -> {
          Table jobsTable = db.getSchema(systemSchemaName).getTable("HpcJobs");

          String filterStatus = (status != null) ? status : HpcJobStatus.PENDING.name();
          var query = jobsTable.where(f("status", f("name", EQUALS, filterStatus)));

          if (processor != null) {
            query = query.where(f("processor", EQUALS, processor));
          }
          if (profile != null) {
            query = query.where(f("profile", EQUALS, profile));
          }

          return query.retrieveRows().size();
        });
  }

  /**
   * Lists pending jobs, optionally filtered by processor and profile. Convenience method for
   * backward compatibility with worker polling.
   */
  public List<Row> listPendingJobs(String processor, String profile) {
    return listJobs(HpcJobStatus.PENDING.name(), processor, profile, 100, 0);
  }

  /**
   * Expires jobs that have exceeded their per-job timeout_seconds. Checks CLAIMED and STARTED jobs
   * with a non-null timeout_seconds and transitions them to FAILED if the timeout has elapsed.
   * Called lazily from listJobs() so it runs once per poll cycle.
   */
  public void expireStaleJobs() {
    tx.tx(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable("HpcJobs");
          LocalDateTime now = LocalDateTime.now();

          // Check CLAIMED jobs
          List<Row> claimedJobs =
              jobsTable
                  .where(f("status", f("name", EQUALS, HpcJobStatus.CLAIMED.name())))
                  .retrieveRows();
          for (Row job : claimedJobs) {
            Integer timeout = job.getInteger("timeout_seconds");
            if (timeout == null) continue;
            LocalDateTime claimedAt = DateTimeUtil.parse(job.getString("claimed_at"));
            if (claimedAt == null) continue;
            if (claimedAt.plusSeconds(timeout).isBefore(now)) {
              String jobId = job.getString("id");
              logger.info("Expiring CLAIMED job {} (timeout {}s exceeded)", jobId, timeout);
              job.set("status", HpcJobStatus.FAILED.name());
              job.set("completed_at", now);
              jobsTable.update(job);
              recordTransition(
                  schema,
                  jobId,
                  HpcJobStatus.CLAIMED,
                  HpcJobStatus.FAILED,
                  null,
                  "timeout: claimed but not submitted within " + timeout + "s");
            }
          }

          // Check STARTED jobs
          List<Row> startedJobs =
              jobsTable
                  .where(f("status", f("name", EQUALS, HpcJobStatus.STARTED.name())))
                  .retrieveRows();
          for (Row job : startedJobs) {
            Integer timeout = job.getInteger("timeout_seconds");
            if (timeout == null) continue;
            LocalDateTime startedAt = DateTimeUtil.parse(job.getString("started_at"));
            if (startedAt == null) continue;
            if (startedAt.plusSeconds(timeout).isBefore(now)) {
              String jobId = job.getString("id");
              logger.info("Expiring STARTED job {} (timeout {}s exceeded)", jobId, timeout);
              job.set("status", HpcJobStatus.FAILED.name());
              job.set("completed_at", now);
              jobsTable.update(job);
              recordTransition(
                  schema,
                  jobId,
                  HpcJobStatus.STARTED,
                  HpcJobStatus.FAILED,
                  null,
                  "timeout: execution exceeded " + timeout + "s");
            }
          }
        });
  }

  /** Returns the audit trail of transitions for a job. */
  public List<Row> getTransitions(String jobId) {
    return tx.txResult(
        db ->
            db.getSchema(systemSchemaName)
                .getTable("HpcJobTransitions")
                .where(f("job_id", EQUALS, jobId))
                .retrieveRows());
  }

  private static void recordTransition(
      Schema schema,
      String jobId,
      HpcJobStatus from,
      HpcJobStatus to,
      String workerId,
      String detail) {
    schema
        .getTable("HpcJobTransitions")
        .insert(
            row(
                "id",
                UUID.randomUUID().toString(),
                "job_id",
                jobId,
                "from_status",
                from != null ? from.name() : null,
                "to_status",
                to.name(),
                "timestamp",
                LocalDateTime.now(),
                "worker_id",
                workerId,
                "detail",
                detail));
  }

  /**
   * Checks whether an identical transition has already been recorded for this job. A transition is
   * considered identical when the to_status, worker_id, and detail all match a previously recorded
   * transition. Used for idempotent retry detection: if the daemon retries a transition after a
   * network timeout, the duplicate should return 200 OK without recording again.
   */
  private static boolean isIdenticalTransitionRecorded(
      Schema schema, String jobId, HpcJobStatus toStatus, String workerId, String detail) {
    List<Row> transitions =
        schema.getTable("HpcJobTransitions").where(f("job_id", EQUALS, jobId)).retrieveRows();
    for (Row t : transitions) {
      if (Objects.equals(toStatus.name(), t.getString("to_status"))
          && Objects.equals(workerId, t.getString("worker_id"))
          && Objects.equals(detail, t.getString("detail"))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Extracts artifact IDs from the inputs JSON. Supports: JSON array of strings, JSON array of
   * objects with "artifact_id" field, or JSON object with string values (name→artifact_id mapping).
   */
  static List<String> extractArtifactIds(String inputsJson) {
    List<String> ids = new ArrayList<>();
    try {
      JsonNode node = MAPPER.readTree(inputsJson);
      if (node.isArray()) {
        for (JsonNode element : node) {
          if (element.isTextual()) {
            ids.add(element.asText());
          } else if (element.isObject() && element.has("artifact_id")) {
            ids.add(element.get("artifact_id").asText());
          }
        }
      } else if (node.isObject()) {
        node.fields()
            .forEachRemaining(
                entry -> {
                  if (entry.getValue().isTextual()) {
                    ids.add(entry.getValue().asText());
                  }
                });
      }
    } catch (Exception e) {
      logger.warn("Could not parse inputs JSON for artifact validation: {}", e.getMessage());
    }
    return ids;
  }

  /**
   * Validates that all referenced artifacts exist and are in COMMITTED status. Throws
   * MolgenisException if any artifact is missing or not committed.
   */
  private static void validateArtifactsCommitted(Schema schema, List<String> artifactIds) {
    if (artifactIds.isEmpty()) return;
    Table artifactsTable = schema.getTable("HpcArtifacts");
    List<String> problems = new ArrayList<>();
    for (String artifactId : artifactIds) {
      List<Row> rows = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
      if (rows.isEmpty()) {
        problems.add("artifact " + artifactId + " not found");
      } else {
        String status = rows.getFirst().getString("status");
        if (!ArtifactStatus.COMMITTED.name().equals(status)) {
          problems.add("artifact " + artifactId + " is " + status + ", expected COMMITTED");
        }
      }
    }
    if (!problems.isEmpty()) {
      throw new MolgenisException("Input artifacts not ready: " + String.join("; ", problems));
    }
  }
}
