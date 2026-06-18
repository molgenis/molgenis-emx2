package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ROLES;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

@Disabled
class TestRowLevelSecurityPerformance {

  private static Database database;
  private static final String SCHEMA = "TestRowLevelSecurityPerformance";
  private static final String ARTICLES = "Articles";
  private static final int ROW_COUNT = 50_000;
  private static final int WARMUP_RUNS = 2;
  private static final int MEASURED_RUNS = 5;

  private static final String USER_TEAM = "rls_perf_user_team";
  private static final String USER_VIEWER = "rls_perf_user_viewer";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_TEAM, USER_VIEWER)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(table(ARTICLES).add(column("id").setPkey()).add(column("title")));

    List<Row> rows = new ArrayList<>(ROW_COUNT);
    for (int i = 0; i < ROW_COUNT; i++) {
      rows.add(new Row().setString("id", "r" + i).setString("title", "row " + i));
    }
    schema.getTable(ARTICLES).insert(rows);
  }

  @Test
  void rlsEnableShouldNotBlowUpQueryTimes() {
    Schema adminSchema = database.getSchema(SCHEMA);

    long baselineNs = timeAdminSelect("baseline (no RLS)", adminSchema);

    database.becomeAdmin();
    adminSchema.createRole("PerfTeam");
    adminSchema.grant(
        "PerfTeam",
        new TablePermission(ARTICLES)
            .select(true)
            .insert(true)
            .update(true)
            .delete(true)
            .rowLevel(true));

    List<Row> stamped = new ArrayList<>(ROW_COUNT / 2);
    for (int i = 0; i < ROW_COUNT; i += 2) {
      stamped.add(new Row().setString("id", "r" + i).set(MG_ROLES, new String[] {"PerfTeam"}));
    }
    adminSchema.getTable(ARTICLES).update(stamped);

    adminSchema.addMember(USER_TEAM, "PerfTeam");
    adminSchema.addMember(USER_VIEWER, Privileges.VIEWER.toString());

    long adminWithRlsNs = timeAdminSelect("admin (RLS on, BYPASSRLS)", adminSchema);
    long viewerNs = timeAsUser("viewer (constant bypass policy)", USER_VIEWER);
    long teamNs = timeAsUser("team member (per-row mg_roles match)", USER_TEAM);

    System.out.printf(
        "%nRLS perf summary (%d rows, median of %d runs):%n", ROW_COUNT, MEASURED_RUNS);
    report("baseline", baselineNs);
    report("admin+RLS", adminWithRlsNs);
    report("viewer", viewerNs);
    report("team", teamNs);
    System.out.printf("admin+RLS / baseline   = %.2fx%n", ratio(adminWithRlsNs, baselineNs));
    System.out.printf("viewer    / baseline   = %.2fx%n", ratio(viewerNs, baselineNs));
    System.out.printf("team      / baseline   = %.2fx%n", ratio(teamNs, baselineNs));

    assertTrue(
        ratio(adminWithRlsNs, baselineNs) < 5.0,
        "admin should not pay more than 5x for RLS being enabled (BYPASSRLS short-circuits)");
    assertTrue(
        ratio(viewerNs, baselineNs) < 5.0,
        "viewer should not pay per-row cost — bypass branch must fold to a constant");
    assertTrue(
        ratio(teamNs, baselineNs) < 20.0,
        "row-match user should be within 20x of baseline at this row count");
  }

  private long timeAdminSelect(String label, Schema schema) {
    return time(label, () -> schema.getTable(ARTICLES).retrieveRows().size());
  }

  private long timeAsUser(String label, String user) {
    long[] holder = new long[1];
    database.setActiveUser(user);
    try {
      database.tx(
          db ->
              holder[0] =
                  time(label, () -> db.getSchema(SCHEMA).getTable(ARTICLES).retrieveRows().size()));
    } finally {
      database.becomeAdmin();
    }
    return holder[0];
  }

  private long time(String label, Supplier<Integer> query) {
    for (int i = 0; i < WARMUP_RUNS; i++) query.get();
    long[] samples = new long[MEASURED_RUNS];
    int rowsSeen = 0;
    for (int i = 0; i < MEASURED_RUNS; i++) {
      long t0 = System.nanoTime();
      rowsSeen = query.get();
      samples[i] = System.nanoTime() - t0;
    }
    long median = median(samples);
    System.out.printf("  %-42s rows=%-6d median=%6.1f ms%n", label, rowsSeen, median / 1_000_000.0);
    return median;
  }

  private static long median(long[] xs) {
    long[] copy = xs.clone();
    java.util.Arrays.sort(copy);
    return copy[copy.length / 2];
  }

  private static double ratio(long a, long b) {
    return b == 0 ? Double.POSITIVE_INFINITY : (double) a / (double) b;
  }

  private static void report(String label, long ns) {
    System.out.printf("  %-12s %6.1f ms%n", label, ns / 1_000_000.0);
  }
}
