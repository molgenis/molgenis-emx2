package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.*;
import org.molgenis.utils.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.molgenis.Database.RowLevelSecurity.MG_EDIT_ROLE;
import static org.molgenis.Type.STRING;

public class MetadataUtils {

  // tables
  private static final org.jooq.Table SCHEMA_METADATA = table(name("MOLGENIS", "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name("MOLGENIS", "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name("MOLGENIS", "column_metadata"));
  private static final org.jooq.Table UNIQUE_METADATA = table(name("MOLGENIS", "unique_metadata"));

  // fields
  private static final org.jooq.Field TABLE_SCHEMA =
      field(name("table_schema"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_NAME =
      field(name("table_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));

  private static final org.jooq.Field DATA_TYPE = field(name("data_type"), VARCHAR.nullable(false));
  private static final org.jooq.Field NULLABLE = field(name("nullable"), BOOLEAN.nullable(false));
  private static final org.jooq.Field REF_TABLE = field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field REF_COLUMN =
      field(name("ref_column"), VARCHAR.nullable(true));
  private static final org.jooq.Field UNIQUE_COLUMNS =
      field(name("unique_columns"), VARCHAR.nullable(true).getArrayDataType());

  static void createMetadataSchemaIfNotExists(DSLContext jooq) throws MolgenisException {

    jooq.createSchemaIfNotExists("MOLGENIS").execute();
    jooq.createTableIfNotExists(SCHEMA_METADATA)
        .columns(TABLE_SCHEMA)
        .constraint(primaryKey(TABLE_SCHEMA))
        .execute();
    // public access

    jooq.createTableIfNotExists(TABLE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME)
        .constraints(
            primaryKey(TABLE_SCHEMA, TABLE_NAME),
            foreignKey(TABLE_SCHEMA)
                .references(SCHEMA_METADATA)
                .onUpdateCascade()
                .onDeleteCascade())
        .execute();
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", TABLE_METADATA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_MANAGER', 'member'))",
        name("TABLE_RLS_MANAGER"), TABLE_METADATA, TABLE_SCHEMA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_VIEWER', 'member'))",
        name("TABLE_RLS_VIEWER"), TABLE_METADATA, TABLE_SCHEMA);

    jooq.createTableIfNotExists(COLUMN_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, REF_TABLE, REF_COLUMN)
        .constraints(
            primaryKey(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME),
            foreignKey(TABLE_SCHEMA, TABLE_NAME)
                .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                .onUpdateCascade()
                .onDeleteCascade())
        .execute();
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", COLUMN_METADATA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_MANAGER', 'member'))",
        name("COLUMN_RLS_MANAGER"), COLUMN_METADATA, TABLE_SCHEMA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_VIEWER', 'member'))",
        name("COLUMN_RLS_VIEWER"), COLUMN_METADATA, TABLE_SCHEMA);

    jooq.createTableIfNotExists(UNIQUE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .constraints(
            primaryKey(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS),
            foreignKey(TABLE_SCHEMA, TABLE_NAME)
                .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                .onUpdateCascade()
                .onDeleteCascade())
        .execute();
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", UNIQUE_METADATA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_MANAGER', 'member'))",
        name("UNIQUE_RLS_MANAGER"), UNIQUE_METADATA, TABLE_SCHEMA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, 'MGROLE_' || upper({2}) || '_VIEWER', 'member'))",
        name("UNIQUE_RLS_VIEWER"), UNIQUE_METADATA, TABLE_SCHEMA);

    jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name("MOLGENIS"));
    jooq.execute("GRANT ALL ON ALL TABLES IN SCHEMA {0} TO PUBLIC", name("MOLGENIS"));
  }

  static void saveSchemaMetadata(DSLContext sql, Schema schema) {
    sql.insertInto(SCHEMA_METADATA)
        .columns(TABLE_SCHEMA)
        .values(schema.getName())
        .onConflict(TABLE_SCHEMA)
        .doNothing()
        .execute();
  }

  static Collection<String> loadSchemaNames(SqlDatabase db) {
    return db.getJooq().selectFrom(SCHEMA_METADATA).fetch().getValues(TABLE_SCHEMA, String.class);
  }

