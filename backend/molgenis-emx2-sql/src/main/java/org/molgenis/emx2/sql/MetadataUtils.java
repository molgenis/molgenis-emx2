package org.molgenis.emx2.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.CreateTableColumnStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.emx2.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;

public class MetadataUtils {

  private static final String MOLGENIS = "MOLGENIS";

  // tables
  private static final org.jooq.Table SCHEMA_METADATA = table(name(MOLGENIS, "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name(MOLGENIS, "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name(MOLGENIS, "column_metadata"));
  private static final org.jooq.Table USERS_METADATA = table(name(MOLGENIS, "users_metadata"));

  // fields
  private static final org.jooq.Field TABLE_SCHEMA =
      field(name("table_schema"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_NAME =
      field(name("table_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_INHERITS =
      field(name("table_inherits"), VARCHAR.nullable(true));
  private static final org.jooq.Field TABLE_DESCRIPTION =
      field(name("table_description"), VARCHAR.nullable(true));

  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field COLUMN_KEY =
      field(name("column_key"), INTEGER.nullable(true));
  private static final org.jooq.Field COLUMN_DESCRIPTION =
      field(name("column_description"), VARCHAR.nullable(true));

  private static final org.jooq.Field DATA_TYPE = field(name("data_type"), VARCHAR.nullable(false));
  private static final org.jooq.Field NULLABLE = field(name("nullable"), BOOLEAN.nullable(false));
  private static final org.jooq.Field REF_TABLE = field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field MAPPED_BY = field(name("mappedBy"), VARCHAR.nullable(true));
  private static final org.jooq.Field VALIDATION_SCRIPT =
      field(name("validationScript"), VARCHAR.nullable(true));

  private static final org.jooq.Field INDEXED = field(name("indexed"), BOOLEAN.nullable(true));
  private static final org.jooq.Field CASCADE_DELETE =
      field(name("cascade_delete"), BOOLEAN.nullable(true));

  private static final org.jooq.Field USER_NAME = field(name("username"), VARCHAR);
  private static final org.jooq.Field USER_PASS = field(name("password"), VARCHAR);

  private MetadataUtils() {
    // to hide the public constructor
  }

  protected static void createMetadataSchemaIfNotExists(DSLContext jooq) {

    // if exists then skip
    if (!jooq.meta().getSchemas(MOLGENIS).isEmpty()) return;

    try (CreateSchemaFinalStep step = jooq.createSchemaIfNotExists(MOLGENIS)) {
      step.execute();

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(SCHEMA_METADATA)) {
        t.columns(TABLE_SCHEMA).constraint(primaryKey(TABLE_SCHEMA)).execute();
      }
      // public access

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(TABLE_METADATA)) {
        t.columns(TABLE_SCHEMA, TABLE_NAME, TABLE_INHERITS, TABLE_DESCRIPTION)
            .constraints(
                primaryKey(TABLE_SCHEMA, TABLE_NAME),
                foreignKey(TABLE_SCHEMA)
                    .references(SCHEMA_METADATA)
                    .onUpdateCascade()
                    .onDeleteCascade())
            .execute();
      }

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(COLUMN_METADATA)) {
        t.columns(
                TABLE_SCHEMA,
                TABLE_NAME,
                COLUMN_NAME,
                DATA_TYPE,
                COLUMN_KEY,
                NULLABLE,
                REF_TABLE,
                MAPPED_BY,
                VALIDATION_SCRIPT,
                INDEXED,
                CASCADE_DELETE,
                COLUMN_DESCRIPTION)
            .constraints(
                primaryKey(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME),
                foreignKey(TABLE_SCHEMA, TABLE_NAME)
                    .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                    .onUpdateCascade()
                    .onDeleteCascade())
            .execute();
      }

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(USERS_METADATA)) {
        t.columns(USER_NAME, USER_PASS).constraint(primaryKey(USER_NAME)).execute();
      }

      jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name(MOLGENIS));
      jooq.execute("GRANT ALL ON ALL TABLES IN SCHEMA {0} TO PUBLIC", name(MOLGENIS));
      createRowLevelPermissions(jooq, TABLE_METADATA);
      createRowLevelPermissions(jooq, COLUMN_METADATA);
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
        .columns(TABLE_SCHEMA, TABLE_NAME, TABLE_INHERITS, TABLE_DESCRIPTION)
        .values(
            table.getSchema().getName(),
            table.getTableName(),
            table.getInherit(),
            table.getDescription())
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
    table.setDescription(tableRecord.get(TABLE_DESCRIPTION, String.class));
    for (Column c : MetadataUtils.loadColumnMetadata(jooq, table)) {
      table.add(c);
    }
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
            COLUMN_KEY,
            NULLABLE,
            REF_TABLE,
            MAPPED_BY,
            VALIDATION_SCRIPT,
            INDEXED,
            CASCADE_DELETE,
            COLUMN_DESCRIPTION)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getTableName(),
            column.getName(),
            column.getColumnType(),
            column.getKey(),
            column.isNullable(),
            column.getRefTableName(),
            column.getMappedBy(),
            column.getValidation(),
            column.isIndexed(),
            column.isCascadeDelete(),
            column.getDescription())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(DATA_TYPE, column.getColumnType())
        .set(COLUMN_KEY, column.getKey())
        .set(NULLABLE, column.isNullable())
        .set(REF_TABLE, column.getRefTableName())
        .set(MAPPED_BY, column.getMappedBy())
        .set(VALIDATION_SCRIPT, column.getValidation())
        .set(INDEXED, column.isIndexed())
        .set(CASCADE_DELETE, column.isCascadeDelete())
        .set(COLUMN_DESCRIPTION, column.getDescription())
        .execute();
  }

  protected static void deleteColumn(DSLContext jooq, Column column) {
    jooq.deleteFrom(COLUMN_METADATA)
        .where(
            TABLE_SCHEMA.eq(column.getTable().getSchemaName()),
            TABLE_NAME.eq(column.getTableName()),
            COLUMN_NAME.eq(column.getName()))
        .execute();
  }

  protected static boolean schemaExists(DSLContext jooq, String name) {
    return jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(name)).fetch().isNotEmpty();
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
      c.key(col.get(COLUMN_KEY, Integer.class));
      c.refTable(col.get(REF_TABLE, String.class));
      c.mappedBy(col.get(MAPPED_BY, String.class));
      c.validation(col.get(VALIDATION_SCRIPT, String.class));
      c.setDescription(col.get(COLUMN_DESCRIPTION, String.class));
      c.cascadeDelete(col.get(CASCADE_DELETE, Boolean.class));
      columnList.add(new Column(table, c));
    }
    return columnList;
  }

  public static void setUserPassword(DSLContext jooq, String user, String password) {
    jooq.insertInto(USERS_METADATA)
        .columns(USER_NAME, USER_PASS)
        .values(user, field("crypt({0}, gen_salt('bf'))", password))
        .onConflict(USER_NAME)
        .doUpdate()
        .set(USER_PASS, field("crypt({0}, gen_salt('bf'))", password))
        .execute();
  }

  public static boolean checkUserPassword(DSLContext jooq, String username, String password) {
    Record result =
        jooq.select(field("{0} = crypt({1}, {0})", USER_PASS, password).as("matches"))
            .from(USERS_METADATA)
            .where(field(USER_NAME).eq(username))
            .fetchOne();

    return result != null && result.get("matches", Boolean.class);
  }
}
