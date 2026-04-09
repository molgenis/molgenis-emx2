package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.hpc.HpcFields.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.HpcTables;
import org.molgenis.emx2.sql.SqlDatabase;

/**
 * Worker registration, capability matching, and heartbeat management. Workers register on startup
 * and send periodic heartbeats. The service matches pending jobs to workers based on advertised
 * (processor, profile) capabilities.
 */
public class WorkerService {

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
          Table workersTable = schema.getTable(HpcTables.WORKERS);
          Table capTable = schema.getTable(HpcTables.WORKER_CAPABILITIES);

          LocalDateTime now = LocalDateTime.now();

          // Upsert worker
          List<Row> existing = workersTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          Row workerRow;
          if (existing.isEmpty()) {
            workerRow =
                row(
                    WORKER_ID, workerId,
                    HOSTNAME, hostname,
                    REGISTERED_AT, now,
                    LAST_HEARTBEAT_AT, now);
            workersTable.insert(workerRow);
          } else {
            workerRow = existing.getFirst();
            workerRow.set(HOSTNAME, hostname);
            if (workerRow.getString(REGISTERED_AT) == null
                || workerRow.getString(REGISTERED_AT).isBlank()) {
              workerRow.set(REGISTERED_AT, now);
            }
            workerRow.set(LAST_HEARTBEAT_AT, now);
            workersTable.update(workerRow);
          }

          // Replace capabilities: delete old, insert new
          List<Row> oldCaps = capTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          for (Row old : oldCaps) {
            capTable.delete(old);
          }

          if (capabilities != null) {
            int idx = 0;
            for (Map<String, Object> cap : capabilities) {
              capTable.insert(
                  row(
                      ID, workerId + "-cap-" + idx++,
                      WORKER_ID, workerId,
                      PROCESSOR, cap.get(PROCESSOR),
                      PROFILE, cap.get(PROFILE),
                      MAX_CONCURRENT_JOBS, cap.get(MAX_CONCURRENT_JOBS)));
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
          Table workersTable = schema.getTable(HpcTables.WORKERS);

          List<Row> rows = workersTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row worker = rows.getFirst();

          // Delete capabilities (FK dependency)
          Table capTable = schema.getTable(HpcTables.WORKER_CAPABILITIES);
          List<Row> caps = capTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          for (Row cap : caps) {
            capTable.delete(cap);
          }

          // Delete worker credentials
          Table credentialsTable = schema.getTable(HpcTables.WORKER_CREDENTIALS);
          List<Row> credentials =
              credentialsTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          for (Row credential : credentials) {
            credentialsTable.delete(credential);
          }

          // Nullify worker_id on any jobs referencing this worker
          Table jobsTable = schema.getTable(HpcTables.JOBS);
          List<Row> jobs = jobsTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          for (Row job : jobs) {
            job.set(WORKER_ID, (String) null);
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
          Table workersTable = db.getSchema(systemSchemaName).getTable(HpcTables.WORKERS);
          List<Row> rows = workersTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
          if (rows.isEmpty()) {
            return false;
          }
          Row worker = rows.getFirst();
          worker.set(LAST_HEARTBEAT_AT, LocalDateTime.now());
          workersTable.update(worker);
          return true;
        });
  }
}