  static void deleteSchema(DSLContext sql, Schema schema) {
    sql.deleteFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).execute();
  }

  static Collection<String> loadTableNames(SqlSchema sqlSchema) {
    return sqlSchema
        .getJooq()
        .selectFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(sqlSchema.getName()))
        .fetch()
        .getValues(TABLE_NAME, String.class);
  }

  static void saveTableMetadata(SqlTable table) {
    table
        .getJooq()
        .insertInto(TABLE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME)
        .values(table.getSchemaName(), table.getName())
        .onConflict(TABLE_SCHEMA, TABLE_NAME)
        .doNothing()
        .execute();
  }

  static void deleteTable(SqlTable table) {
    table
        .getJooq()
        .deleteFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(table.getSchemaName()), TABLE_NAME.eq(table.getName()))
        .execute();
  }

  static void saveColumn(SqlColumn column) {
    column
        .getJooq()
        .insertInto(COLUMN_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, REF_TABLE, REF_COLUMN)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getName(),
            column.getName(),
            column.getDataType(),
            column.isNullable(),
            column.getRefTable(),
            column.getRefColumn())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(DATA_TYPE, column.getDataType())
        .set(NULLABLE, column.isNullable())
        .set(REF_TABLE, column.getRefTable())
        .set(REF_COLUMN, column.getRefColumn())
        .execute();
  }

  static void deleteColumn(DSLContext sql, Column column) {
    sql.deleteFrom(COLUMN_METADATA)
        .where(
            TABLE_SCHEMA.eq(column.getTable().getSchema()),
            TABLE_NAME.eq(column.getTable().getName()),
            COLUMN_NAME.eq(column.getName()))
        .execute();
  }

  static void saveUnique(DSLContext sql, Unique unique) {
    sql.insertInto(UNIQUE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .values(unique.getSchemaName(), unique.getTableName(), unique.getColumnNames())
        .onDuplicateKeyUpdate()
        .set(UNIQUE_COLUMNS, unique.getColumnNames())
        .execute();
  }

  static void deleteUnique(DSLContext sql, Unique unique) {
    sql.deleteFrom(UNIQUE_METADATA)
        .where(
            TABLE_SCHEMA.eq(unique.getSchemaName()),
            TABLE_NAME.eq(unique.getTableName()),
            UNIQUE_COLUMNS.eq(unique.getColumnNames()))
        .execute();
  }

  static boolean schemaExists(SqlSchema schema) {
    return schema
        .getJooq()
        .selectFrom(SCHEMA_METADATA)
        .where(TABLE_SCHEMA.eq(schema.getName()))
        .fetch()
        .isNotEmpty();
  }

  static void loadUniqueMetadata(SqlTable table, Map<String, Unique> uniqueMap) {}

  static void loadColumnMetadata(SqlTable table, Map<String, Column> columnMap)
      throws MolgenisException {
    StopWatch.print("begin load column metadata");

    // load tables and columns
    Collection<Record> columnRecords =
        table
            .getJooq()
            .selectFrom(COLUMN_METADATA)
            .where(TABLE_SCHEMA.eq(table.getSchemaName()), (TABLE_NAME).eq(table.getName()))
            .fetch();

    for (Record col : columnRecords) {
      String column_name = col.get(COLUMN_NAME, String.class);
      Type column_type = Type.valueOf(col.get(DATA_TYPE, String.class));
      Boolean nullable = col.get(NULLABLE, Boolean.class);
      String ref_table = col.get(REF_TABLE, String.class);
      String ref_column = col.get(REF_COLUMN, String.class);
      switch (column_type) {
        case REF:
          columnMap.put(
              column_name, new RefSqlColumn(table, column_name, ref_table, ref_column, nullable));
          break;
        case REF_ARRAY:
          columnMap.put(
              column_name,
              new RefArraySqlColumn(table, column_name, ref_table, ref_column, nullable));
          break;
        default:
          columnMap.put(column_name, new SqlColumn(table, column_name, column_type, nullable));
      }
    }
    StopWatch.print("load column metadata complete");
  }
}
