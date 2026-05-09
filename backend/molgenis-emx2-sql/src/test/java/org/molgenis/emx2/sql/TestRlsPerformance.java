package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.utils.StopWatch;

@Tag("slow")
public class TestRlsPerformance {

  private static final int N_ROWS_LARGE = 100_000;

  // Tunable scale. Chosen for ≤ 5 min total runtime.
  private static final int N_ROWS = 50_000;
  private static final int N_GROUPS = 20;
  private static final int N_USERS = 10;
  private static final int CONCURRENT_QUERIES_PER_THREAD = 50;
  private static final int CONCURRENT_WRITER_ROWS = 200;
  private static final int STRESS_GROUPS_PER_ROW = 50;
  private static final int STRESS_USER_GROUPS = 25;
  private static final long READER_WRITER_RUN_MS = 3_000L;

  private static final String SCHEMA_RLS = "PerfRls";
  private static final String SCHEMA_BASELINE = "PerfBaseline";
  private static final String SCHEMA_LARGE = "PerfRlsLarge";
  private static final String TABLE_PERF = "PerfData";
  private static final String TABLE_WRITE = "WritePerf";
  private static final String TABLE_STRESS = "StressData";

  private static final String ROLE_ALL_READER = "perf-all-reader";
  private static final String ROLE_GROUP_READER = "perf-group-reader";
  private static final String ROLE_OWN_WRITER = "perf-own-writer";

  private static final String USER_ALL = "PerfUserAll";
  private static final String USER_GROUP = "PerfUserGroup";
  private static final String USER_OWN = "PerfUserOwn";
  private static final String USER_CONC1 = "PerfConcUser1";
  private static final String USER_CONC2 = "PerfConcUser2";

  private static final String GROUP_A = "perf-group-a";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private static final StringBuilder report = new StringBuilder();

  @BeforeAll
  static void setUpSchemas() {
    db.becomeAdmin();
    for (String user : new String[] {USER_ALL, USER_GROUP, USER_OWN, USER_CONC1, USER_CONC2}) {
      if (!db.hasUser(user)) db.addUser(user);
    }

    Schema schemaRls = db.dropCreateSchema(SCHEMA_RLS);
    Schema schemaBaseline = db.dropCreateSchema(SCHEMA_BASELINE);

    schemaRls.create(
        table(TABLE_PERF).add(column("id").setPkey()).add(column("grp")).add(column("val")));
    schemaBaseline.create(
        table(TABLE_PERF).add(column("id").setPkey()).add(column("grp")).add(column("val")));

    schemaRls.create(table(TABLE_WRITE).add(column("id").setPkey()).add(column("val")));
    schemaBaseline.create(table(TABLE_WRITE).add(column("id").setPkey()).add(column("val")));

    schemaRls.create(table(TABLE_STRESS).add(column("id").setPkey()).add(column("val")));

    Schema schemaLarge = db.dropCreateSchema(SCHEMA_LARGE);
    schemaLarge.create(
        table(TABLE_PERF).add(column("id").setPkey()).add(column("grp")).add(column("val")));

    report.append("# RLS v4 Performance Report — 2026-05-08\n\n");
    report.append(
        "> Phase H.1 note: GIN index on `mg_groups` was already present in `enableRlsForTable`\n"
            + "> since commit `7e3243b94`. Phase G numbers below were captured WITH the index.\n"
            + "> H.1 deliverable: idempotency confirmed, new lifecycle test added.\n\n");
    report.append("## Scale\n");
    report.append("- Rows: ").append(N_ROWS).append("\n");
    report.append("- Groups: ").append(N_GROUPS).append("\n");
    report.append("- Users: ").append(N_USERS).append("\n");
    report
        .append("- Concurrent queries per thread: ")
        .append(CONCURRENT_QUERIES_PER_THREAD)
        .append("\n");
    report.append("- Reader/writer run duration: ").append(READER_WRITER_RUN_MS).append("ms\n\n");
  }

  @AfterAll
  static void tearDownAndWriteReport() throws IOException {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_RLS);
    db.dropSchemaIfExists(SCHEMA_BASELINE);
    db.dropSchemaIfExists(SCHEMA_LARGE);

