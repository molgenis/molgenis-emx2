package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.emx2.sql.Constants.MG_ROLE_PREFIX;

import java.util.*;
import org.jooq.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.User;
import org.molgenis.emx2.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataUtils {
  private static Logger logger = LoggerFactory.getLogger(MetadataUtils.class);

  private static final String MOLGENIS = "MOLGENIS";
  private static final String NOT_PROVIDED = "NOT_PROVIDED";
  // tables
  private static final org.jooq.Table VERSION_METADATA = table(name(MOLGENIS, "version_metadata"));
  private static final org.jooq.Table SCHEMA_METADATA = table(name(MOLGENIS, "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name(MOLGENIS, "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name(MOLGENIS, "column_metadata"));
  private static final org.jooq.Table USERS_METADATA = table(name(MOLGENIS, "users_metadata"));
  private static final org.jooq.Table SETTINGS_METADATA =
      table(name(MOLGENIS, "settings_metadata"));

  // version
  private static final org.jooq.Field VERSION_ID = field(name("id"), INTEGER.nullable(false));
  private static final org.jooq.Field VERSION = field(name("version"), VARCHAR.nullable(false));

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
  private static final org.jooq.Field TALBE_SEMANTICS =
      field(name("table_semantics"), VARCHAR.getArrayDataType().nullable(true));

  // column
  private static final org.jooq.Field COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));
  private static final org.jooq.Field COLUMN_KEY = field(name("key"), INTEGER.nullable(true));
  private static final org.jooq.Field COLUMN_POSITION = field(name("position"), INTEGER);
  private static final org.jooq.Field COLUMN_DESCRIPTION =
      field(name("description"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_VISIBLE =
      field(name("visible"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_FORMAT = field(name("format"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_SEMANTICS =
      field(name("columnSemantics"), VARCHAR.nullable(true).getArrayType());
  private static final org.jooq.Field COLUMN_TYPE =
      field(name("columnType"), VARCHAR.nullable(false));
  private static final org.jooq.Field COLUMN_REQUIRED =
      field(name("required"), BOOLEAN.nullable(false));
  private static final org.jooq.Field COLUMN_REF_TABLE =
      field(name("ref_table"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_REF_SCHEMA =
      field(name("ref_schema"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_REF_LINK =
      field(name("ref_link"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_REF_LABEL =
      field(name("refLabel"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_REF_BACK =
      field(name("refBack"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_VALIDATION =
      field(name("validation"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_COMPUTED =
      field(name("computed"), VARCHAR.nullable(true));
  private static final org.jooq.Field COLUMN_INDEXED =
      field(name("indexed"), BOOLEAN.nullable(true));
  private static final org.jooq.Field COLUMN_CASCADE =
      field(name("cascade"), BOOLEAN.nullable(true));

  // users
  private static final org.jooq.Field USER_NAME = field(name("username"), VARCHAR);
  private static final org.jooq.Field USER_PASS = field(name("password"), VARCHAR);

  // settings
  private static final org.jooq.Field SETTINGS_NAME =
      field(name(org.molgenis.emx2.Constants.SETTINGS_NAME), VARCHAR);
  private static final org.jooq.Field SETTINGS_TABLE_NAME =
      field(
          name(TABLE_NAME.getName()),
          VARCHAR.nullable(true)); // note table might be null in case of schema
  private static final org.jooq.Field SETTINGS_VALUE =
      field(name(org.molgenis.emx2.Constants.SETTINGS_VALUE), VARCHAR);

  private MetadataUtils() {
    // to hide the public constructor
  }

  protected static synchronized String getVersion(DSLContext jooq) {
    try {
      Result<Record> result = jooq.selectFrom(VERSION_METADATA).fetch();
      if (result.size() > 0) {
        return (String) result.get(0).get(VERSION);
      }
    } catch (Exception e) {
      // nothing
    }
    return null;
  }

  // should never run in parallel
  protected static synchronized void init(DSLContext jooq) {

    // wait a little to ensure in tests, they are not created on same time
    try {
      Thread.sleep((long) Math.random() * 1000); // NOSONAR
    } catch (InterruptedException e) {
      // should never happen
      Thread.currentThread().interrupt();
    }

    if (jooq.meta().getSchemas(MOLGENIS).size() == 0) {
      logger.info("INITIALIZING MOLGENIS METADATA SCHEMA");

      try (CreateSchemaFinalStep step = jooq.createSchemaIfNotExists(MOLGENIS)) {
        step.execute();
      }
      jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name(MOLGENIS));
      jooq.execute(
          "ALTER DEFAULT PRIVILEGES IN SCHEMA {0} GRANT ALL ON  TABLES  TO PUBLIC", name(MOLGENIS));

      // set version
      try (CreateTableColumnStep t = jooq.createTableIfNotExists(VERSION_METADATA)) {
        t.columns(VERSION_ID, VERSION).constraints(primaryKey(VERSION_ID)).execute();
        jooq.insertInto(VERSION_METADATA, VERSION_ID, VERSION)
            .values(1, Version.getSpecificationVersion())
            .onConflict(VERSION_ID)
            .doUpdate()
            .set(VERSION, Version.getSpecificationVersion())
            .execute();
      }

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(SCHEMA_METADATA)) {
        t.columns(TABLE_SCHEMA).constraint(primaryKey(TABLE_SCHEMA)).execute();

        jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", SCHEMA_METADATA);

        jooq.execute(
            "DROP POLICY IF EXISTS {0} ON {1}",
            name(SCHEMA_METADATA.getName() + "_POLICY"), SCHEMA_METADATA);
        jooq.execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(CONCAT({2},UPPER({3}),'/Viewer'),'MEMBER'))",
            name(SCHEMA_METADATA.getName() + "_POLICY"),
            SCHEMA_METADATA,
            MG_ROLE_PREFIX,
            TABLE_SCHEMA);
      }
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
      try (CreateTableColumnStep t = jooq.createTableIfNotExists(USERS_METADATA)) {
        t.columns(USER_NAME, USER_PASS).constraint(primaryKey(USER_NAME)).execute();
      }

      try (CreateTableColumnStep t = jooq.createTableIfNotExists(SETTINGS_METADATA)) {
        t.columns(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME, SETTINGS_VALUE)
            .constraint(primaryKey(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME))
            .execute();
      }

      logger.info("INITIALIZING MOLGENIS METADATA SCHEMA COMPLETE");
    }

    // this way more robust for non breaking changes
    for (Field field :
        new Field[] {TABLE_INHERITS, TABLE_IMPORT_SCHEMA, TABLE_DESCRIPTION, TALBE_SEMANTICS}) {
      jooq.alterTable(TABLE_METADATA).addColumnIfNotExists(field).execute();
    }

    // this way more robust for non-breaking changes
    for (Field field :
        new Field[] {
          COLUMN_TYPE,
          COLUMN_KEY,
          COLUMN_POSITION,
          COLUMN_REQUIRED,
          COLUMN_REF_SCHEMA,
          COLUMN_REF_TABLE,
          COLUMN_REF_LINK,
          COLUMN_REF_LABEL,
          COLUMN_REF_BACK,
          COLUMN_VALIDATION,
          COLUMN_COMPUTED,
          COLUMN_INDEXED,
          COLUMN_CASCADE,
          COLUMN_DESCRIPTION,
          COLUMN_SEMANTICS,
          COLUMN_VISIBLE,
          COLUMN_FORMAT
        }) {
      jooq.alterTable(COLUMN_METADATA).addColumnIfNotExists(field).execute();
    }
  }

  private static void createRowLevelPermissions(DSLContext jooq, org.jooq.Table table) {
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
    // we record the role name in as a column 'table_rls_manager' and 'table_rls_viewer' and use
    // this to enforce policy of being able to change vs view table.
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2} || upper({3}) || '/"
            + Privileges.MANAGER.toString()
            + "', 'member'))",
        name("TABLE_RLS_" + Privileges.MANAGER),
        table,
        MG_ROLE_PREFIX,
        TABLE_SCHEMA);

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, {2} || upper({3}) || '/"
            + Privileges.VIEWER
            + "', 'member'))",
        name("TABLE_RLS_" + Privileges.VIEWER),
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
              TALBE_SEMANTICS)
          .values(
              table.getSchema().getName(),
              table.getTableName(),
              table.getInherit(),
              table.getImportSchema(),
              table.getDescription(),
              table.getSemantics())
          .onConflict(TABLE_SCHEMA, TABLE_NAME)
          .doUpdate()
          .set(TABLE_INHERITS, table.getInherit())
          .set(TABLE_IMPORT_SCHEMA, table.getImportSchema())
          .set(TABLE_DESCRIPTION, table.getDescription())
          .set(TALBE_SEMANTICS, table.getSemantics())
          .execute();
    } catch (Exception e) {
      throw new MolgenisException("save of table metadata failed", e);
    }
  }

  protected static void alterTableName(DSLContext jooq, TableMetadata table, String newName) {
    jooq.update(TABLE_METADATA)
        .set(TABLE_NAME, newName)
        .where(TABLE_SCHEMA.eq(table.getSchemaName()), TABLE_NAME.eq(table.getTableName()))
        .execute();
  }

  protected static List<User> loadUsers(DSLContext jooq, int limit, int offset) {
    List<User> users = new ArrayList<>();
    for (Object username :
        jooq.select(USER_NAME)
            .from(USERS_METADATA)
            .orderBy(USER_NAME)
            .limit(limit)
            .offset(offset)
            .fetch()
            .getValues(USER_NAME)) {
      users.add(new User((String) username));
    }
    return users;
  }

  public static int countUsers(DSLContext jooq) {
    return jooq.select(count()).from(USERS_METADATA).fetchOne(count());
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
        table.setSemantics(r.get(TALBE_SEMANTICS, String[].class));
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
              .orderBy(COLUMN_POSITION.asc())
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
            COLUMN_TYPE,
            COLUMN_KEY,
            COLUMN_POSITION,
            COLUMN_REQUIRED,
            COLUMN_REF_SCHEMA,
            COLUMN_REF_TABLE,
            COLUMN_REF_LINK,
            COLUMN_REF_LABEL,
            COLUMN_REF_BACK,
            COLUMN_VALIDATION,
            COLUMN_COMPUTED,
            COLUMN_INDEXED,
            COLUMN_CASCADE,
            COLUMN_DESCRIPTION,
            COLUMN_SEMANTICS,
            COLUMN_VISIBLE,
            COLUMN_FORMAT)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getTableName(),
            column.getName(),
            column.getColumnType(),
            column.getKey(),
            column.getPosition(),
            column.isRequired(),
            refSchema,
            column.getRefTableName(),
            column.getRefLink(),
            column.getRefLabelIfSet(),
            column.getRefBack(),
            column.getValidation(),
            column.getComputed(),
            column.isIndexed(),
            column.isCascadeDelete(),
            column.getDescription(),
            column.getSemantics(),
            column.getVisible(),
            column.getColumnFormat())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(COLUMN_TYPE, column.getColumnType())
        .set(COLUMN_KEY, column.getKey())
        .set(COLUMN_POSITION, column.getPosition())
        .set(COLUMN_REQUIRED, column.isRequired())
        .set(COLUMN_REF_SCHEMA, refSchema)
        .set(COLUMN_REF_TABLE, column.getRefTableName())
        .set(COLUMN_REF_LINK, column.getRefLink())
        .set(COLUMN_REF_LABEL, column.getRefLabelIfSet())
        .set(COLUMN_REF_BACK, column.getRefBack())
        .set(COLUMN_VALIDATION, column.getValidation())
        .set(COLUMN_COMPUTED, column.getComputed())
        .set(COLUMN_INDEXED, column.isIndexed())
        .set(COLUMN_CASCADE, column.isCascadeDelete())
        .set(COLUMN_DESCRIPTION, column.getDescription())
        .set(COLUMN_SEMANTICS, column.getSemantics())
        .set(COLUMN_VISIBLE, column.getVisible())
        .set(COLUMN_FORMAT, column.getColumnFormat())
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

  private static Column recordToColumn(org.jooq.Record col) {
    Column c = new Column(col.get(COLUMN_NAME, String.class));
    c.setType(ColumnType.valueOf(col.get(COLUMN_TYPE, String.class)));
    c.setRequired(col.get(COLUMN_REQUIRED, Boolean.class));
    c.setKey(col.get(COLUMN_KEY, Integer.class));
    c.setPosition(col.get(COLUMN_POSITION, Integer.class));
    c.setRefSchema(col.get(COLUMN_REF_SCHEMA, String.class));
    c.setRefTable(col.get(COLUMN_REF_TABLE, String.class));
    c.setRefLink(col.get(COLUMN_REF_LINK, String.class));
    c.setRefLabel(col.get(COLUMN_REF_LABEL, String.class));
    c.setRefBack(col.get(COLUMN_REF_BACK, String.class));
    c.setValidation(col.get(COLUMN_VALIDATION, String.class));
    c.setComputed(col.get(COLUMN_COMPUTED, String.class));
    c.setDescription(col.get(COLUMN_DESCRIPTION, String.class));
    c.setCascadeDelete(col.get(COLUMN_CASCADE, Boolean.class));
    c.setSemantics(col.get(COLUMN_SEMANTICS, String[].class));
    c.setVisible(col.get(COLUMN_VISIBLE, String.class));
    c.setColumnFormat(col.get(COLUMN_FORMAT, String.class));
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
