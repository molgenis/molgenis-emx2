package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlDatabase.TEN_SECONDS;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.MG_TABLECLASS_UPDATE;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.*;
import org.jooq.Record;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrations {
  // version the current software needs to work
  private static final int SOFTWARE_DATABASE_VERSION = 18;
  public static final int THREE_MINUTES = 180;
  private static Logger logger = LoggerFactory.getLogger(Migrations.class);

  public static synchronized void initOrMigrate(SqlDatabase db) {
    db.tx(
        // transaction ensures migration succeeds completely or is rolled back completely
        tdb -> {
          int version = MetadataUtils.getVersion(db.getJooq());

          // -1 getVersion indicates schema does not exist
          if (version < 0) MetadataUtils.init(((SqlDatabase) tdb).getJooq());

          // migration steps to update from a previous version to SOFTWARE_DATABASE_VERSION
          // idea is that all steps need to be run when initializing empty server
          // this ensures 'new' and 'updated' servers are equal and same logic is not specified 2x
          // (one for 'new' and one for 'migration')
          if (version < 1)
            executeMigrationFile(
                tdb,
                "migration1.sql",
                "database migration: MOLGENIS.version_metadata tracks int database version number checks against code instead of string version number from released jar");
          if (version < 2)
            executeMigrationFile(
                tdb,
                "migration2.sql",
                "database migration: role names are made case-sensitive matching schema names, to fix issue where roles where conflicting between schemas with same uppercase(name)");
          if (version < 3)
            executeMigrationFile(
                tdb,
                "migration3.sql",
                "database migration: add description column to MOLGENIS.schema_metadata to store schema description");
          if (version < 4)
            executeMigrationFile(
                tdb,
                "migration4.sql",
                "database migration: add MOLGENIS.table_metadata.table_type");

          if (version < 5) {
            migration5addMgTableclassUpdateTrigger((SqlDatabase) tdb);
            logger.debug(
                "Updated all tables to have mg_tableclass trigger to prevent accidental overwrite of subclass records with same primary key value");
          }

          if (version < 6)
            executeMigrationFile(
                tdb, "migration5.sql", "database migration: add MOLGENIS.column_metadata.readonly");

          // if cannot migrate then throw a MolgenisException. This happens in case of breaking
          // change for database backend.
          if (version < 7)
            executeMigration7(
                (SqlDatabase) tdb,
                "refactor settings, rename molgenis_version table to database_metadata");

          if (version < 8) executeMigrationFile(tdb, "migration8.sql", "add labels column");

          if (version < 9)
            executeMigrationFile(
                tdb, "migration9.sql", "update row level policy for schema metadata");

          if (version < 10)
            executeMigrationFile(tdb, "migration10.sql", "add Aggregate role for each schema");

          if (version < 11) executeMigrationFile(tdb, "migration11.sql", "add profile metadata");

          if (version < 12)
            executeMigrationFile(tdb, "migration12.sql", "add defaultValue in metadata schema");

          if (version < 13)
            executeMigrationFile(
                tdb, "migration13.sql", "remove default value for mg_tableclass columns");

          if (version < 14) {
            executeMigrationFile(tdb, "migration14.sql", "cleanup column_metadata ref column");
          }

          if (version < 15) {
            executeMigrationFile(tdb, "migration15.sql", "changed required field to varchar");
          }

          if (version < 16) {
            executeMigrationFile(tdb, "migration16.sql", "make required nullable");
          }

          if (version < 17) {
            executeMigrationFile(tdb, "migration17.sql", "add fkey to inherit table");
          }

          if (version < 18) {
            executeMigrationFile(
                tdb, "migration18.sql", "add filename to tables contain FILE datatype");
          }

          // if success, update version to SOFTWARE_DATABASE_VERSION
          updateDatabaseVersion((SqlDatabase) tdb, SOFTWARE_DATABASE_VERSION);
        });
  }

  private static void executeMigration7(SqlDatabase tdb, String message) {
    DSLContext jooq = tdb.getJooq();
    // rename table
    jooq.alterTableIfExists(table(name(MOLGENIS, "version_metadata")))
        .renameTo(table(name(MOLGENIS, "database_metadata")))
        .execute();

    // add settings columns
    jooq.alterTableIfExists(table(name(MOLGENIS, "database_metadata")))
        .addColumn(SETTINGS)
        .execute();
    jooq.alterTableIfExists(table(name(MOLGENIS, "schema_metadata"))).addColumn(SETTINGS).execute();
    jooq.alterTableIfExists(table(name(MOLGENIS, "table_metadata"))).addColumn(SETTINGS).execute();
    jooq.alterTableIfExists(table(name(MOLGENIS, "users_metadata"))).addColumn(SETTINGS).execute();
    // todo: do we also want column settings?

    // copy settings table into schema and table respectively
    Result<Record> previousSettings =
        jooq.selectFrom(table(name(MOLGENIS, "settings_metadata"))).fetch();

    // t.columns(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME, SETTINGS_VALUE)
    final org.jooq.Field SETTINGS_NAME =
        field(name(org.molgenis.emx2.Constants.SETTINGS_NAME), VARCHAR);
    final org.jooq.Field SETTINGS_TABLE_NAME =
        field(
            name(TABLE_NAME.getName()),
            VARCHAR.nullable(true)); // note table might be null in case of schema
    final org.jooq.Field SETTINGS_VALUE =
        field(name(org.molgenis.emx2.Constants.SETTINGS_VALUE), VARCHAR);

    for (Record record : previousSettings) {
      String schemaName = record.get(TABLE_SCHEMA, String.class);
      String tableName = record.get(SETTINGS_TABLE_NAME, String.class);
      String key = record.get(SETTINGS_NAME, String.class);
      String value = record.get(SETTINGS_VALUE, String.class);
      logger.info("migrating setting " + schemaName + "." + tableName + "." + key + "=xxxxx");
      if (schemaName == null || schemaName.equals(NOT_PROVIDED)) {
        // database level setting
        tdb.setSetting(key, value);
      } else {
        Schema schema = tdb.getSchema(schemaName);
        if (tableName == null || tableName.equals(NOT_PROVIDED)) {
          // schema level setting
          schema.getMetadata().setSetting(key, value);
        } else {
          // table level setting
          Table table = schema.getTable(tableName);
          table.getMetadata().setSetting(key, value);
        }
      }
    }

    // delete settings table

    logger.debug(message);
  }

  static void executeMigrationFile(Database db, String sqlFile, String message) {
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    try {
      jooq.settings().setQueryTimeout(THREE_MINUTES);
      String sql = new String(Migrations.class.getResourceAsStream(sqlFile).readAllBytes());
      jooq.execute(sql);
      logger.debug(message + "(file = " + sqlFile);
    } catch (IOException e) {
      throw new MolgenisException("Execute migration failed", e);
    } finally {
      jooq.settings().setQueryTimeout(TEN_SECONDS);
    }
  }

  private static void updateDatabaseVersion(SqlDatabase db, int newVersion) {
    MetadataUtils.setVersion(db.getJooq(), newVersion);
  }

  static void migration5addMgTableclassUpdateTrigger(SqlDatabase db) {
    // should add trigger to all root tables, identfied by having MG_TABLECLASS column
    DSLContext jooq = db.getJooq();
    // all schemas, except MOLGENIS
    for (org.jooq.Table table : jooq.meta().getTables()) {
      for (Field field : table.fields()) {
        if (field.getName().equals(MG_TABLECLASS)) {
          // rewrite of createMgTableClassCannotUpdateCheck
          // such that it doesn't use the metadata schema because that gives errors if migration is
          // not yet executed, savvy?
          String functionName = table.getName() + MG_TABLECLASS_UPDATE;
          List<TableField> keyFields = ((UniqueKey) table.getKeys().get(0)).getFields();
          String keyColumns =
              keyFields.stream()
                  .map(keyField -> keyField.getName())
                  .collect(Collectors.joining(","));
          String keyValues =
              keyFields.stream()
                  .map(keyField -> "OLD." + name(keyField.getName()))
                  .collect(Collectors.joining("||','||"));

          jooq.execute("DROP TRIGGER IF EXISTS {0} ON {1};", name(functionName), table);
          jooq.execute(
              "DROP FUNCTION IF EXISTS {0}", name(table.getSchema().getName(), functionName));

          jooq.execute(
              "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ "
                  + "\nBEGIN"
                  + "\n\tIF OLD.{1} <> NEW.{1} THEN"
                  + "\n\t\tRAISE EXCEPTION USING ERRCODE='23505'"
                  + ", MESSAGE = 'insert or update on table ' || NEW.{1} || ' violates primary key constraint'"
                  + ", DETAIL = 'Duplicate key: ('||{2}||')=('|| {3} ||') already exists in inherited table ' || OLD.{1};"
                  + "\n\tEND IF;"
                  + "\n\tRETURN NEW;"
                  + "\nEND; $BODY$ LANGUAGE plpgsql;",
              name(table.getSchema().getName(), functionName),
              name(MG_TABLECLASS),
              inline(keyColumns),
              keyword(keyValues));

          jooq.execute(
              "CREATE CONSTRAINT TRIGGER {0} "
                  + "\n\tAFTER UPDATE OF {1} ON {2}"
                  + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                  + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
              name(functionName),
              name(MG_TABLECLASS),
              table,
              name(table.getSchema().getName(), functionName));

          logger.info(
              "migration 5 added mg_tableclass update trigger for table " + table.getName());
        }
      }
    }
  }
}