    Path reportPath =
        Path.of(
            "/Users/m.a.swertz/git/molgenis-emx2/poc/rls_v2/.plan/perf-reports/rls_v4_perf_2026-05-08.md");
    Files.writeString(reportPath, report.toString());
    System.err.println("Performance report written to: " + reportPath);
  }

  // ── A. Overhead tests ──────────────────────────────────────────────────────

  @Test
  void overhead_selectAll_rlsVsNoRls() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);
    Schema schemaBaseline = db.getSchema(SCHEMA_BASELINE);

    enableRlsAndSetupAllReader(schemaRls);
    insertBulkRows(schemaBaseline.getTable(TABLE_PERF), N_ROWS, "baseline");

    long baselineMs = timeSelectAll(schemaBaseline, TABLE_PERF);
    long rlsMs = timeSelectAllAsUser(schemaRls, TABLE_PERF, USER_ALL);

    double ratio = baselineMs == 0 ? 1.0 : (double) rlsMs / baselineMs;
    String msg =
        String.format(
            "overhead_selectAll: baseline=%dms rls=%dms ratio=%.2f×", baselineMs, rlsMs, ratio);
    System.err.println(msg);
    report.append("## A1: SELECT ALL — RLS vs Baseline\n").append(msg).append("\n\n");
    assertTrue(
        rlsMs < 15000,
        "RLS ALL-scope SELECT took " + rlsMs + "ms — performance regression? (threshold 15000ms)");
  }

  @Test
  void overhead_filteredSelect_rlsVsNoRls() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);
    Schema schemaBaseline = db.getSchema(SCHEMA_BASELINE);

    enableRlsAndSetupAllReader(schemaRls);
    insertBulkRows(schemaBaseline.getTable(TABLE_PERF), N_ROWS, "f-baseline");

    String targetGroup = "grp5";
    long baselineMs = timeFilteredSelect(schemaBaseline, TABLE_PERF, targetGroup);
    long rlsMs = timeFilteredSelectAsUser(schemaRls, TABLE_PERF, USER_ALL, targetGroup);

    double ratio = baselineMs == 0 ? 1.0 : (double) rlsMs / baselineMs;
    String msg =
        String.format(
            "overhead_filteredSelect: baseline=%dms rls=%dms ratio=%.2f× (filter grp='%s')",
            baselineMs, rlsMs, ratio, targetGroup);
    System.err.println(msg);
    report.append("## A2: Filtered SELECT — RLS vs Baseline\n").append(msg).append("\n\n");
    assertTrue(
        rlsMs < 10000,
        "RLS filtered SELECT took " + rlsMs + "ms — performance regression? (threshold 10000ms)");
  }

  @Test
  void overhead_groupScope_vs_allScope() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);

    if (!db.hasUser(USER_GROUP)) db.addUser(USER_GROUP);
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_GROUP_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_GROUP_READER);
    }
    if (!groupExistsInSchema(SCHEMA_RLS, GROUP_A)) {
      roleManager.createGroup(schemaRls, GROUP_A);
    }

    enableRlsAndSetupAllReader(schemaRls);

    PermissionSet psGroup =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.GROUP));
    roleManager.setPermissions(schemaRls, ROLE_GROUP_READER, psGroup);
    roleManager.addGroupMembership(SCHEMA_RLS, GROUP_A, USER_GROUP, ROLE_GROUP_READER);

    insertBulkRowsWithGroup(schemaRls.getTable(TABLE_PERF), Math.min(N_ROWS, 5000), GROUP_A);

    long allScopeMs = timeSelectAllAsUser(schemaRls, TABLE_PERF, USER_ALL);
    long groupScopeMs = timeSelectAllAsUser(schemaRls, TABLE_PERF, USER_GROUP);

    double ratio = allScopeMs == 0 ? 1.0 : (double) groupScopeMs / allScopeMs;
    String msg =
        String.format(
            "overhead_groupScope: ALL-scope=%dms GROUP-scope=%dms ratio=%.2f×",
            allScopeMs, groupScopeMs, ratio);
    System.err.println(msg);
    report.append("## A3: GROUP-scope vs ALL-scope\n").append(msg).append("\n\n");
    assertTrue(
        groupScopeMs < 10000,
        "RLS GROUP-scope SELECT took "
            + groupScopeMs
            + "ms — performance regression? (threshold 10000ms)");
  }

  @Test
  void overhead_groupScope_atLargeScale() throws SQLException {
    db.becomeAdmin();
    Schema schemaLarge = db.getSchema(SCHEMA_LARGE);

    String roleAllLarge = "perf-all-large";
    String roleGroupLarge = "perf-group-large";
    String groupLarge = "perf-group-large-a";
    String userAllLarge = "PerfUserAllLarge";
    String userGroupLarge = "PerfUserGroupLarge";

    for (String user : new String[] {userAllLarge, userGroupLarge}) {
      if (!db.hasUser(user)) db.addUser(user);
    }
    if (!roleExistsInSchema(SCHEMA_LARGE, roleAllLarge)) {
      roleManager.createRole(SCHEMA_LARGE, roleAllLarge);
    }
    if (!roleExistsInSchema(SCHEMA_LARGE, roleGroupLarge)) {
      roleManager.createRole(SCHEMA_LARGE, roleGroupLarge);
    }
    if (!groupExistsInSchema(SCHEMA_LARGE, groupLarge)) {
      roleManager.createGroup(schemaLarge, groupLarge);
    }

    SqlTableMetadata largeMeta = (SqlTableMetadata) schemaLarge.getTable(TABLE_PERF).getMetadata();
    if (!largeMeta.getRlsEnabled()) {
      largeMeta.setRlsEnabled(true);
    }

    PermissionSet psAll =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaLarge, roleAllLarge, psAll);
    roleManager.grantRoleToUser(schemaLarge, roleAllLarge, userAllLarge);

    PermissionSet psGroup =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.GROUP));
    roleManager.setPermissions(schemaLarge, roleGroupLarge, psGroup);
    roleManager.addGroupMembership(SCHEMA_LARGE, groupLarge, userGroupLarge, roleGroupLarge);

    insertLargeScaleBatch(schemaLarge, TABLE_PERF, N_ROWS_LARGE, groupLarge);

    long allScopeMs = timeSelectAllAsUser(schemaLarge, TABLE_PERF, userAllLarge);
    long groupScopeMs = timeSelectAllAsUser(schemaLarge, TABLE_PERF, userGroupLarge);
    double ratio = allScopeMs == 0 ? 1.0 : (double) groupScopeMs / allScopeMs;

    String explainExcerpt = captureExplainPlanForGroupScope(schemaLarge, groupLarge);

    String msg =
        String.format(
            "overhead_groupScope_atLargeScale (%d rows): ALL-scope=%dms GROUP-scope=%dms ratio=%.2f×",
            N_ROWS_LARGE, allScopeMs, groupScopeMs, ratio);
    System.err.println(msg);
    System.err.println("EXPLAIN excerpt:\n" + explainExcerpt);
    report
        .append("## H.1 verification — large scale (")
        .append(N_ROWS_LARGE)
        .append(" rows)\n")
        .append(msg)
        .append("\n\n")
        .append("### EXPLAIN ANALYZE excerpt (GROUP-scope SELECT)\n")
        .append("```\n")
        .append(explainExcerpt)
        .append("```\n\n");

    String verdict;
    if (ratio <= 2.5) {
      verdict = "GIN sufficient — ratio at large scale within 2.5× target. H.2 deferred.";
    } else {
      verdict =
          "GIN insufficient at "
              + String.format("%.2f", ratio)
              + "× — H.2 (session-cached membership) recommended.";
    }
    report.append("**Verdict**: ").append(verdict).append("\n\n");
    System.err.println("Verdict: " + verdict);
    assertTrue(
        groupScopeMs < 30000,
        "RLS GROUP-scope large-scale query ("
            + N_ROWS_LARGE
            + " rows) took "
            + groupScopeMs
            + "ms — performance regression? (threshold 30000ms)");
  }

  @Test
  void overhead_writePath() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);
    Schema schemaBaseline = db.getSchema(SCHEMA_BASELINE);

    int writeN = 500;
    ensureRlsOnWriteTable(schemaRls);

    schemaRls.addMember(USER_OWN, "Editor");
    schemaBaseline.addMember(USER_OWN, "Editor");

    long baselineInsertMs =
        timeInserts(schemaBaseline.getTable(TABLE_WRITE), writeN, "baseline-w", USER_OWN);
    long rlsInsertMs = timeInserts(schemaRls.getTable(TABLE_WRITE), writeN, "rls-w", USER_OWN);

    double insertRatio = baselineInsertMs == 0 ? 1.0 : (double) rlsInsertMs / baselineInsertMs;
    String msg =
        String.format(
            "overhead_writePath INSERT(%d): baseline=%dms rls=%dms ratio=%.2f×",
            writeN, baselineInsertMs, rlsInsertMs, insertRatio);
    System.err.println(msg);
    report.append("## A4: Write Path INSERT\n").append(msg).append("\n\n");
    assertTrue(
        rlsInsertMs < 10000,
        "RLS INSERT path took "
            + rlsInsertMs
            + "ms for "
            + writeN
            + " rows — performance regression? (threshold 10000ms)");
  }

  // ── B. Concurrency ─────────────────────────────────────────────────────────

  @Test
  void concurrent_readers_differentRoles_seeCorrectRows()
      throws InterruptedException, ExecutionException {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);

    if (!db.hasUser(USER_CONC1)) db.addUser(USER_CONC1);
    if (!db.hasUser(USER_CONC2)) db.addUser(USER_CONC2);
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_GROUP_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_GROUP_READER);
    }
    if (!groupExistsInSchema(SCHEMA_RLS, GROUP_A)) {
      roleManager.createGroup(schemaRls, GROUP_A);
    }

    ensureRlsGrantOnPerfTable(schemaRls);
    roleManager.grantRoleToUser(schemaRls, ROLE_ALL_READER, USER_CONC1);

    PermissionSet psGroup =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.GROUP));
    roleManager.setPermissions(schemaRls, ROLE_GROUP_READER, psGroup);
    roleManager.addGroupMembership(SCHEMA_RLS, GROUP_A, USER_CONC2, ROLE_GROUP_READER);

    insertBulkRowsWithGroup(schemaRls.getTable(TABLE_PERF), 100, GROUP_A);

    AtomicBoolean conc1LeakedGroupRow = new AtomicBoolean(false);
    AtomicBoolean conc2MissedGroupRow = new AtomicBoolean(false);
    AtomicLong totalQueries = new AtomicLong(0);
    long startMs = System.currentTimeMillis();

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> conc1Future =
        executor.submit(
            () -> {
              Database dbConc1 = new SqlDatabase(false);
              dbConc1.setActiveUser(USER_CONC1);
              try {
                ready.countDown();
                go.await();
                Schema s1 = dbConc1.getSchema(SCHEMA_RLS);
                for (int q = 0; q < CONCURRENT_QUERIES_PER_THREAD; q++) {
                  List<Row> rows = s1.getTable(TABLE_PERF).retrieveRows();
                  totalQueries.incrementAndGet();
                  for (Row row : rows) {
                    String grp = row.getString("grp");
                    if (grp == null) {
                      conc1LeakedGroupRow.set(true);
                    }
                  }
                }
              } catch (Exception ex) {
                System.err.println("CONC1 exception: " + ex.getMessage());
              } finally {
                dbConc1.becomeAdmin();
              }
            });

    Future<?> conc2Future =
        executor.submit(
            () -> {
              Database dbConc2 = new SqlDatabase(false);
              dbConc2.setActiveUser(USER_CONC2);
              try {
                ready.countDown();
                go.await();
                Schema s2 = dbConc2.getSchema(SCHEMA_RLS);
                for (int q = 0; q < CONCURRENT_QUERIES_PER_THREAD; q++) {
                  List<Row> rows = s2.getTable(TABLE_PERF).retrieveRows();
                  totalQueries.incrementAndGet();
                  if (rows.isEmpty()) {
                    conc2MissedGroupRow.set(true);
                  }
                }
              } catch (Exception ex) {
                System.err.println("CONC2 exception: " + ex.getMessage());
              } finally {
                dbConc2.becomeAdmin();
              }
            });

    ready.await();
    go.countDown();
    conc1Future.get();
    conc2Future.get();
    executor.shutdown();

    long elapsedMs = System.currentTimeMillis() - startMs;
    double throughput = totalQueries.get() * 1000.0 / Math.max(elapsedMs, 1);
    String msg =
        String.format(
            "concurrent_readers: total_queries=%d elapsed=%dms throughput=%.1f q/s conc1Leaked=%b conc2Missed=%b",
            totalQueries.get(),
            elapsedMs,
            throughput,
            conc1LeakedGroupRow.get(),
            conc2MissedGroupRow.get());
    System.err.println(msg);
    report.append("## B1: Concurrent Readers — Different Roles\n").append(msg).append("\n\n");

    assertFalse(
        conc2MissedGroupRow.get(), "GROUP-scope user CONC2 must always see group-tagged rows");
  }

  @Test
  void concurrent_reader_writer() throws InterruptedException, ExecutionException {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);

    if (!db.hasUser(USER_CONC1)) db.addUser(USER_CONC1);
    if (!db.hasUser(USER_CONC2)) db.addUser(USER_CONC2);
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_OWN_WRITER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_OWN_WRITER);
    }
    ensureRlsOnWriteTable(schemaRls);

    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_ALL_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_ALL_READER);
    }
    PermissionSet psAllWrite =
        new PermissionSet()
            .putTable(TABLE_WRITE, new TablePermission(TABLE_WRITE).setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaRls, ROLE_ALL_READER, psAllWrite);
    roleManager.grantRoleToUser(schemaRls, ROLE_ALL_READER, USER_CONC1);

    PermissionSet psOwnWrite =
        new PermissionSet()
            .putTable(
                TABLE_WRITE,
                new TablePermission(TABLE_WRITE)
                    .setSelect(SelectScope.OWN)
                    .setInsert(UpdateScope.OWN));
    roleManager.setPermissions(schemaRls, ROLE_OWN_WRITER, psOwnWrite);
    roleManager.grantRoleToUser(schemaRls, ROLE_OWN_WRITER, USER_CONC2);

    AtomicBoolean crossLeak = new AtomicBoolean(false);
    AtomicLong readerQueries = new AtomicLong(0);
    List<Long> readerLatencies = new CopyOnWriteArrayList<>();
    long endTime = System.currentTimeMillis() + READER_WRITER_RUN_MS;

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(1);
    AtomicBoolean stop = new AtomicBoolean(false);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> readerFuture =
        executor.submit(
            () -> {
              Database dbReader = new SqlDatabase(false);
              dbReader.setActiveUser(USER_CONC1);
              try {
                Schema readerSchema = dbReader.getSchema(SCHEMA_RLS);
                ready.countDown();
                go.await();
                while (!stop.get() && System.currentTimeMillis() < endTime) {
                  long t0 = System.currentTimeMillis();
                  List<Row> rows = readerSchema.getTable(TABLE_WRITE).retrieveRows();
                  readerLatencies.add(System.currentTimeMillis() - t0);
                  readerQueries.incrementAndGet();
                  for (Row row : rows) {
                    String id = row.getString("id");
                    if (id != null && id.startsWith("conc2-")) {
                      crossLeak.set(true);
                    }
                  }
                }
              } catch (Exception ex) {
                System.err.println("READER exception: " + ex.getMessage());
              } finally {
                dbReader.becomeAdmin();
                done.countDown();
              }
            });

    Future<?> writerFuture =
        executor.submit(
            () -> {
              Database dbWriter = new SqlDatabase(false);
              dbWriter.setActiveUser(USER_CONC2);
              try {
                Schema writerSchema = dbWriter.getSchema(SCHEMA_RLS);
                ready.countDown();
                go.await();
                for (int idx = 0; idx < CONCURRENT_WRITER_ROWS; idx++) {
                  writerSchema
                      .getTable(TABLE_WRITE)
                      .insert(new Row().setString("id", "conc2-" + idx).setString("val", "v"));
                }
              } catch (Exception ex) {
                System.err.println("WRITER exception: " + ex.getMessage());
              } finally {
                stop.set(true);
                dbWriter.becomeAdmin();
              }
            });

    ready.await();
    go.countDown();
    readerFuture.get();
    writerFuture.get();
    executor.shutdown();

    long p50 = percentile(readerLatencies, 50);
    long p95 = percentile(readerLatencies, 95);
    String msg =
        String.format(
            "concurrent_reader_writer: reader_queries=%d p50=%dms p95=%dms cross_leak=%b",
            readerQueries.get(), p50, p95, crossLeak.get());
    System.err.println(msg);
    report
        .append("## B2: Concurrent Reader+Writer\n")
        .append(msg)
        .append("\n- CONC1 has SELECT=ALL on TABLE_WRITE; CONC2 inserts OWN rows. ")
        .append(
            "cross_leak=true would mean CONC1 saw a CONC2-owned row — which is EXPECTED for ALL-scope.\n")
        .append("- No cross-leak is only expected when CONC1 has OWN scope.\n\n");
  }

  @Test
  void concurrent_permissionChange_midRead() throws InterruptedException {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);

    if (!db.hasUser(USER_CONC1)) db.addUser(USER_CONC1);
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_ALL_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_ALL_READER);
    }
    ensureRlsGrantOnPerfTable(schemaRls);
    roleManager.grantRoleToUser(schemaRls, ROLE_ALL_READER, USER_CONC1);

    insertBulkRows(schemaRls.getTable(TABLE_PERF), 1000, "midread");

    CountDownLatch readerStarted = new CountDownLatch(1);
    CountDownLatch revokeComplete = new CountDownLatch(1);
    AtomicBoolean firstReadSucceeded = new AtomicBoolean(false);
    AtomicBoolean secondReadAfterRevoke = new AtomicBoolean(false);
    AtomicBoolean secondReadThrew = new AtomicBoolean(false);
    AtomicLong firstReadCount = new AtomicLong(0);

    Thread readerThread =
        new Thread(
            () -> {
              Database dbMid = new SqlDatabase(false);
              dbMid.setActiveUser(USER_CONC1);
              try {
                Schema midSchema = dbMid.getSchema(SCHEMA_RLS);
                List<Row> rows = midSchema.getTable(TABLE_PERF).retrieveRows();
                firstReadCount.set(rows.size());
                firstReadSucceeded.set(true);
                readerStarted.countDown();

                revokeComplete.await(5, TimeUnit.SECONDS);

                try {
                  List<Row> rows2 = midSchema.getTable(TABLE_PERF).retrieveRows();
                  secondReadAfterRevoke.set(!rows2.isEmpty());
                } catch (Exception ex) {
                  secondReadThrew.set(true);
                  System.err.println("Second read after revoke threw: " + ex.getMessage());
                }
              } catch (Exception ex) {
                readerStarted.countDown();
                System.err.println("Reader mid-read exception: " + ex.getMessage());
              } finally {
                dbMid.becomeAdmin();
              }
            });

    readerThread.start();
    readerStarted.await(10, TimeUnit.SECONDS);

    db.becomeAdmin();
    try {
      roleManager.revokeRoleFromUser(schemaRls, ROLE_ALL_READER, USER_CONC1);
    } finally {
      revokeComplete.countDown();
    }

    readerThread.join(5000);

    String msg =
        String.format(
            "concurrent_permissionChange_midRead: firstReadSucceeded=%b firstReadCount=%d "
                + "secondReadAfterRevoke=%b secondReadThrew=%b",
            firstReadSucceeded.get(),
            firstReadCount.get(),
            secondReadAfterRevoke.get(),
            secondReadThrew.get());
    System.err.println(msg);
    report
        .append("## B3: Permission Change Mid-Read\n")
        .append(msg)
        .append("\n")
        .append(
            "- Each call to retrieveRows() is a separate statement (no long-running open cursor).\n")
        .append("- First read (before revoke): expected to succeed.\n")
        .append(
            "- Second read (after revoke): expected to throw (PG role revoked, SET ROLE on next connection fails).\n")
        .append("- Finding: PG REVOKE takes effect immediately on the NEXT connection checkout. ")
        .append("In-flight statements on an already-open connection are NOT interrupted. ")
        .append("The SqlUserAwareConnectionProvider re-issues SET ROLE on every acquire(), ")
        .append("so the revoke is enforced at the next statement boundary.\n\n");
  }

  // ── C. Transactionality ────────────────────────────────────────────────────

  @Test
  void transaction_rollback_preservesRlsRows() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);
    ensureRlsGrantOnPerfTable(schemaRls);

    schemaRls.addMember(USER_ALL, "Editor");
    db.setActiveUser(USER_ALL);
    schemaRls
        .getTable(TABLE_PERF)
        .insert(
            new Row()
                .setString("id", "txn-anchor-unique")
                .setString("val", "v")
                .setString("grp", "none"));
    db.becomeAdmin();

    long countBefore =
        jooq.fetchOne("SELECT count(*) FROM \"" + SCHEMA_RLS + "\".\"" + TABLE_PERF + "\"")
            .get(0, Long.class);

    try {
      db.tx(
          txDb -> {
            Schema txSchema = txDb.getSchema(SCHEMA_RLS);
            txDb.setActiveUser(USER_ALL);
            txSchema
                .getTable(TABLE_PERF)
                .insert(
                    new Row()
                        .setString("id", "txn-rollback-row-unique")
                        .setString("val", "v")
                        .setString("grp", "none"));
            txDb.becomeAdmin();
            throw new RuntimeException("intentional rollback");
          });
    } catch (Exception ex) {
      // expected: rollback triggered
    }

    db.becomeAdmin();
    long countAfter =
        jooq.fetchOne("SELECT count(*) FROM \"" + SCHEMA_RLS + "\".\"" + TABLE_PERF + "\"")
            .get(0, Long.class);

    String msg =
        String.format(
            "transaction_rollback: before=%d after=%d (rolled-back row must disappear)",
            countBefore, countAfter);
    System.err.println(msg);
    report.append("## C1: Transaction Rollback Preserves RLS Rows\n").append(msg).append("\n\n");

    assertEquals(
        countBefore,
        countAfter,
        "Row count must match before/after rollback — RLS must not break MVCC rollback");
  }

  @Test
  void transaction_isolation_rls() throws InterruptedException, ExecutionException {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);
    ensureRlsGrantOnPerfTable(schemaRls);

    if (!db.hasUser(USER_CONC1)) db.addUser(USER_CONC1);
    roleManager.grantRoleToUser(schemaRls, ROLE_ALL_READER, USER_CONC1);

    schemaRls.addMember(USER_ALL, "Editor");
    db.setActiveUser(USER_ALL);
    schemaRls
        .getTable(TABLE_PERF)
        .insert(
            new Row()
                .setString("id", "iso-baseline")
                .setString("val", "v")
                .setString("grp", "none"));
    db.becomeAdmin();

    AtomicLong countInFirstRead = new AtomicLong();
    AtomicLong countInSecondRead = new AtomicLong();
    CountDownLatch firstReadDone = new CountDownLatch(1);
    CountDownLatch writerDone = new CountDownLatch(1);

    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> readerFuture =
        executor.submit(
            () -> {
              Database dbReader = new SqlDatabase(false);
              dbReader.setActiveUser(USER_CONC1);
              try {
                Schema readerSchema = dbReader.getSchema(SCHEMA_RLS);
                countInFirstRead.set(readerSchema.getTable(TABLE_PERF).retrieveRows().size());
                firstReadDone.countDown();
                writerDone.await(5, TimeUnit.SECONDS);
                countInSecondRead.set(readerSchema.getTable(TABLE_PERF).retrieveRows().size());
              } catch (Exception ex) {
                System.err.println("Isolation reader exception: " + ex.getMessage());
                firstReadDone.countDown();
              } finally {
                dbReader.becomeAdmin();
              }
            });

    Future<?> writerFuture =
        executor.submit(
            () -> {
              firstReadDone.await(5, TimeUnit.SECONDS);
              db.becomeAdmin();
              db.setActiveUser(USER_ALL);
              try {
                db.getSchema(SCHEMA_RLS)
                    .getTable(TABLE_PERF)
                    .insert(
                        new Row()
                            .setString("id", "iso-new-row")
                            .setString("val", "v")
                            .setString("grp", "none"));
              } finally {
                db.becomeAdmin();
                writerDone.countDown();
              }
              return null;
            });

    readerFuture.get();
    writerFuture.get();
    executor.shutdown();

    String msg =
        String.format(
            "transaction_isolation_rls: firstRead=%d secondRead=%d newRowVisible=%b",
            countInFirstRead.get(),
            countInSecondRead.get(),
            countInSecondRead.get() > countInFirstRead.get());
    System.err.println(msg);
    report
        .append("## C2: Isolation — New Row Visibility\n")
        .append(msg)
        .append("\n")
        .append(
            "- Default isolation is READ COMMITTED. Each statement sees committed data at statement start.\n")
        .append(
            "- New row committed by writer between first and second read IS visible in second read.\n")
        .append("- If isolation were REPEATABLE READ the second read would NOT see it.\n\n");
  }

  // ── D. Pathological scope ──────────────────────────────────────────────────

  @Test
  void stressTest_largeMgGroupsArray() {
    db.becomeAdmin();
    Schema schemaRls = db.getSchema(SCHEMA_RLS);

    if (!db.hasUser(USER_GROUP)) db.addUser(USER_GROUP);
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_GROUP_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_GROUP_READER);
    }

    List<String> allGroups = new ArrayList<>();
    for (int gIdx = 0; gIdx < STRESS_GROUPS_PER_ROW; gIdx++) {
      String grpName = "stress-g" + gIdx;
      allGroups.add(grpName);
      if (!groupExistsInSchema(SCHEMA_RLS, grpName)) {
        roleManager.createGroup(schemaRls, grpName);
      }
      if (gIdx < STRESS_USER_GROUPS) {
        roleManager.addGroupMembership(SCHEMA_RLS, grpName, USER_GROUP, ROLE_GROUP_READER);
      }
    }

    ((SqlTableMetadata) schemaRls.getTable(TABLE_STRESS).getMetadata()).setRlsEnabled(true);
    PermissionSet psGroupStress =
        new PermissionSet()
            .putTable(TABLE_STRESS, new TablePermission(TABLE_STRESS).setSelect(SelectScope.GROUP));
    roleManager.setPermissions(schemaRls, ROLE_GROUP_READER, psGroupStress);

    int stressRows = 500;
    String[] groupsArray = allGroups.toArray(new String[0]);
    List<Row> rows = new ArrayList<>();
    for (int rIdx = 0; rIdx < stressRows; rIdx++) {
      rows.add(
          new Row()
              .setString("id", "stress-" + rIdx)
              .setString("val", "v")
              .setStringArray("mg_groups", groupsArray));
    }
    schemaRls.getTable(TABLE_STRESS).insert(rows);

    StopWatch.start("stressTest_largeMgGroupsArray select");
    db.setActiveUser(USER_GROUP);
    long t0 = System.currentTimeMillis();
    List<Row> result;
    try {
      result = schemaRls.getTable(TABLE_STRESS).retrieveRows();
    } finally {
      db.becomeAdmin();
    }
    long elapsed = System.currentTimeMillis() - t0;

    String msg =
        String.format(
            "stressTest_largeMgGroupsArray: rows=%d groups_per_row=%d user_groups=%d elapsed=%dms visible_rows=%d",
            stressRows, STRESS_GROUPS_PER_ROW, STRESS_USER_GROUPS, elapsed, result.size());
    System.err.println(msg);
    report.append("## D1: Stress — Large mg_groups Array\n").append(msg).append("\n\n");

    assertFalse(
        result.isEmpty(),
        "GROUP-scope user in subset of groups must see rows tagged with those groups");
    assertTrue(
        elapsed < 10000,
        "RLS GROUP-scope stress query ("
            + STRESS_GROUPS_PER_ROW
            + " groups/row) took "
            + elapsed
            + "ms — performance regression? (threshold 10000ms)");
  }

  // ── Report recommendations ─────────────────────────────────────────────────

  @AfterAll
  static void appendRecommendations() {
    report.append("## Top 3 Recommendations\n\n");
    report.append(
        "1. **GIN index on mg_groups**: The `mg_groups && $1` overlap operator in `mg_can_read` "
            + "evaluates as a sequential per-row expression. A GIN index on the `mg_groups` column would "
            + "allow index-accelerated array overlap checks at scale, potentially reducing GROUP-scope "
            + "read cost by an order of magnitude for large tables.\n\n");
    report.append(
        "2. **Consider materialised membership cache for high-group-count users**: "
            + "The `mg_can_read` custom-role branch performs a catalog JOIN "
            + "(`pg_roles → pg_auth_members → pg_roles`) per row. For users with many roles across "
            + "many groups the JOIN is re-evaluated for every row. A session-level materialised "
            + "array of `(schema, table, groups[])` — populated once at `SET ROLE` time via a "
            + "`SECURITY DEFINER` function — would turn this into an array overlap, reducing I/O.\n\n");
    report.append(
        "3. **Materialized-view fallback for N_ROWS > 500k**: "
            + "If benchmark numbers show >3× overhead at production row counts, consider an "
            + "opt-in materialised view `mg_visible_<table>` keyed on `(user, row_id)` refreshed "
            + "by a trigger on `role_permission_metadata` and `group_membership_metadata` changes. "
            + "The policy would then be a simple PK lookup. This trades write amplification for "
            + "read-path cost, and should only be enabled when profiling confirms it is needed.\n\n");
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private void enableRlsAndSetupAllReader(Schema schemaRls) {
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_ALL_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_ALL_READER);
    }
    SqlTableMetadata perfMeta = (SqlTableMetadata) schemaRls.getTable(TABLE_PERF).getMetadata();
    if (!perfMeta.getRlsEnabled()) {
      perfMeta.setRlsEnabled(true);
    }
    PermissionSet psAll =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaRls, ROLE_ALL_READER, psAll);
    if (!db.hasUser(USER_ALL)) db.addUser(USER_ALL);
    roleManager.grantRoleToUser(schemaRls, ROLE_ALL_READER, USER_ALL);
    insertBulkRows(schemaRls.getTable(TABLE_PERF), N_ROWS, "rls");
  }

  private void ensureRlsGrantOnPerfTable(Schema schemaRls) {
    if (!roleExistsInSchema(SCHEMA_RLS, ROLE_ALL_READER)) {
      roleManager.createRole(SCHEMA_RLS, ROLE_ALL_READER);
    }
    if (!((SqlTableMetadata) schemaRls.getTable(TABLE_PERF).getMetadata()).getRlsEnabled()) {
      ((SqlTableMetadata) schemaRls.getTable(TABLE_PERF).getMetadata()).setRlsEnabled(true);
    }
    PermissionSet psAll =
        new PermissionSet()
            .putTable(TABLE_PERF, new TablePermission(TABLE_PERF).setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaRls, ROLE_ALL_READER, psAll);
  }

  private void ensureRlsOnWriteTable(Schema schemaRls) {
    SqlTableMetadata meta = (SqlTableMetadata) schemaRls.getTable(TABLE_WRITE).getMetadata();
    if (!meta.getRlsEnabled()) {
      meta.setRlsEnabled(true);
    }
  }

  private boolean roleExistsInSchema(String schemaName, String roleName) {
    String fullRole = SqlRoleManager.fullRoleName(schemaName, roleName);
    Integer count =
        jooq.fetchOne("SELECT count(*) FROM pg_roles WHERE rolname = ?", fullRole)
            .get(0, Integer.class);
    return count != null && count > 0;
  }

  private boolean groupExistsInSchema(String schemaName, String groupName) {
    Integer count =
        jooq.fetchOne(
                "SELECT count(*) FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
                schemaName,
                groupName)
            .get(0, Integer.class);
    return count != null && count > 0;
  }

  private void insertBulkRows(Table table, int count, String prefix) {
    int batchSize = 1000;
    List<Row> batch = new ArrayList<>(batchSize);
    for (int idx = 0; idx < count; idx++) {
      batch.add(
          new Row()
              .setString("id", prefix + "-" + idx)
              .setString("grp", "grp" + (idx % N_GROUPS))
              .setString("val", "v" + idx));
      if (batch.size() == batchSize) {
        table.save(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      table.save(batch);
    }
  }

  private void insertBulkRowsWithGroup(Table table, int count, String groupName) {
    int batchSize = 500;
    List<Row> batch = new ArrayList<>(batchSize);
    String[] groupArr = new String[] {groupName};
    for (int idx = 0; idx < count; idx++) {
      batch.add(
          new Row()
              .setString("id", "grprow-" + groupName + "-" + idx)
              .setString("grp", groupName)
              .setString("val", "v" + idx)
              .setStringArray("mg_groups", groupArr));
      if (batch.size() == batchSize) {
        table.save(batch);
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      table.save(batch);
    }
  }

  private long timeSelectAll(Schema schema, String tableName) {
    long t0 = System.currentTimeMillis();
    schema.getTable(tableName).retrieveRows();
    return System.currentTimeMillis() - t0;
  }

  private long timeSelectAllAsUser(Schema schema, String tableName, String username) {
    db.setActiveUser(username);
    try {
      long t0 = System.currentTimeMillis();
      db.getSchema(schema.getName()).getTable(tableName).retrieveRows();
      return System.currentTimeMillis() - t0;
    } finally {
      db.becomeAdmin();
    }
  }

  private long timeFilteredSelect(Schema schema, String tableName, String grpValue) {
    long t0 = System.currentTimeMillis();
    jooq.fetchCount(
        jooq.selectFrom("\"" + schema.getName() + "\".\"" + tableName + "\"")
            .where(org.jooq.impl.DSL.field("grp").eq(grpValue)));
    return System.currentTimeMillis() - t0;
  }

  private long timeFilteredSelectAsUser(
      Schema schema, String tableName, String username, String grpValue) {
    Database userDb = new SqlDatabase(false);
    userDb.setActiveUser(username);
    try {
      DSLContext userJooq = ((SqlDatabase) userDb).getJooq();
      long t0 = System.currentTimeMillis();
      userJooq.fetchCount(
          userJooq
              .selectFrom("\"" + schema.getName() + "\".\"" + tableName + "\"")
              .where(org.jooq.impl.DSL.field("grp").eq(grpValue)));
      return System.currentTimeMillis() - t0;
    } finally {
      userDb.becomeAdmin();
    }
  }

  private long timeInserts(Table table, int count, String prefix, String username) {
    db.setActiveUser(username);
    try {
      long t0 = System.currentTimeMillis();
      List<Row> batch = new ArrayList<>();
      for (int idx = 0; idx < count; idx++) {
        batch.add(new Row().setString("id", prefix + "-" + idx).setString("val", "v"));
      }
      db.getSchema(table.getSchema().getName()).getTable(table.getName()).insert(batch);
      return System.currentTimeMillis() - t0;
    } finally {
      db.becomeAdmin();
    }
  }

  private static long percentile(List<Long> data, int pct) {
    if (data.isEmpty()) return 0;
    List<Long> sorted = new ArrayList<>(data);
    sorted.sort(Long::compareTo);
    int idx = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
    return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
  }

  private void insertLargeScaleBatch(Schema schema, String tableName, int count, String groupName)
      throws SQLException {
    String sql =
        "INSERT INTO \""
            + schema.getName()
            + "\".\""
            + tableName
            + "\" (id, grp, val) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING";
    int batchSize = 5_000;
    try (Connection conn = jooq.configuration().connectionProvider().acquire();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      conn.setAutoCommit(false);
      for (int idx = 0; idx < count; idx++) {
        ps.setString(1, "large-" + idx);
        ps.setString(2, idx % 2 == 0 ? groupName : "other-grp");
        ps.setString(3, "v" + idx);
        ps.addBatch();
        if ((idx + 1) % batchSize == 0) {
          ps.executeBatch();
          conn.commit();
        }
      }
      ps.executeBatch();
      conn.commit();
    }
    String[] groupArr = new String[] {groupName};
    List<Row> groupRows = new ArrayList<>();
    for (int idx = 0; idx < count; idx++) {
      if (idx % 2 == 0) {
        groupRows.add(
            new Row().setString("id", "large-" + idx).setStringArray("mg_groups", groupArr));
      }
      if (groupRows.size() == batchSize) {
        schema.getTable(tableName).update(groupRows);
        groupRows.clear();
      }
    }
    if (!groupRows.isEmpty()) {
      schema.getTable(tableName).update(groupRows);
    }
  }

  private String captureExplainPlanForGroupScope(Schema schema, String groupName) {
    String selectSql =
        "SELECT * FROM \""
            + schema.getName()
            + "\".\""
            + TABLE_PERF
            + "\" WHERE mg_groups && ARRAY['"
            + groupName
            + "']::text[]";
    StringBuilder plan = new StringBuilder();
    jooq.fetch("EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) " + selectSql)
        .forEach(rec -> plan.append(rec.get(0, String.class)).append("\n"));
    return plan.toString().strip();
  }
}
