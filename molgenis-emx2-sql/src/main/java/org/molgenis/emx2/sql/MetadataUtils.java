package org.molgenis.emx2.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;
import org.molgenis.emx2.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class MetadataUtils {
  // this demonstrates why we don't want to use jooq directly, but only as our interface
  // so ambition is to replace this with a pure molgenis api version.

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
  private static final org.jooq.Field TABLE_INHERITS =
      field(name("table_inherits"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));

  private static final org.jooq.Field DATA_TYPE = field(name("data_type"), VARCHAR.nullable(false));
  private static final org.jooq.Field NULLABLE = field(name("nullable"), BOOLEAN.nullable(false));
  private static final org.jooq.Field PKEY = field(name("pkey"), BOOLEAN.nullable(false));
  private static final org.jooq.Field REF_TABLE = field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field REF_COLUMN =
      field(name("ref_column"), VARCHAR.nullable(true));
  private static final org.jooq.Field VIA_COLUMN =
      field(name("via_column"), VARCHAR.nullable(true));
  private static final org.jooq.Field UNIQUE_COLUMNS =
      field(name("unique_columns"), VARCHAR.nullable(true).getArrayDataType());
  private static final org.jooq.Field INDEXED = field(name("indexed"), BOOLEAN.nullable(true));

  private MetadataUtils() {
    // to hide the public constructor
  }

  protected static void createMetadataSchemaIfNotExists(DSLContext jooq) {

    // if exists then skip
    if (!jooq.meta().getSchemas(MOLGENIS).isEmpty()) return;

    try (CreateSchemaFinalStep step = jooq.createSchemaIfNotExists(MOLGENIS)) {
      step.execute();

      jooq.createTableIfNotExists(SCHEMA_METADATA)
          .columns(TABLE_SCHEMA)
          .constraint(primaryKey(TABLE_SCHEMA))
          .execute();
      // public access

      jooq.createTableIfNotExists(TABLE_METADATA)
          .columns(TABLE_SCHEMA, TABLE_NAME, TABLE_INHERITS)
          .constraints(
              primaryKey(TABLE_SCHEMA, TABLE_NAME),
              foreignKey(TABLE_SCHEMA)
                  .references(SCHEMA_METADATA)
                  .onUpdateCascade()
                  .onDeleteCascade())
          .execute();

      jooq.createTableIfNotExists(COLUMN_METADATA)
          .columns(
              TABLE_SCHEMA,
              TABLE_NAME,
              COLUMN_NAME,
              DATA_TYPE,
              NULLABLE,
              PKEY,
              REF_TABLE,
              REF_COLUMN,
              //              REVERSE_REF_TABLE,
              //              REVERSE_REF_COLUMN,
              VIA_COLUMN,
              INDEXED)
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
    } catch (RuntimeException e) {
      throw e;
    }
  }

  private static void createRowLevelPermissions(DSLContext jooq, org.jooq.Table table) {
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
    // we record the role name in as a column 'table_rls_manager' and 'table_rls_viewer' and use
    // this to enforce policy of being able to change vs view table.
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2} || upper({3}) || '/"
            + DefaultRoles.MANAGER.toString()
            + "', 'member'))",
        name("TABLE_RLS_" + DefaultRoles.MANAGER),
        table,
        Constants.MG_ROLE_PREFIX,
        TABLE_SCHEMA);

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, {2} || upper({3}) || '/"
            + DefaultRoles.VIEWER
            + "', 'member'))",
        name("TABLE_RLS_" + DefaultRoles.VIEWER),
        table,
        Constants.MG_ROLE_PREFIX,
        TABLE_SCHEMA);
  }

  protected static void saveSchemaMetadata(DSLContext sql, SchemaMetadata schema) {
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

  protected static void deleteSchema(DSLContext jooq, SchemaMetadata schema) {
    jooq.deleteFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).execute();
  }

  protected static Collection<String> loadTableNames(DSLContext jooq, SchemaMetadata sqlSchema) {
    return jooq.selectFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(sqlSchema.getName()))
        .fetch()
        .getValues(TABLE_NAME, String.class);
  }

  protected static void saveTableMetadata(DSLContext jooq, TableMetadata table) {
    jooq.insertInto(TABLE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, TABLE_INHERITS)
        .values(table.getSchema().getName(), table.getTableName(), table.getInherit())
        .onConflict(TABLE_SCHEMA, TABLE_NAME)
        .doUpdate()
        .set(TABLE_INHERITS, table.getInherit())
        .execute();
  }

  protected static void loadTableMetadata(DSLContext jooq, TableMetadata table) {
    Record tableRecord =
        jooq.selectFrom(TABLE_METADATA)
            .where(
                TABLE_SCHEMA.eq(table.getSchema().getName()), (TABLE_NAME).eq(table.getTableName()))
            .fetchOne();
    if (tableRecord == null) {
      return;
    }
    table.setInherit(tableRecord.get(TABLE_INHERITS, String.class));
    for (Column c : MetadataUtils.loadColumnMetadata(jooq, table)) {
      table.addColumn(c);
    }
    MetadataUtils.loadUniqueMetadata(jooq, table);
  }

  protected static void deleteTable(DSLContext jooq, TableMetadata table) {
    jooq.deleteFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(table.getSchema().getName()), TABLE_NAME.eq(table.getTableName()))
        .execute();
  }

  protected static void saveColumnMetadata(DSLContext jooq, Column column) {
    jooq.insertInto(COLUMN_METADATA)
        .columns(
            TABLE_SCHEMA,
            TABLE_NAME,
            COLUMN_NAME,
            DATA_TYPE,
            NULLABLE,
            PKEY,
            REF_TABLE,
            REF_COLUMN,
            VIA_COLUMN,
            INDEXED)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getTableName(),
            column.getName(),
            column.getColumnType(),
            column.isNullable(),
            column.isPrimaryKey(),
            column.getRefTableName(),
            column.getRefColumnName(),
            //            column.getReverseRefTableName(),
            //            column.getReverseRefColumn(),
            column.getMappedBy(),
            column.isIndexed())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(DATA_TYPE, column.getColumnType())
        .set(NULLABLE, column.isNullable())
        .set(PKEY, column.isPrimaryKey())
        .set(REF_TABLE, column.getRefTableName())
        .set(REF_COLUMN, column.getRefColumnName())
        .set(VIA_COLUMN, column.getMappedBy())
        .set(INDEXED, column.isIndexed())
        .execute();
  }

  protected static void deleteColumn(DSLContext jooq, Column column) {
    jooq.deleteFrom(COLUMN_METADATA)
        .where(
            TABLE_SCHEMA.eq(column.getTable().getSchema().getName()),
            TABLE_NAME.eq(column.getTable().getTableName()),
            COLUMN_NAME.eq(column.getName()))
        .execute();
  }

  protected static void saveUnique(DSLContext jooq, TableMetadata table, String... columnNames) {
    jooq.insertInto(UNIQUE_METADATA)
        .columns(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .values(table.getSchema().getName(), table.getTableName(), columnNames)
        .onConflict(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS)
        .doNothing()
        .execute();
  }

  protected static void deleteUnique(DSLContext jooq, TableMetadata table, String... columnNames) {
    jooq.deleteFrom(UNIQUE_METADATA)
        .where(
            TABLE_SCHEMA.eq(table.getSchema().getName()),
            TABLE_NAME.eq(table.getTableName()),
            UNIQUE_COLUMNS.eq(columnNames))
        .execute();
  }

  protected static boolean schemaExists(DSLContext jooq, String name) {
    return jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(name)).fetch().isNotEmpty();
  }

  protected static void loadUniqueMetadata(DSLContext jooq, TableMetadata table) {
    List<Record> uniqueRecordList =
        jooq.selectFrom(UNIQUE_METADATA)
            .where(
                TABLE_SCHEMA.eq(table.getSchema().getName()), (TABLE_NAME).eq(table.getTableName()))
            .fetch();

    for (Record uniqueRecord : uniqueRecordList) {
      table.addUnique(uniqueRecord.get(UNIQUE_COLUMNS, String[].class));
    }
  }

  protected static List<Column> loadColumnMetadata(DSLContext jooq, TableMetadata table) {
    List<Column> columnList = new ArrayList<>();
    // load tables and columns
    Collection<Record> columnRecords =
        jooq.selectFrom(COLUMN_METADATA)
            .where(
                TABLE_SCHEMA.eq(table.getSchema().getName()), (TABLE_NAME).eq(table.getTableName()))
            .fetch();

    for (Record col : columnRecords) {
      Column c = new Column(col.get(COLUMN_NAME, String.class));
      c.type(ColumnType.valueOf(col.get(DATA_TYPE, String.class)));
      c.nullable(col.get(NULLABLE, Boolean.class));
      c.pkey(col.get(PKEY, Boolean.class));
      c.refTable(col.get(REF_TABLE, String.class));
      c.refColumn(col.get(REF_COLUMN, String.class));
      c.mappedBy(col.get(VIA_COLUMN, String.class));
      columnList.add(new Column(table, c));
    }
    return columnList;
  }
}
