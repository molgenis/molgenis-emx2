package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.emx2.sql.Constants.MG_ROLE_PREFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.jooq.CreateSchemaFinalStep;
import org.jooq.CreateTableColumnStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.molgenis.emx2.*;

public class MetadataUtils {

  private static final String MOLGENIS = "MOLGENIS";
  private static final String NOT_PROVIDED = "NOT_PROVIDED";
  // tables
  private static final org.jooq.Table SCHEMA_METADATA = table(name(MOLGENIS, "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name(MOLGENIS, "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name(MOLGENIS, "column_metadata"));
  private static final org.jooq.Table USERS_METADATA = table(name(MOLGENIS, "users_metadata"));
  private static final org.jooq.Table SETTINGS_METADATA =
      table(name(MOLGENIS, "settings_metadata"));

  // table
  private static final org.jooq.Field TABLE_SCHEMA =
      field(name("table_schema"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_NAME =
      field(name("table_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field TABLE_INHERITS =
      field(name("table_inherits"), VARCHAR.nullable(true));
  private static final org.jooq.Field TABLE_IMPORT_SCHEMA =
      field(name("import_schema"), VARCHAR.nullable(true));
  private static final org.jooq.Field TABLE_DESCRIPTION =
      field(name("table_description"), VARCHAR.nullable(true));
  private static final org.jooq.Field TABLE_JSONLD_TYPE =
      field(name("table_jsonld_type"), JSON.nullable(true));
  // column
  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field COLUMN_KEY =
      field(name("column_key"), INTEGER.nullable(true));
  private static final org.jooq.Field COLUMN_POSITION = field(name("column_position"), INTEGER);
  private static final org.jooq.Field COLUMN_DESCRIPTION =
      field(name("column_description"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_JSONLD_TYPE =
      field(name("column_jsonld_type"), JSON.nullable(true));
  private static final org.jooq.Field DATA_TYPE = field(name("data_type"), VARCHAR.nullable(false));
  private static final org.jooq.Field NULLABLE = field(name("nullable"), BOOLEAN.nullable(false));
  private static final org.jooq.Field REF_TABLE = field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field REF_SCHEMA =
      field(name("ref_schema"), VARCHAR.nullable(true));
  private static final org.jooq.Field REF_FROM =
      field(name("ref_from"), VARCHAR.getArrayDataType().nullable(true));
  private static final org.jooq.Field REF_TO =
      field(name("ref_to"), VARCHAR.getArrayDataType().nullable(true));
  private static final org.jooq.Field MAPPED_BY = field(name("mappedBy"), VARCHAR.nullable(true));
  private static final org.jooq.Field VALIDATION_SCRIPT =
      field(name("validationScript"), VARCHAR.nullable(true));
  private static final org.jooq.Field COMPUTE_SCRIPT =
      field(name("computeScript"), VARCHAR.nullable(true));
  private static final org.jooq.Field INDEXED = field(name("indexed"), BOOLEAN.nullable(true));
  private static final org.jooq.Field CASCADE_DELETE =
      field(name("cascade_delete"), BOOLEAN.nullable(true));

  // users
  private static final org.jooq.Field USER_NAME = field(name("username"), VARCHAR);
  private static final org.jooq.Field USER_PASS = field(name("password"), VARCHAR);

  // settings
  private static final org.jooq.Field SETTINGS_TABLE_NAME =
      field(
          name(TABLE_NAME.getName()),
          VARCHAR.nullable(true)); // note table might be null in case of schema
  private static final org.jooq.Field SETTINGS_NAME =
      field(name(org.molgenis.emx2.Constants.SETTINGS_NAME), VARCHAR);
  private static final org.jooq.Field SETTINGS_VALUE =
      field(name(org.molgenis.emx2.Constants.SETTINGS_VALUE), VARCHAR);

  // helper method
  private static ObjectMapper jsonMapper = new ObjectMapper();

  private MetadataUtils() {
    // to hide the public constructor
  }

  protected static void createMetadataSchemaIfNotExists(DSLContext jooq) {

    try (CreateSchemaFinalStep step = jooq.createSchemaIfNotExists(MOLGENIS)) {
      step.execute();
    }

    try (CreateTableColumnStep t = jooq.createTableIfNotExists(SCHEMA_METADATA)) {
      t.columns(TABLE_SCHEMA).constraint(primaryKey(TABLE_SCHEMA)).execute();
    }

    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", SCHEMA_METADATA);
    jooq.execute(
        "DROP POLICY IF EXISTS {0} ON {1}",
        name(SCHEMA_METADATA.getName() + "_POLICY"), SCHEMA_METADATA);
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(CONCAT({2},UPPER({3}),'/Viewer'),'MEMBER'))",
        name(SCHEMA_METADATA.getName() + "_POLICY"), SCHEMA_METADATA, MG_ROLE_PREFIX, TABLE_SCHEMA);
    // rowlevel secur the schema table

    // public access
    try (CreateTableColumnStep t = jooq.createTableIfNotExists(TABLE_METADATA)) {
      int result =
          t.columns(TABLE_SCHEMA, TABLE_NAME)
              .constraints(
                  primaryKey(TABLE_SCHEMA, TABLE_NAME),
                  foreignKey(TABLE_SCHEMA)
                      .references(SCHEMA_METADATA)
                      .onUpdateCascade()
                      .onDeleteCascade())
              .execute();
      if (result > 0) createRowLevelPermissions(jooq, TABLE_METADATA);
    }

    // this way more robust for non breaking changes
    for (Field field :
        new Field[] {TABLE_INHERITS, TABLE_IMPORT_SCHEMA, TABLE_DESCRIPTION, TABLE_JSONLD_TYPE}) {
      jooq.alterTable(TABLE_METADATA).addColumnIfNotExists(field).execute();
    }

    try (CreateTableColumnStep t = jooq.createTableIfNotExists(COLUMN_METADATA)) {
      int result =
          t.columns(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
              .constraints(
                  primaryKey(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME),
                  foreignKey(TABLE_SCHEMA, TABLE_NAME)
                      .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                      .onUpdateCascade()
                      .onDeleteCascade())
              .execute();
      if (result > 0) createRowLevelPermissions(jooq, COLUMN_METADATA);
    }

    // this way more robust for non-breaking changes
    for (Field field :
        new Field[] {
          DATA_TYPE,
          COLUMN_KEY,
          COLUMN_POSITION,
          NULLABLE,
          REF_SCHEMA,
          REF_TABLE,
          REF_TO,
          REF_FROM,
          MAPPED_BY,
          VALIDATION_SCRIPT,
          COMPUTE_SCRIPT,
          INDEXED,
          CASCADE_DELETE,
          COLUMN_DESCRIPTION,
          COLUMN_JSONLD_TYPE
        }) {
      jooq.alterTable(COLUMN_METADATA).addColumnIfNotExists(field).execute();
    }

    try (CreateTableColumnStep t = jooq.createTableIfNotExists(USERS_METADATA)) {
      t.columns(USER_NAME, USER_PASS).constraint(primaryKey(USER_NAME)).execute();
    }

    try (CreateTableColumnStep t = jooq.createTableIfNotExists(SETTINGS_METADATA)) {
      t.columns(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME, SETTINGS_VALUE)
          .constraint(primaryKey(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME))
          .execute();
    }

    jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name(MOLGENIS));
    jooq.execute("GRANT ALL ON ALL TABLES IN SCHEMA {0} TO PUBLIC", name(MOLGENIS));
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
        MG_ROLE_PREFIX,
        TABLE_SCHEMA);

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, {2} || upper({3}) || '/"
            + DefaultRoles.VIEWER
            + "', 'member'))",
        name("TABLE_RLS_" + DefaultRoles.VIEWER),
        table,
        MG_ROLE_PREFIX,
        TABLE_SCHEMA);
  }

  protected static void saveSchemaMetadata(DSLContext sql, SchemaMetadata schema) {
    try {
      sql.insertInto(SCHEMA_METADATA).columns(TABLE_SCHEMA).values(schema.getName()).execute();
    } catch (Exception e) {
      throw new MolgenisException("save of schema metadata failed", e);
    }
  }

  protected static Collection<String> loadSchemaNames(SqlDatabase db) {
    return db.getJooq().selectFrom(SCHEMA_METADATA).fetch().getValues(TABLE_SCHEMA, String.class);
  }

  protected static SchemaMetadata loadSchemaMetadata(DSLContext jooq, SchemaMetadata schema) {
    org.jooq.Record tableRecord =
        jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).fetchOne();
    if (tableRecord == null) {
      return schema;
    }
    return schema;
  }

  protected static void deleteSchema(DSLContext jooq, String schemaName) {
    jooq.deleteFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schemaName)).execute();
    jooq.deleteFrom(SETTINGS_METADATA).where(TABLE_SCHEMA.eq(schemaName)).execute();
  }

