package org.molgenis.emx2.hpc.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.hpc.HpcFields.*;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.HpcTables;
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
      Object parameters,
      Object inputs,
      String submitUser,
      Integer timeoutSeconds) {
    String jobId = UUID.randomUUID().toString();
    tx.tx(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);

          // Validate that all input artifacts are COMMITTED
          if (inputs != null) {
            List<String> artifactIds = extractArtifactIds(inputs);
            validateArtifactsCommitted(schema, artifactIds);
          }

          Row jobRow =
              row(
                  ID, jobId,
                  PROCESSOR, processor,
                  PROFILE, profile,
                  PARAMETERS, parameters,
                  STATUS, HpcJobStatus.PENDING.name(),
                  INPUTS, inputs,
                  SUBMIT_USER, submitUser,
                  CREATED_AT, LocalDateTime.now());
          if (timeoutSeconds != null) {
            jobRow.set(TIMEOUT_SECONDS, timeoutSeconds);
          }
          schema.getTable(HpcTables.JOBS).insert(jobRow);

          recordTransition(
              schema, jobId, null, HpcJobStatus.PENDING, null, "Job created", null, null, null);
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
          org.jooq.Table<?> jobsJooq = table(name(systemSchemaName, HpcTables.JOBS));
          LocalDateTime now = LocalDateTime.now();

          int affected =
              jooq.update(jobsJooq)
                  .set(field(STATUS), HpcJobStatus.CLAIMED.name())
                  .set(field(WORKER_ID), workerId)
                  .set(field(CLAIMED_AT), now)
                  .where(field(ID).eq(jobId).and(field(STATUS).eq(HpcJobStatus.PENDING.name())))
                  .execute();

          if (affected == 0) {
            logger.warn(
                "Claim failed for job={} by worker={} (not PENDING or not found)", jobId, workerId);
            return ClaimResult.notPending();
          }

          // Re-fetch the claimed job
          List<Row> rows =
              schema.getTable(HpcTables.JOBS).where(f(ID, EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return ClaimResult.notPending();
          }
          Row job = rows.getFirst();

          // Verify worker has a matching capability for this job's processor/profile
          String processor = job.getString(PROCESSOR);
          String profile = job.getString(PROFILE);
          if (!workerHasCapability(schema, workerId, processor, profile)) {
            // Roll back: set status back to PENDING (guarded by status='CLAIMED')
            jooq.update(jobsJooq)
                .set(field(STATUS), HpcJobStatus.PENDING.name())
                .setNull(field(WORKER_ID))
                .setNull(field(CLAIMED_AT))
                .where(field(ID).eq(jobId).and(field(STATUS).eq(HpcJobStatus.CLAIMED.name())))
                .execute();
            logger.warn(
                "Claim rejected for job={}: worker {} lacks capability {}/{}",
                jobId,
                workerId,
                processor,
                profile);
            return ClaimResult.capabilityMismatch();
          }

          // Enforce max_concurrent_jobs from worker capability registration
          if (isWorkerAtCapacity(schema, jooq, systemSchemaName, workerId, processor, profile)) {
            // Roll back: set status back to PENDING (guarded by status='CLAIMED')
            jooq.update(jobsJooq)
                .set(field(STATUS), HpcJobStatus.PENDING.name())
                .setNull(field(WORKER_ID))
                .setNull(field(CLAIMED_AT))
                .where(field(ID).eq(jobId).and(field(STATUS).eq(HpcJobStatus.CLAIMED.name())))
                .execute();
            logger.warn(
                "Claim rejected for job={}: worker {} at max_concurrent_jobs for {}/{}",
                jobId,
                workerId,
                processor,
                profile);
            return ClaimResult.capacityExceeded();
          }

          recordTransition(
              schema,
              jobId,
              HpcJobStatus.PENDING,
              HpcJobStatus.CLAIMED,
              workerId,
              "Claimed by worker " + workerId,
              null,
              null,
              null);

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
            .getTable(HpcTables.WORKER_CAPABILITIES)
            .where(f(WORKER_ID, EQUALS, workerId))
            .where(f(PROCESSOR, EQUALS, processor))
            .retrieveRows();
    if (caps.isEmpty()) {
      return false;
    }
    // Job with no profile matches any capability for this processor
    if (profile == null || profile.isBlank()) {
      return true;
    }
    return caps.stream().anyMatch(c -> profile.equals(c.getString(PROFILE)));
  }

  /**
   * Checks whether a worker has reached its max_concurrent_jobs limit for the matching capability.
   * Counts active (CLAIMED, SUBMITTED, STARTED) jobs assigned to this worker and compares against
   * the declared limit. Returns false (not at capacity) if no limit is declared.
   */
  private static boolean isWorkerAtCapacity(
      Schema schema,
      DSLContext jooq,
      String systemSchemaName,
      String workerId,
      String processor,
      String profile) {
    // Find the matching capability's max_concurrent_jobs
    List<Row> caps =
        schema
            .getTable(HpcTables.WORKER_CAPABILITIES)
            .where(f(WORKER_ID, EQUALS, workerId))
            .where(f(PROCESSOR, EQUALS, processor))
            .retrieveRows();

    Integer maxJobs = null;
    for (Row cap : caps) {
      String capProfile = cap.getString(PROFILE);
      if (profile == null || profile.isBlank() || profile.equals(capProfile)) {
        maxJobs = cap.getInteger(MAX_CONCURRENT_JOBS);
        break;
      }
    }

    if (maxJobs == null || maxJobs <= 0) {
      return false; // No limit declared
    }

    // Count active jobs for this worker (across all processors/profiles)
    org.jooq.Table<?> jobsJooq = table(name(systemSchemaName, HpcTables.JOBS));
    int activeCount =
        jooq.fetchCount(
            jooq.selectFrom(jobsJooq)
                .where(
                    field(WORKER_ID)
                        .eq(workerId)
                        .and(
                            field(STATUS)
                                .in(
                                    HpcJobStatus.CLAIMED.name(),
                                    HpcJobStatus.SUBMITTED.name(),
                                    HpcJobStatus.STARTED.name()))));

    return activeCount > maxJobs; // > not >= because the just-claimed job is already counted
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
      String logArtifactId,
      String phase,
      String message,
      Double progress) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table jobsTable = schema.getTable(HpcTables.JOBS);

          List<Row> rows = jobsTable.where(f(ID, EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row job = rows.getFirst();
          HpcJobStatus currentStatus = HpcJobStatus.valueOf(job.getString(STATUS));

          if (!isAuthorizedTransition(job, targetStatus, workerId)) {
            logger.warn(
                "Rejected transition for job={} {} -> {}: worker {} does not own job (owner={})",
                jobId,
                currentStatus,
                targetStatus,
                workerId,
                job.getString(WORKER_ID));
            return null;
          }

          // Same-state transition: idempotent retry detection or progress update
          if (currentStatus == targetStatus) {
            if (isIdenticalTransitionRecorded(
                schema, jobId, targetStatus, workerId, detail, phase, message, progress)) {
              logger.debug(
                  "Idempotent duplicate transition for job={} status={}", jobId, targetStatus);
              return job;
            }
            if ((detail != null && !detail.isBlank())
                || phase != null
                || message != null
                || progress != null) {
              applyProgressSnapshot(job, phase, message, progress);
              jobsTable.update(job);
              recordTransition(
                  schema,
                  jobId,
                  currentStatus,
                  targetStatus,
                  workerId,
                  detail,
                  phase,
                  message,
                  progress);
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
          job.set(STATUS, targetStatus.name());
          if (workerId != null) {
            job.set(WORKER_ID, workerId);
          }
          if (slurmJobId != null) {
            job.set(SLURM_JOB_ID, slurmJobId);
          }
          if (outputArtifactId != null) {
            job.set(OUTPUT_ARTIFACT_ID, outputArtifactId);
          }
          if (logArtifactId != null) {
            job.set(LOG_ARTIFACT_ID, logArtifactId);
          }
          applyProgressSnapshot(job, phase, message, progress);

          // Set timestamp fields based on target status
          LocalDateTime now = LocalDateTime.now();
          switch (targetStatus) {
            case SUBMITTED -> job.set(SUBMITTED_AT, now);
            case STARTED -> job.set(STARTED_AT, now);
            case COMPLETED, FAILED, CANCELLED -> job.set(COMPLETED_AT, now);
            default -> {}
          }

          jobsTable.update(job);
          recordTransition(
              schema,
              jobId,
              currentStatus,
              targetStatus,
              workerId,
              detail,
              phase,
              message,
              progress);
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
    String assignedWorkerId = job.getString(WORKER_ID);
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
          Table jobsTable = schema.getTable(HpcTables.JOBS);

          List<Row> rows = jobsTable.where(f(ID, EQUALS, jobId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row job = rows.getFirst();
          HpcJobStatus status = HpcJobStatus.valueOf(job.getString(STATUS));

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
          Table transitionsTable = schema.getTable(HpcTables.JOB_TRANSITIONS);
          List<Row> transitions = transitionsTable.where(f(JOB_ID, EQUALS, jobId)).retrieveRows();
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
                  .getTable(HpcTables.JOBS)
                  .where(f(ID, EQUALS, jobId))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
  }

  /**
   * Lists jobs with optional filters, pagination, and sorting. When status is null, defaults to
   * PENDING (backwards compatible with worker polling).
   */
  public List<Row> listJobs(
      String status, String processor, String profile, int limit, int offset) {
    return tx.txResult(
        db -> {
          Table jobsTable = db.getSchema(systemSchemaName).getTable(HpcTables.JOBS);
          Query query = applyJobFilters(jobsTable.query(), status, processor, profile);
          return query.orderBy(jobListOrder()).limit(limit).offset(offset).retrieveRows();
        });
  }

  /** Counts jobs matching the given filters. Used for pagination metadata. */
  public int countJobs(String status, String processor, String profile) {
    return tx.txResult(
        db -> {
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          return jooq.fetchCount(
              jooq.selectFrom(table(name(systemSchemaName, HpcTables.JOBS)))
                  .where(jobListCondition(status, processor, profile)));
        });
  }

  /**
   * Lists pending jobs, optionally filtered by processor and profile. Convenience method for
   * backward compatibility with worker polling.
   */
  public List<Row> listPendingJobs(String processor, String profile) {
    return listJobs(HpcJobStatus.PENDING.name(), processor, profile, 100, 0);
  }

  private Query applyJobFilters(Query query, String status, String processor, String profile) {
    String filterStatus = (status != null) ? status : HpcJobStatus.PENDING.name();
    query.where(f(STATUS, f(NAME, EQUALS, filterStatus)));
    if (processor != null) {
      query.where(f(PROCESSOR, EQUALS, processor));
    }
    if (profile != null) {
      query.where(f(PROFILE, EQUALS, profile));
    }
    return query;
  }

  private Condition jobListCondition(String status, String processor, String profile) {
    String filterStatus = (status != null) ? status : HpcJobStatus.PENDING.name();
    Condition condition = field(name(STATUS)).eq(filterStatus);
    if (processor != null) {
      condition = condition.and(field(name(PROCESSOR)).eq(processor));
    }
    if (profile != null) {
      condition = condition.and(field(name(PROFILE)).eq(profile));
    }
    return condition;
  }

  private Map<String, Order> jobListOrder() {
    Map<String, Order> order = new LinkedHashMap<>();
    order.put(CREATED_AT, Order.ASC);
    order.put(ID, Order.ASC);
    return order;
  }

  /** Returns the audit trail of transitions for a job. */
  public List<Row> getTransitions(String jobId) {
    return tx.txResult(
        db ->
            db.getSchema(systemSchemaName)
                .getTable(HpcTables.JOB_TRANSITIONS)
                .where(f(JOB_ID, EQUALS, jobId))
                .orderBy(TIMESTAMP, Order.ASC)
                .retrieveRows());
  }

  private static void recordTransition(
      Schema schema,
      String jobId,
      HpcJobStatus from,
      HpcJobStatus to,
      String workerId,
      String detail,
      String phase,
      String message,
      Double progress) {
    schema
        .getTable(HpcTables.JOB_TRANSITIONS)
        .insert(
            row(
                ID,
                UUID.randomUUID().toString(),
                JOB_ID,
                jobId,
                FROM_STATUS,
                from != null ? from.name() : null,
                TO_STATUS,
                to.name(),
                TIMESTAMP,
                LocalDateTime.now(),
                WORKER_ID,
                workerId,
                DETAIL,
                detail,
                PHASE,
                phase,
                MESSAGE,
                message,
                PROGRESS,
                progress));
  }

  /**
   * Checks whether an identical transition has already been recorded for this job. A transition is
   * considered identical when the to_status, worker_id, detail, and structured progress fields all
   * match a previously recorded transition. Used for idempotent retry detection: if the daemon
   * retries a transition after a network timeout, the duplicate should return 200 OK without
   * recording again.
   */
  private static boolean isIdenticalTransitionRecorded(
      Schema schema,
      String jobId,
      HpcJobStatus toStatus,
      String workerId,
      String detail,
      String phase,
      String message,
      Double progress) {
    List<Row> transitions =
        schema.getTable(HpcTables.JOB_TRANSITIONS).where(f(JOB_ID, EQUALS, jobId)).retrieveRows();
    for (Row t : transitions) {
      if (Objects.equals(toStatus.name(), t.getString(TO_STATUS))
          && Objects.equals(workerId, t.getString(WORKER_ID))
          && Objects.equals(detail, t.getString(DETAIL))
          && Objects.equals(phase, t.getString(PHASE))
          && Objects.equals(message, t.getString(MESSAGE))
          && Objects.equals(progress, t.getDecimal(PROGRESS))) {
        return true;
      }
    }
    return false;
  }

  private static void applyProgressSnapshot(
      Row job, String phase, String message, Double progress) {
    if (phase != null) {
      job.set(PHASE, phase);
    }
    if (message != null) {
      job.set(MESSAGE, message);
    }
    if (progress != null) {
      job.set(PROGRESS, progress);
    }
  }

  /**
   * Extracts artifact IDs from the inputs JSON. Supports: JSON array of strings, JSON array of
   * objects with "artifact_id" field, or JSON object with string values (name→artifact_id mapping).
   */
  static List<String> extractArtifactIds(Object inputsValue) {
    List<String> ids = new ArrayList<>();
    try {
      JsonNode node = toJsonNode(inputsValue);
      if (node == null) {
        return ids;
      }
      if (node.isArray()) {
        for (JsonNode element : node) {
          if (element.isTextual()) {
            ids.add(element.asText());
          } else if (element.isObject() && element.has(ARTIFACT_ID)) {
            ids.add(element.get(ARTIFACT_ID).asText());
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
      logger.warn(
          "Could not parse inputs JSON for artifact validation (type={}): {}",
          inputsValue != null ? inputsValue.getClass().getSimpleName() : "null",
          e.getMessage());
    }
    return ids;
  }

  private static JsonNode toJsonNode(Object value) throws Exception {
    if (value == null) {
      return null;
    }
    if (value instanceof JsonNode node) {
      return node;
    }
    return MAPPER.valueToTree(value);
  }

  /**
   * Validates that all referenced artifacts exist and are in COMMITTED status. Throws
   * MolgenisException if any artifact is missing or not committed.
   */
  private static void validateArtifactsCommitted(Schema schema, List<String> artifactIds) {
    if (artifactIds.isEmpty()) return;
    Table artifactsTable = schema.getTable(HpcTables.ARTIFACTS);
    List<String> problems = new ArrayList<>();
    for (String artifactId : artifactIds) {
      List<Row> rows = artifactsTable.where(f(ID, EQUALS, artifactId)).retrieveRows();
      if (rows.isEmpty()) {
        problems.add("artifact " + artifactId + " not found");
      } else {
        String status = rows.getFirst().getString(STATUS);
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
