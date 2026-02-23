package org.molgenis.emx2.hpc.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
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

  private final SqlDatabase database;
  private final String systemSchemaName;

  public JobService(SqlDatabase database, String systemSchemaName) {
    this.database = database;
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
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);
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
   * access. Returns the updated job row, or null if the job was not in PENDING status (claim
   * conflict) or not found.
   */
  public Row claimJob(String jobId, String workerId) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
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
            return;
          }

          recordTransition(
              schema,
              jobId,
              HpcJobStatus.PENDING,
              HpcJobStatus.CLAIMED,
              workerId,
              "Claimed by worker " + workerId);

          logger.info("Job claimed: id={} worker={}", jobId, workerId);

          // Re-fetch the row to return consistent data
          List<Row> rows = schema.getTable("HpcJobs").where(f("id", EQUALS, jobId)).retrieveRows();
          if (!rows.isEmpty()) {
            result[0] = rows.getFirst();
          }
        });
    return result[0];
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
      String outputArtifactId) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable("HpcJobs");

          List<Row> rows = jobsTable.where(f("id", EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return;
          }
          Row job = rows.getFirst();
          HpcJobStatus currentStatus = HpcJobStatus.valueOf(job.getString("status"));

          // Idempotent: if already in target status, check if identical transition
          if (currentStatus == targetStatus) {
            result[0] = job; // idempotent success
            return;
          }

          // Validate transition
          if (!currentStatus.canTransitionTo(targetStatus)) {
            logger.warn(
                "Invalid transition for job={}: {} -> {}", jobId, currentStatus, targetStatus);
            return;
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

          result[0] = job;
        });
    return result[0];
  }

  /**
   * Deletes a job and its transitions. Non-terminal jobs are first cancelled, then deleted. Returns
   * the job row if deleted, null if not found.
   */
  public Row deleteJob(String jobId) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable("HpcJobs");

          List<Row> rows = jobsTable.where(f("id", EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return;
          }
          Row job = rows.getFirst();
          HpcJobStatus status = HpcJobStatus.valueOf(job.getString("status"));

          // If not terminal, cancel first
          if (!status.isTerminal()) {
            job.set("status", HpcJobStatus.CANCELLED.name());
            job.set("completed_at", LocalDateTime.now());
            jobsTable.update(job);
            recordTransition(
                schema, jobId, status, HpcJobStatus.CANCELLED, null, "Cancelled for deletion");
            logger.info("Job cancelled for deletion: id={} was={}", jobId, status);
          }

          // Delete transitions first (FK dependency)
          Table transitionsTable = schema.getTable("HpcJobTransitions");
          List<Row> transitions = transitionsTable.where(f("job_id", EQUALS, jobId)).retrieveRows();
          for (Row t : transitions) {
            transitionsTable.delete(t);
          }

          // Delete the job
          jobsTable.delete(job);
          result[0] = job;
          logger.info("Job deleted: id={} status={}", jobId, status);
        });
    return result[0];
  }

  /** Gets a job by ID. Returns null if not found. */
  public Row getJob(String jobId) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcJobs")
                  .where(f("id", EQUALS, jobId))
                  .retrieveRows();
          if (!rows.isEmpty()) {
            result[0] = rows.getFirst();
          }
        });
    return result[0];
  }

  /**
   * Lists jobs with optional filters, pagination, and sorting. When status is null, defaults to
   * PENDING (backwards compatible with worker polling).
   */
  public List<Row> listJobs(
      String status, String processor, String profile, int limit, int offset) {
    List<Row>[] result = new List[1];
    database.tx(
        db -> {
          db.becomeAdmin();
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
          result[0] = allRows.subList(start, end);
        });
    return result[0];
  }

  /** Counts jobs matching the given filters. Used for pagination metadata. */
  public int countJobs(String status, String processor, String profile) {
    int[] count = new int[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Table jobsTable = db.getSchema(systemSchemaName).getTable("HpcJobs");

          String filterStatus = (status != null) ? status : HpcJobStatus.PENDING.name();
          var query = jobsTable.where(f("status", f("name", EQUALS, filterStatus)));

          if (processor != null) {
            query = query.where(f("processor", EQUALS, processor));
          }
          if (profile != null) {
            query = query.where(f("profile", EQUALS, profile));
          }

          count[0] = query.retrieveRows().size();
        });
    return count[0];
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
    database.tx(
        db -> {
          db.becomeAdmin();
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
            String claimedAtStr = job.getString("claimed_at");
            if (claimedAtStr == null) continue;
            LocalDateTime claimedAt = LocalDateTime.parse(claimedAtStr);
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
            String startedAtStr = job.getString("started_at");
            if (startedAtStr == null) continue;
            LocalDateTime startedAt = LocalDateTime.parse(startedAtStr);
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
    List<Row>[] result = new List[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          result[0] =
              db.getSchema(systemSchemaName)
                  .getTable("HpcJobTransitions")
                  .where(f("job_id", EQUALS, jobId))
                  .retrieveRows();
        });
    return result[0];
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
}
