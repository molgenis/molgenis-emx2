// TODO: Slice 6d — integrate with Migrations.initOrMigrate and SOFTWARE_DATABASE_VERSION. Not wired
// up currently.
package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MigrationRunner {

  private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);
  private static final String MIGRATIONS_TABLE = "_migrations";
  private static final String COL_SUBSET = "subset";
  private static final String COL_SCRIPT = "script";

  private MigrationRunner() {}

  static void runMigrationsForSubset(
      DSLContext jooq, String schemaName, Path bundleSourceDir, String subsetName) {
    if (bundleSourceDir == null) return;

    Path migrationsDir = bundleSourceDir.resolve("migrations").resolve(subsetName);
    if (!Files.isDirectory(migrationsDir)) return;

    ensureMigrationsTable(jooq, schemaName);

    List<Path> scripts = collectSortedSqlScripts(migrationsDir);
    for (Path script : scripts) {
      String scriptName = script.getFileName().toString();
      if (hasRunMigration(jooq, schemaName, subsetName, scriptName)) {
        log.debug("Migration already applied: {}/{}", subsetName, scriptName);
        continue;
      }
      runScript(jooq, schemaName, subsetName, scriptName, script);
      recordMigration(jooq, schemaName, subsetName, scriptName);
    }
  }

  private static List<Path> collectSortedSqlScripts(Path migrationsDir) {
    try {
      return Files.list(migrationsDir)
          .filter(p -> p.getFileName().toString().endsWith(".sql"))
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new MolgenisException(
          "Failed to list migration scripts in " + migrationsDir + ": " + e.getMessage());
    }
  }

  private static void ensureMigrationsTable(DSLContext jooq, String schemaName) {
    jooq.execute(
        "CREATE TABLE IF NOT EXISTS {0} ("
            + "{1} TEXT NOT NULL, "
            + "{2} TEXT NOT NULL, "
            + "ran_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
            + "PRIMARY KEY ({1}, {2})"
            + ")",
        name(schemaName, MIGRATIONS_TABLE), name(COL_SUBSET), name(COL_SCRIPT));
  }

  private static boolean hasRunMigration(
      DSLContext jooq, String schemaName, String subsetName, String scriptName) {
    return 0
        < jooq.select(count())
            .from(table(name(schemaName, MIGRATIONS_TABLE)))
            .where(
                field(name(COL_SUBSET)).eq(subsetName).and(field(name(COL_SCRIPT)).eq(scriptName)))
            .fetchOne(0, Integer.class);
  }

  private static void runScript(
      DSLContext jooq, String schemaName, String subsetName, String scriptName, Path scriptPath) {
    String sql;
    try {
      sql = Files.readString(scriptPath);
    } catch (IOException e) {
      throw new MolgenisException(
          "Migration '" + subsetName + "/" + scriptName + "' could not be read: " + e.getMessage());
    }
    log.info("Running migration {}/{} on schema '{}'", subsetName, scriptName, schemaName);
    try {
      jooq.execute("SET search_path TO {0}", name(schemaName));
      jooq.execute(sql);
    } catch (Exception e) {
      throw new MolgenisException(
          "Migration '" + subsetName + "/" + scriptName + "' failed: " + e.getMessage());
    }
  }

  private static void recordMigration(
      DSLContext jooq, String schemaName, String subsetName, String scriptName) {
    jooq.execute(
        "INSERT INTO {0} ({1}, {2}) VALUES ({3}, {4})",
        name(schemaName, MIGRATIONS_TABLE),
        name(COL_SUBSET),
        name(COL_SCRIPT),
        subsetName,
        scriptName);
  }
}