  protected static void saveTableMetadata(DSLContext jooq, TableMetadata table) {
    try {
      jooq.insertInto(TABLE_METADATA)
          .columns(
              TABLE_SCHEMA,
              TABLE_NAME,
              TABLE_INHERITS,
              TABLE_IMPORT_SCHEMA,
              TABLE_DESCRIPTION,
              TABLE_JSONLD_TYPE)
          .values(
              table.getSchema().getName(),
              table.getTableName(),
              table.getInherit(),
              table.getImportSchema(),
              table.getDescription(),
              table.getJsonldType())
          .onConflict(TABLE_SCHEMA, TABLE_NAME)
          .doUpdate()
          .set(TABLE_INHERITS, table.getInherit())
          .set(TABLE_IMPORT_SCHEMA, table.getImportSchema())
          .set(TABLE_DESCRIPTION, table.getDescription())
          .set(TABLE_JSONLD_TYPE, table.getJsonldType())
          .execute();
    } catch (Exception e) {
      throw new MolgenisException("save of table metadata failed", e);
    }
  }

  protected static Collection<TableMetadata> loadTables(DSLContext jooq, SchemaMetadata schema) {
    try {
      Map<String, TableMetadata> result = new LinkedHashMap<>();
      // tables
      List<org.jooq.Record> tableRecords =
          jooq.selectFrom(TABLE_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).fetch();
      for (org.jooq.Record r : tableRecords) {
        TableMetadata table = new TableMetadata(r.get(TABLE_NAME, String.class));
        table.setInherit(r.get(TABLE_INHERITS, String.class));
        table.setImportSchema(r.get(TABLE_IMPORT_SCHEMA, String.class));
        table.setDescription(r.get(TABLE_DESCRIPTION, String.class));
        table.setJsonldType(r.get(TABLE_JSONLD_TYPE, String.class));
        result.put(table.getTableName(), table);
      }

      // settings
      List<org.jooq.Record> settingRecords =
          jooq.selectFrom(SETTINGS_METADATA)
              .where(TABLE_SCHEMA.eq(schema.getName()), SETTINGS_TABLE_NAME.notEqual(NOT_PROVIDED))
              .fetch();
      for (org.jooq.Record r : settingRecords) {
        result
            .get(r.get(SETTINGS_TABLE_NAME, String.class))
            .setSetting(r.get(SETTINGS_NAME, String.class), r.get(SETTINGS_VALUE, String.class));
      }

      // columns
      List<org.jooq.Record> columnRecords =
          jooq.selectFrom(COLUMN_METADATA)
              .where(TABLE_SCHEMA.eq(schema.getName()))
              .orderBy(COLUMN_POSITION)
              .fetch();
      for (org.jooq.Record r : columnRecords) {
        result.get(r.get(TABLE_NAME, String.class)).add(recordToColumn(r));
      }
      return result.values();
    } catch (Exception e) {
      throw new MolgenisException("load of table metadata failed", e);
    }
  }

