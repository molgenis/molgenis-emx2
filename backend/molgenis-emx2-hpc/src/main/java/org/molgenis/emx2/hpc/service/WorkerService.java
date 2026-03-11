package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker registration, capability matching, and heartbeat management. Workers register on startup
 * and send periodic heartbeats. The service matches pending jobs to workers based on advertised
 * (processor, profile) capabilities.
 */
public class WorkerService {

  private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);

  /** Workers that haven't sent a heartbeat within this window are considered stale. */
  private static final long STALE_HEARTBEAT_SECONDS = 600;

  private final TxHelper tx;
  private final String systemSchemaName;

  public WorkerService(SqlDatabase database, String systemSchemaName) {
    this.tx = new TxHelper(database);
    this.systemSchemaName = systemSchemaName;
  }

  /**
   * Registers a worker or updates its heartbeat if already registered. Upserts the worker record
   * and replaces its capability set.
   *
   * @param workerId unique worker identifier
   * @param hostname the worker's hostname
   * @param capabilities list of {processor, profile, maxConcurrentJobs} maps
   * @return the worker row as stored
   */
  public Row registerOrHeartbeat(
      String workerId, String hostname, List<Map<String, Object>> capabilities) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table workersTable = schema.getTable("HpcWorkers");
          Table capTable = schema.getTable("HpcWorkerCapabilities");

          LocalDateTime now = LocalDateTime.now();

          // Upsert worker
          List<Row> existing = workersTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          Row workerRow;
          if (existing.isEmpty()) {
            workerRow =
                row(
                    "worker_id", workerId,
                    "hostname", hostname,
                    "registered_at", now,
                    "last_heartbeat_at", now);
            workersTable.insert(workerRow);
          } else {
            workerRow = existing.getFirst();
            workerRow.set("hostname", hostname);
            if (workerRow.getString("registered_at") == null
                || workerRow.getString("registered_at").isBlank()) {
              workerRow.set("registered_at", now);
            }
            workerRow.set("last_heartbeat_at", now);
            workersTable.update(workerRow);
          }

          // Replace capabilities: delete old, insert new
          List<Row> oldCaps = capTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          for (Row old : oldCaps) {
            capTable.delete(old);
          }

          if (capabilities != null) {
            int idx = 0;
            for (Map<String, Object> cap : capabilities) {
              capTable.insert(
                  row(
                      "id", workerId + "-cap-" + idx++,
                      "worker_id", workerId,
                      "processor", cap.get("processor"),
                      "profile", cap.get("profile"),
                      "max_concurrent_jobs", cap.get("max_concurrent_jobs")));
            }
          }

          return workerRow;
        });
  }

  /**
   * Deletes a worker, its capabilities, and all worker credentials. Nullifies worker_id on any jobs
   * referencing this worker.
   *
   * @return the deleted worker row, or null if not found
   */
  public Row deleteWorker(String workerId) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table workersTable = schema.getTable("HpcWorkers");

          List<Row> rows = workersTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row worker = rows.getFirst();

          // Delete capabilities (FK dependency)
          Table capTable = schema.getTable("HpcWorkerCapabilities");
          List<Row> caps = capTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          for (Row cap : caps) {
            capTable.delete(cap);
          }

          // Delete worker credentials
          Table credentialsTable = schema.getTable("HpcWorkerCredentials");
          List<Row> credentials =
              credentialsTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          for (Row credential : credentials) {
            credentialsTable.delete(credential);
          }

          // Nullify worker_id on any jobs referencing this worker
          Table jobsTable = schema.getTable("HpcJobs");
          List<Row> jobs = jobsTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          for (Row job : jobs) {
            job.set("worker_id", (String) null);
            jobsTable.update(job);
          }

          // Delete the worker
          workersTable.delete(worker);
          return worker;
        });
  }

  /** Updates the heartbeat timestamp for a worker. Returns false if worker is not registered. */
  public boolean heartbeat(String workerId) {
    return tx.txResult(
        db -> {
          Table workersTable = db.getSchema(systemSchemaName).getTable("HpcWorkers");
          List<Row> rows = workersTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
          if (rows.isEmpty()) {
            return false;
          }
          Row worker = rows.getFirst();
          worker.set("last_heartbeat_at", LocalDateTime.now());
          workersTable.update(worker);
          return true;
        });
  }

  /**
   * Detects and removes workers whose last heartbeat exceeds the stale threshold. Called lazily
   * from the job-list poll cycle. Nullifies worker_id on any jobs referencing stale workers and
   * removes their credentials.
   */
  public void expireStaleWorkers() {
    tx.tx(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table workersTable = schema.getTable("HpcWorkers");
          Table capTable = schema.getTable("HpcWorkerCapabilities");
          Table credentialsTable = schema.getTable("HpcWorkerCredentials");
          Table jobsTable = schema.getTable("HpcJobs");
          LocalDateTime cutoff = LocalDateTime.now().minusSeconds(STALE_HEARTBEAT_SECONDS);

          List<Row> allWorkers = workersTable.retrieveRows();
          for (Row worker : allWorkers) {
            LocalDateTime lastHeartbeat = DateTimeUtil.parse(worker.getString("last_heartbeat_at"));
            if (lastHeartbeat == null) continue;
            if (lastHeartbeat.isBefore(cutoff)) {
              String workerId = worker.getString("worker_id");
              logger.info("Removing stale worker {} (last heartbeat: {})", workerId, lastHeartbeat);

              // Delete capabilities
              List<Row> caps = capTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
              for (Row cap : caps) {
                capTable.delete(cap);
              }

              // Delete worker credentials
              List<Row> credentials =
                  credentialsTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
              for (Row credential : credentials) {
                credentialsTable.delete(credential);
              }

              // Nullify worker_id on associated jobs
              List<Row> jobs = jobsTable.where(f("worker_id", EQUALS, workerId)).retrieveRows();
              for (Row job : jobs) {
                job.set("worker_id", (String) null);
                jobsTable.update(job);
              }

              workersTable.delete(worker);
            }
          }
        });
  }
}
