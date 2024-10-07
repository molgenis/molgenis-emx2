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
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import org.jooq.*;
import org.jooq.Record;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Schema;

public class ChangeLogExecutor {

  public static final String MG_CHANGLOG = "mg_changelog";
  private static final Field<String> OPERATION = field(name("operation"), CHAR(1).nullable(false));
  private static final Field<Timestamp> STAMP = field(name("stamp"), TIMESTAMP.nullable(false));
  private static final Field<String> USERID = field(name("userid"), VARCHAR.nullable(false));
  private static final Field<String> TABLENAME = field(name("tablename"), VARCHAR.nullable(false));
  private static final Field<org.jooq.JSON> OLD = field(name("old"), JSON.nullable(true));
  private static final Field<org.jooq.JSON> NEW = field(name("new"), JSON.nullable(true));

  private static final Field<String> SCHEMA_NAME =
      field(name("table_schema"), VARCHAR.nullable(false));

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

  static List<LastUpdate> executeLastUpdates(DSLContext jooq) {

    // get a list of schema's with changelogs, need due to limited support for cross schema queries
    List<String> schemasWithChangeLog = getSchemasWithChangeLog(jooq);

    // get the last updated table and details from all schema's that have a change log
    SelectLimitPercentStep<Record4<String, Timestamp, String, String>> query =
        jooq.select(OPERATION, STAMP, USERID, TABLENAME)
            .from(table(name(schemasWithChangeLog.get(0), MG_CHANGLOG)))
            .orderBy(STAMP.desc())
            .limit(1);
    // union the select for schema's in a loop
    for (String schemaName : schemasWithChangeLog) {
      query.unionAll(
          jooq.select(OPERATION, STAMP, USERID, TABLENAME)
              .from(table(name(schemaName, MG_CHANGLOG)))
              .orderBy(STAMP.desc())
              .limit(1));
    }

    // execute to query with all the unions
    Result<Record4<String, Timestamp, String, String>> result = query.fetch();

    // transform the result in to records
    List<LastUpdate> lastUpdatesWithOutSchemaName =
        result.stream()
            .map(
                r -> {
                  char operation = r.getValue(OPERATION, char.class);
                  Timestamp stamp = r.getValue(STAMP, Timestamp.class);
                  String userId = r.getValue(USERID, String.class);
                  String tableName = r.getValue(TABLENAME, String.class);

                  return new LastUpdate(operation, stamp, userId, tableName, "");
                })
            .toList();

    // join in the schema names with the records
    List<LastUpdate> lastUpdates =
        new java.util.ArrayList<>(
            IntStream.range(0, schemasWithChangeLog.size())
                .mapToObj(
                    i -> {
                      LastUpdate lastUpdate = lastUpdatesWithOutSchemaName.get(i);
                      return new LastUpdate(
                          lastUpdate.operation(),
                          lastUpdate.stamp(),
                          lastUpdate.userId(),
                          lastUpdate.tableName(),
                          schemasWithChangeLog.get(i));
                    })
                .toList());

    // sort the final result by timestamp ( i.e. last updated )
    lastUpdates.sort(Comparator.comparing(LastUpdate::stamp));

    return lastUpdates;
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

  static List<String> getSchemasWithChangeLog(DSLContext jooq) {
    Result<Record> result =
        jooq.select()
            .from(table(name("information_schema", "tables")))
            .where(field("table_name").eq(MG_CHANGLOG))
            .fetch();

    return result.stream().map(r -> r.getValue(SCHEMA_NAME, String.class)).toList();
  }
}