  protected static void deleteTable(DSLContext jooq, TableMetadata table) {
    jooq.deleteFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(table.getSchema().getName()), TABLE_NAME.eq(table.getTableName()))
        .execute();
    jooq.deleteFrom(SETTINGS_METADATA)
        .where(
            TABLE_SCHEMA.eq(table.getSchema().getName()),
            SETTINGS_TABLE_NAME.eq(table.getTableName()))
        .execute();
  }

  protected static void saveColumnMetadata(DSLContext jooq, Column column) {
    String refSchema =
        column.getRefSchema().equals(column.getSchemaName()) ? null : column.getRefSchema();
    jooq.insertInto(COLUMN_METADATA)
        .columns(
            TABLE_SCHEMA,
            TABLE_NAME,
            COLUMN_NAME,
            DATA_TYPE,
            COLUMN_KEY,
            COLUMN_POSITION,
            NULLABLE,
            REF_SCHEMA,
            REF_TABLE,
            REF_FROM,
            REF_TO,
            MAPPED_BY,
            VALIDATION_SCRIPT,
            COMPUTE_SCRIPT,
            INDEXED,
            CASCADE_DELETE,
            COLUMN_DESCRIPTION,
            COLUMN_JSONLD_TYPE)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getTableName(),
            column.getName(),
            column.getColumnType(),
            column.getKey(),
            column.getPosition(),
            column.isNullable(),
            refSchema,
            column.getRefTableName(),
            column.getRefFrom(),
            column.getRefTo(),
            column.getMappedBy(),
            column.getValidationScript(),
            column.getComputed(),
            column.isIndexed(),
            column.isCascadeDelete(),
            column.getDescription(),
            column.getJsonldType())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(DATA_TYPE, column.getColumnType())
        .set(COLUMN_KEY, column.getKey())
        .set(COLUMN_POSITION, column.getPosition())
        .set(NULLABLE, column.isNullable())
        .set(REF_SCHEMA, refSchema)
        .set(REF_TABLE, column.getRefTableName())
        .set(REF_FROM, column.getRefFrom())
        .set(REF_TO, column.getRefTo())
        .set(MAPPED_BY, column.getMappedBy())
        .set(VALIDATION_SCRIPT, column.getValidationScript())
        .set(COMPUTE_SCRIPT, column.getComputed())
        .set(INDEXED, column.isIndexed())
        .set(CASCADE_DELETE, column.isCascadeDelete())
        .set(COLUMN_DESCRIPTION, column.getDescription())
        .set(COLUMN_JSONLD_TYPE, column.getJsonldType())
        .execute();
  }

  protected static void deleteColumn(DSLContext jooq, Column column) {
    jooq.deleteFrom(COLUMN_METADATA)
        .where(
            TABLE_SCHEMA.eq(column.getSchemaName()),
            TABLE_NAME.eq(column.getTableName()),
            COLUMN_NAME.eq(column.getName()))
        .execute();
  }

  protected static List<Setting> loadSettings(DSLContext jooq, SchemaMetadata schema) {
    List<org.jooq.Record> settingRecords =
        jooq.selectFrom(SETTINGS_METADATA)
            .where(TABLE_SCHEMA.eq(schema.getName()), SETTINGS_TABLE_NAME.eq(NOT_PROVIDED))
            .fetch();
    List<Setting> settings = new ArrayList<>();
    for (org.jooq.Record record : settingRecords) {
      settings.add(
          new Setting(
              record.get(SETTINGS_NAME, String.class), record.get(SETTINGS_VALUE, String.class)));
    }
    return settings;
  }

  protected static void saveSetting(
      DSLContext jooq, SchemaMetadata schema, TableMetadata table, Setting setting) {
    try {
      jooq.insertInto(SETTINGS_METADATA)
          .columns(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME, SETTINGS_VALUE)
          .values(
              schema.getName(),
              table != null ? table.getTableName() : NOT_PROVIDED,
              setting.getKey(),
              setting.getValue())
          .onConflict(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME)
          .doUpdate()
          .set(SETTINGS_VALUE, setting.getValue())
          .execute();
    } catch (Exception e) {
      throw new MolgenisException("save of settings failed", e);
    }
  }

  protected static void deleteSetting(
      DSLContext jooq, SchemaMetadata schema, TableMetadata table, Setting setting) {
    jooq.deleteFrom(SETTINGS_METADATA)
        .where(
            TABLE_SCHEMA.eq(schema.getName()),
            table != null ? TABLE_NAME.eq(table.getTableName()) : TABLE_NAME.eq(NOT_PROVIDED),
            SETTINGS_NAME.eq(setting.getKey()))
        .execute();
  }

  protected static boolean schemaExists(DSLContext jooq, String name) {
    return jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(name)).fetch().isNotEmpty();
  }

  protected static List<Column> loadColumnMetadata(DSLContext jooq, TableMetadata table) {
    List<Column> columnList = new ArrayList<>();
    // load tables and columns
    Collection<org.jooq.Record> columnRecords =
        jooq.selectFrom(COLUMN_METADATA)
            .where(
                TABLE_SCHEMA.eq(table.getSchema().getName()), (TABLE_NAME).eq(table.getTableName()))
            .fetch();

    for (org.jooq.Record col : columnRecords) {
      columnList.add(new Column(table, recordToColumn(col)));
    }
    return columnList;
  }

  private static Column recordToColumn(org.jooq.Record col) {
    Column c = new Column(col.get(COLUMN_NAME, String.class));
    c.setType(ColumnType.valueOf(col.get(DATA_TYPE, String.class)));
    c.setNullable(col.get(NULLABLE, Boolean.class));
    c.setKey(col.get(COLUMN_KEY, Integer.class));
    c.setPosition(col.get(COLUMN_POSITION, Integer.class));
    c.setRefSchema(col.get(REF_SCHEMA, String.class));
    c.setRefTable(col.get(REF_TABLE, String.class));
    c.setRefFrom(col.get(REF_FROM, String[].class));
    c.setRefTo(col.get(REF_TO, String[].class));
    c.setMappedBy(col.get(MAPPED_BY, String.class));
    c.setValidationScript(col.get(VALIDATION_SCRIPT, String.class));
    c.setComputed(col.get(COMPUTE_SCRIPT, String.class));
    c.setDescription(col.get(COLUMN_DESCRIPTION, String.class));
    c.setCascadeDelete(col.get(CASCADE_DELETE, Boolean.class));
    c.setJsonldType(col.get(COLUMN_JSONLD_TYPE, String.class));
    return c;
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
    org.jooq.Record result =
        jooq.select(field("{0} = crypt({1}, {0})", USER_PASS, password).as("matches"))
            .from(USERS_METADATA)
            .where(field(USER_NAME).eq(username))
            .fetchOne();

    return result != null && result.get("matches", Boolean.class);
  }
}
