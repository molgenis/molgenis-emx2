package org.molgenis.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.utils.StopWatch;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.molgenis.sql.SqlTable.MG_ROLE_PREFIX;

public class MetadataUtils {

  private static final String MOLGENIS = "MOLGENIS";

  // tables
  private static final org.jooq.Table SCHEMA_METADATA = table(name(MOLGENIS, "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name(MOLGENIS, "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name(MOLGENIS, "column_metadata"));
  private static final org.jooq.Table UNIQUE_METADATA = table(name(MOLGENIS, "unique_metadata"));

  // fields
  private static final org.jooq.Field TABLE_SCHEMA =
      field(name("table_schema"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_NAME =
      field(name("table_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_PRIMARYKEY =
      field(name(MOLGENIS, "table_primary_key"), SQLDataType.VARCHAR(255).getArrayDataType());

  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));

  private static final org.jooq.Field DATA_TYPE = field(name("data_type"), VARCHAR.nullable(false));
  private static final org.jooq.Field NULLABLE = field(name("nullable"), BOOLEAN.nullable(false));
  private static final org.jooq.Field REF_TABLE = field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field REF_COLUMN =
      field(name("ref_column"), VARCHAR.nullable(true));
  private static final org.jooq.Field UNIQUE_COLUMNS =
      field(name("unique_columns"), VARCHAR.nullable(true).getArrayDataType());

  private MetadataUtils() {
    // to hide the public constructor
  }

  protected static void createMetadataSchemaIfNotExists(DSLContext jooq) throws MolgenisException {

    try (CreateSchemaFinalStep step = jooq.createSchemaIfNotExists(MOLGENIS)) {
      step.execute();

      jooq.createTableIfNotExists(SCHEMA_METADATA)
          .columns(TABLE_SCHEMA)
          .constraint(primaryKey(TABLE_SCHEMA))
          .execute();
      // public access

      jooq.createTableIfNotExists(TABLE_METADATA)
          .columns(TABLE_SCHEMA, TABLE_NAME, TABLE_PRIMARYKEY)
          .constraints(
              primaryKey(TABLE_SCHEMA, TABLE_NAME),
              foreignKey(TABLE_SCHEMA)
                  .references(SCHEMA_METADATA)
                  .onUpdateCascade()
                  .onDeleteCascade())
          .execute();

      jooq.createTableIfNotExists(COLUMN_METADATA)
          .columns(
              TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, REF_TABLE, REF_COLUMN)
          .constraints(
              primaryKey(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME),
              foreignKey(TABLE_SCHEMA, TABLE_NAME)
                  .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                  .onUpdateCascade()
                  .onDeleteCascade())
          .execute();

      jooq.createTableIfNotExists(UNIQUE_METADATA)
          .columns(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
          .constraints(
              primaryKey(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS),
              foreignKey(TABLE_SCHEMA, TABLE_NAME)
                  .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                  .onUpdateCascade()
                  .onDeleteCascade())
          .execute();

      jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name(MOLGENIS));
      jooq.execute("GRANT ALL ON ALL TABLES IN SCHEMA {0} TO PUBLIC", name(MOLGENIS));
      createRowLevelPermissions(jooq, TABLE_METADATA);
      createRowLevelPermissions(jooq, COLUMN_METADATA);
      createRowLevelPermissions(jooq, UNIQUE_METADATA);
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
  }

  private static void createRowLevelPermissions(DSLContext jooq, org.jooq.Table table) {
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2} || upper({3}) || 'MANAGER', 'member'))",
        name("TABLE_RLS_MANAGER"), table, MG_ROLE_PREFIX, TABLE_SCHEMA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, {2} || upper({3}) || 'VIEWER', 'member'))",
        name("TABLE_RLS_VIEWER"), table, MG_ROLE_PREFIX, TABLE_SCHEMA);
  }

  protected static void saveSchemaMetadata(DSLContext sql, Schema schema) {
    sql.insertInto(SCHEMA_METADATA)
        .columns(TABLE_SCHEMA)
        .values(schema.getName())
        .onConflict(TABLE_SCHEMA)
        .doNothing()
        .execute();
  }

  protected static Collection<String> loadSchemaNames(SqlDatabase db) {
    return db.getJooq().selectFrom(SCHEMA_METADATA).fetch().getValues(TABLE_SCHEMA, String.class);
  }

  protected static void deleteSchema(DSLContext sql, Schema schema) {
    sql.deleteFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).execute();
  }

  protected static Collection<String> loadTableNames(SqlSchema sqlSchema) {
    return sqlSchema
        .getJooq()
        .selectFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(sqlSchema.getName()))
        .fetch()
        .getValues(TABLE_NAME, String.class);
  }

  protected static void saveTableMetadata(SqlTable table) {
    table
        .getJooq()
        .insertInto(TABLE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, TABLE_PRIMARYKEY)
        .values(table.getSchemaName(), table.getName(), table.getPrimaryKey())
        .onConflict(TABLE_SCHEMA, TABLE_NAME)
        .doUpdate()
        .set(TABLE_PRIMARYKEY, table.getPrimaryKey())
        .execute();
  }

  protected static void loadTableMetadata(SqlTable table) throws MolgenisException {
    // load tables metadata
    //   Collection<Record> columnRecords =
    Record tableRecord =
        table
            .getJooq()
            .selectFrom(TABLE_METADATA)
            .where(TABLE_SCHEMA.eq(table.getSchemaName()), (TABLE_NAME).eq(table.getName()))
            .fetchOne();
    String[] pkey = tableRecord.get(TABLE_PRIMARYKEY, String[].class);
    if (pkey.length > 0) table.loadPrimaryKey(pkey);
  }

  protected static void deleteTable(SqlTable table) {
    table
        .getJooq()
        .deleteFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(table.getSchemaName()), TABLE_NAME.eq(table.getName()))
        .execute();
  }

  protected static void saveColumnMetadata(SqlColumn column) throws MolgenisException {
    column
        .getJooq()
        .insertInto(COLUMN_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, DATA_TYPE, NULLABLE, REF_TABLE, REF_COLUMN)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getName(),
            column.getName(),
            column.getType(),
            column.isNullable(),
            column.getRefTable(),
            column.getRefColumn())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(DATA_TYPE, column.getType())
        .set(NULLABLE, column.isNullable())
        .set(REF_TABLE, column.getRefTable())
        .set(REF_COLUMN, column.getRefColumn())
        .execute();
  }

  protected static void deleteColumn(DSLContext sql, Column column) {
    sql.deleteFrom(COLUMN_METADATA)
        .where(
            TABLE_SCHEMA.eq(column.getTable().getSchema()),
            TABLE_NAME.eq(column.getTable().getName()),
            COLUMN_NAME.eq(column.getName()))
        .execute();
  }

  protected static void saveUnique(DSLContext sql, Unique unique) {
    sql.insertInto(UNIQUE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .values(unique.getSchemaName(), unique.getTableName(), unique.getColumnNames())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .doNothing()
        .execute();
  }

  protected static void deleteUnique(DSLContext sql, Unique unique) {
    sql.deleteFrom(UNIQUE_METADATA)
        .where(
            TABLE_SCHEMA.eq(unique.getSchemaName()),
            TABLE_NAME.eq(unique.getTableName()),
            UNIQUE_COLUMNS.eq(unique.getColumnNames()))
        .execute();
  }

  protected static boolean schemaExists(SqlSchema schema) {
    return schema
        .getJooq()
        .selectFrom(SCHEMA_METADATA)
        .where(TABLE_SCHEMA.eq(schema.getName()))
        .fetch()
        .isNotEmpty();
  }

  protected static void loadUniqueMetadata(SqlTable table) throws MolgenisException {
    List<Record> uniqueRecordList =
        table
            .getJooq()
            .selectFrom(UNIQUE_METADATA)
            .where(TABLE_SCHEMA.eq(table.getSchemaName()), (TABLE_NAME).eq(table.getName()))
            .fetch();

    for (Record uniqueRecord : uniqueRecordList) {
      table.loadUnique(uniqueRecord.get(UNIQUE_COLUMNS, String[].class));
    }
  }

  protected static void loadColumnMetadata(SqlTable table, Map<String, Column> columnMap)
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
      String columnName = col.get(COLUMN_NAME, String.class);
      Type columnType = Type.valueOf(col.get(DATA_TYPE, String.class));
      Boolean nullable = col.get(NULLABLE, Boolean.class);

      String toTable = col.get(REF_TABLE, String.class);
      String toColumn = col.get(REF_COLUMN, String.class);
      switch (columnType) {
        case REF:
          columnMap.put(
              columnName,
              new RefSqlColumn(table, columnName, toTable, toColumn).loadNullable(nullable));
          break;
        case REF_ARRAY:
          columnMap.put(
              columnName,
              new RefArraySqlColumn(table, columnName, toTable, toColumn).loadNullable(nullable));
          break;
        default:
          columnMap.put(
              columnName, new SqlColumn(table, columnName, columnType).loadNullable(nullable));
      }
    }
    StopWatch.print("load column metadata complete");
  }
}
