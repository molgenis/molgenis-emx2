package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.SQLDataType.CHAR;
import static org.jooq.impl.SQLDataType.JSON;
import static org.jooq.impl.SQLDataType.TIMESTAMP;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.molgenis.emx2.Privileges.EDITOR;
import static org.molgenis.emx2.Privileges.MANAGER;
import static org.molgenis.emx2.Privileges.VIEWER;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.getRolePrefix;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.Result;
import org.molgenis.emx2.Change;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class ChangeLogExecutor {

  public static final String MG_CHANGLOG = "mg_changelog";
  private static final Field<String> OPERATION = field(name("operation"), CHAR(1).nullable(false));
  private static final Field<Timestamp> STAMP = field(name("stamp"), TIMESTAMP.nullable(false));
  private static final Field<String> USERID = field(name("userid"), VARCHAR.nullable(false));
  private static final Field<String> TABLENAME = field(name("tablename"), VARCHAR.nullable(false));
  private static final Field<org.jooq.JSON> OLD = field(name("old"), JSON.nullable(true));
  private static final Field<org.jooq.JSON> NEW = field(name("new"), JSON.nullable(true));

  private ChangeLogExecutor() {
    // hide
  }

  static void enableChangeLog(SqlDatabase db, SchemaMetadata schema) {
    // Create change log table
    org.jooq.Table<Record> changelogTable = table(name(schema.getName(), MG_CHANGLOG));
    db.getJooq()
        .createTableIfNotExists(changelogTable)
        .columns(OPERATION, STAMP, USERID, TABLENAME, OLD, NEW)
        .execute();

    // Grant rights to all to insert into change log, only admin and owner can read
    final String schemaName = schema.getName();
    final String member = getRolePrefix(schemaName) + VIEWER;
    final String editor = getRolePrefix(schemaName) + EDITOR;
    final String manager = getRolePrefix(schemaName) + MANAGER;

    db.getJooq()
        .execute(
            "GRANT INSERT ON {0} TO {1}, {2}, {3}",
            changelogTable, name(editor), name(manager), name(member));
    db.getJooq().execute("GRANT SELECT ON {0} TO {1}", changelogTable, name(manager));

    // Setup trigger for each table in schema
    for (TableMetadata table : schema.getTables()) {
      // drop previous version
      db.getJooq()
          .execute(ChangeLogUtils.buildAuditTriggerRemove(schema.getName(), table.getTableName()));
      // setup trigger processing function
      db.getJooq()
          .execute(
              ChangeLogUtils.buildProcessAuditFunction(schema.getName(), table.getTableName()));

      // set audit trigger, logs insert, update and delete actions on table
      db.getJooq()
          .execute(ChangeLogUtils.buildAuditTrigger(schema.getName(), table.getTableName()));
    }
  }

  static void executeDropChangeLogTableForSchema(SqlDatabase db, Schema schema) {
    db.getJooq().dropTableIfExists(table(name(schema.getName(), MG_CHANGLOG))).execute();
  }

  static void disableChangeLog(SqlDatabase db, SchemaMetadata schema) {
    // disable trigger for each table
    for (TableMetadata table : schema.getTables()) {
      // disable the trigger
      disableChangeLog(db, table);
    }
  }

  static void disableChangeLog(SqlDatabase db, TableMetadata table) {
    // remove trigger
    db.getJooq()
        .execute(
            ChangeLogUtils.buildAuditTriggerRemove(table.getSchemaName(), table.getTableName()));
    // remove the trigger function
    db.getJooq()
        .execute(
            ChangeLogUtils.buildProcessAuditFunctionRemove(
                table.getSchemaName(), table.getTableName()));
  }

  static List<Change> executeGetChanges(DSLContext jooq, SchemaMetadata schema, int limit) {
    if (!hasChangeLogTable(jooq, schema)) {
      return Collections.emptyList();
    }
    Result<Record6<String, Timestamp, String, String, org.jooq.JSON, org.jooq.JSON>> result =
        jooq.select(OPERATION, STAMP, USERID, TABLENAME, OLD, NEW)
            .from(table(name(schema.getName(), MG_CHANGLOG)))
            .orderBy(STAMP.desc())
            .limit(limit)
            .fetch();

    return result.stream()
        .map(
            r -> {
              char operation = r.getValue(OPERATION, char.class);
              Timestamp stamp = r.getValue(STAMP, Timestamp.class);
              String userId = r.getValue(USERID, String.class);
              String tableName = r.getValue(TABLENAME, String.class);
              String oldRowData = r.getValue(OLD, String.class);
              String newRowData = r.getValue(NEW, String.class);
              return new Change(operation, stamp, userId, tableName, oldRowData, newRowData);
            })
        .toList();
  }

  static Integer executeGetChangesCount(DSLContext jooq, SchemaMetadata schema) {
    if (hasChangeLogTable(jooq, schema)) {
      return jooq.fetchCount(table(name(schema.getName(), MG_CHANGLOG)));
    } else {
      // do not query db when changelog table does not exist
      return 0;
    }
  }

  static boolean hasChangeLogTable(DSLContext jooq, SchemaMetadata schema) {
    return jooq.fetchExists(
        select()
            .from(table(name("information_schema", "tables")))
            .where(field("table_schema").eq(schema.getName()))
            .and(field("table_name").eq(MG_CHANGLOG)));
  }
}
