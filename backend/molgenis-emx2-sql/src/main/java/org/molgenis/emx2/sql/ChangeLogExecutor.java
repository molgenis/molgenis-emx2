package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
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
import org.jooq.Record;
import org.molgenis.emx2.Change;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class ChangeLogExecutor {

  private static final String MG_CHANGLOG = "mg_changelog";
  private static final org.jooq.Field OPERATION = field(name("operation"), CHAR(1).nullable(false));
  private static final org.jooq.Field STAMP = field(name("stamp"), TIMESTAMP.nullable(false));
  private static final org.jooq.Field USERID = field(name("userid"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLENAME = field(name("tablename"), VARCHAR.nullable(false));
  private static final org.jooq.Field OLD = field(name("old"), JSON.nullable(true));
  private static final org.jooq.Field NEW = field(name("new"), JSON.nullable(true));

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
      // setup trigger processing function
      db.getJooq()
          .execute(
              ChangeLogUtils.buildProcessAuditFunction(schema.getName(), table.getTableName()));

      // set audit trigger, logs insert, update and delete actions on table
      db.getJooq()
          .execute(ChangeLogUtils.buildAuditTrigger(schema.getName(), table.getTableName()));
    }
  }

  static void disableChangeLog(SqlDatabase db, SchemaMetadata schema) {

    // disable trigger for each table
    for (TableMetadata table : schema.getTables()) {
      // disable the trigger
      db.getJooq()
          .execute(ChangeLogUtils.buildAuditTriggerRemove(schema.getName(), table.getTableName()));
      // remove the trigger function
      db.getJooq()
          .execute(
              ChangeLogUtils.buildProcessAuditFunctionRemove(
                  schema.getName(), table.getTableName()));
    }

    // todo revoke grants for changelog table ?

    // remove the changelog data table
    removeChangeLogTable(db);
  }

  static void removeChangeLogTable(SqlDatabase db) {
    db.getJooq().dropTableIfExists(MG_CHANGLOG);
  }

  static List<Change> executeGetChanges(DSLContext jooq, SchemaMetadata schema, int limit) {
    if (!ChangeLogUtils.isChangeSchema(schema.getDatabase(), schema.getName())) {
      return Collections.emptyList();
    }
    List<Record> result =
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
    return jooq.fetchCount(table(name(schema.getName(), MG_CHANGLOG)));
  }

  static boolean isChangeLogEnabled(SqlDatabase db, SchemaMetadata schema) {
    return db.getJooq().fetchExists(table(name(schema.getName(), MG_CHANGLOG)));
  }
}
