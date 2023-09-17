package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.utils.TypeUtils.convertToTitleCase;

import java.util.*;
import org.jooq.*;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataUtils {
  private static Logger logger = LoggerFactory.getLogger(MetadataUtils.class);
  private static Integer version;

  static final String MOLGENIS = "MOLGENIS";
  static final String NOT_PROVIDED = "NOT_PROVIDED";
  // tables
  private static final org.jooq.Table DATABASE_METADATA =
      table(name(MOLGENIS, "database_metadata"));
  private static final org.jooq.Table SCHEMA_METADATA = table(name(MOLGENIS, "schema_metadata"));
  private static final org.jooq.Table TABLE_METADATA = table(name(MOLGENIS, "table_metadata"));
  private static final org.jooq.Table COLUMN_METADATA = table(name(MOLGENIS, "column_metadata"));
  private static final org.jooq.Table USERS_METADATA = table(name(MOLGENIS, "users_metadata"));
  private static final org.jooq.Table SETTINGS_METADATA =
      table(name(MOLGENIS, "settings_metadata"));

  // deprecated table/clumn, to be delete on next major upgrade
  private static final org.jooq.Table VERSION_METADATA = table(name(MOLGENIS, "version_metadata"));
  private static final org.jooq.Field<Integer> VERSION_ID =
      field(name("id"), INTEGER.nullable(false));

  // version
  private static final org.jooq.Field<Integer> DATABASE_ID =
      field(name("id"), INTEGER.nullable(false));
  private static final org.jooq.Field<Integer> VERSION =
      field(name("version"), INTEGER.nullable(false));

  // table
  static final org.jooq.Field<String> TABLE_SCHEMA =
      field(name("table_schema"), VARCHAR.nullable(false));
  private static final Field<String> SCHEMA_DESCRIPTION =
      field(name("description"), VARCHAR.nullable(true));
  static final Field<String> TABLE_NAME = field(name("table_name"), VARCHAR.nullable(false));
  private static final Field<String> TABLE_INHERITS =
      field(name("table_inherits"), VARCHAR.nullable(true));
  private static final Field<String> TABLE_IMPORT_SCHEMA =
      field(name("import_schema"), VARCHAR.nullable(true));
  private static final Field<JSON> TABLE_DESCRIPTION =
      field(name("table_description"), JSON.nullable(true));
  private static final Field<JSON> TABLE_LABEL = field(name("table_label"), JSON.nullable(true));
  private static final Field<String[]> TABLE_SEMANTICS =
      field(name("table_semantics"), VARCHAR.getArrayDataType().nullable(true));
  private static final Field<String> TABLE_TYPE = field(name("table_type"), VARCHAR.nullable(true));

  // column
  private static final Field<String> COLUMN_NAME =
      field(name("column_name"), VARCHAR.nullable(false));
  private static final Field<JSON> COLUMN_LABEL = field(name("label"), JSON.nullable(false));
  private static final Field<Integer> COLUMN_KEY = field(name("key"), INTEGER.nullable(true));
  private static final Field<Integer> COLUMN_POSITION = field(name("position"), INTEGER);
  private static final Field<JSON> COLUMN_DESCRIPTION =
      field(name("description"), JSON.nullable(true));
  private static final Field<String> COLUMN_VISIBLE =
      field(name("visible"), VARCHAR.nullable(true));
  private static final Field<String[]> COLUMN_SEMANTICS =
      field(name("columnSemantics"), VARCHAR.nullable(true).getArrayType());
  private static final Field<String> COLUMN_TYPE =
      field(name("columnType"), VARCHAR.nullable(false));
  private static final Field<Boolean> COLUMN_REQUIRED =
      field(name("required"), BOOLEAN.nullable(false));
  private static final Field<String> COLUMN_REF_TABLE =
      field(name("ref_table"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_REF_SCHEMA =
      field(name("ref_schema"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_REF_LINK =
      field(name("ref_link"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_REF_LABEL =
      field(name("refLabel"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_REF_BACK =
      field(name("refBack"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_VALIDATION =
      field(name("validation"), VARCHAR.nullable(true));
  private static final Field<String> COLUMN_COMPUTED =
      field(name("computed"), VARCHAR.nullable(true));
  private static final Field<Boolean> COLUMN_INDEXED =
      field(name("indexed"), BOOLEAN.nullable(true));
  private static final Field<Boolean> COLUMN_CASCADE =
      field(name("cascade"), BOOLEAN.nullable(true));
  private static final Field<Boolean> COLUMN_READONLY =
      field(name("readonly"), BOOLEAN.nullable(true));

  // users
  private static final Field<String> USER_NAME = field(name("username"), VARCHAR);
  private static final Field<String> USER_PASS = field(name("password"), VARCHAR);

  // settings field, reused by all other metadata
  static final org.jooq.Field SETTINGS = field(name(org.molgenis.emx2.Constants.SETTINGS), JSON);

  // old settingstable, to be removed on major update
  private static final org.jooq.Field<String> SETTINGS_NAME =
      field(name(org.molgenis.emx2.Constants.SETTINGS_NAME), VARCHAR);
  private static final org.jooq.Field<String> SETTINGS_TABLE_NAME =
      field(
          name(TABLE_NAME.getName()),
          VARCHAR.nullable(true)); // note table might be null in case of schema
  private static final Field<String> SETTINGS_VALUE =
      field(name(org.molgenis.emx2.Constants.SETTINGS_VALUE), VARCHAR);

  private MetadataUtils() {
    // to hide the public constructor
  }

  /**
   * Returns version number. Returns -1 if metadata does not exist (i.e. MOLGENIS schema does not
   * exist)
   *
   * @param jooq
   * @return
   */
  protected static Integer getVersion(DSLContext jooq) {
    if (version == null) {
      if (jooq.meta().getSchemas(MOLGENIS).size() == 0) {
        // schema does not exist, need to do a full init
        version = -1;
        return version;
      } else {
        // use DATABASE_METADATA table, this is what recent versions would use
        try {
          Result<org.jooq.Record> result = jooq.selectFrom(DATABASE_METADATA).fetch();
          if (result.size() > 0) {
            version = (Integer) result.get(0).get(VERSION);
            return version;
          }
        } catch (Exception e) {
          // if fails, might be older schema, we before used "version_metadata'
          try {
            Result<org.jooq.Record> result =
                jooq.selectFrom(name("MOLGENIS", "version_metadata")).fetch();
            if (result.size() > 0) {
              // in very old version this was a string so might not be integer
              version = (Integer) result.get(0).get(VERSION);
              return version;
            }
          } catch (ClassCastException e2) {
            // this is to handle the legacy systems: before Migration system we used version
            // string of
            // software
            logger.debug(
                "Updating from old 'x.y.z' based database version number to numeric database version number");
          }
          // default if schema exists but seems empty
          version = 0;
          return version;
        }
      }
    }
    return version;
  }

  // should never run in parallel
  protected static void init(DSLContext j) {
    if (j.meta().getSchemas(MOLGENIS).size() == 0) {
      logger.info("INITIALIZING MOLGENIS METADATA SCHEMA");
      j.transaction(
          config -> {
            DSLContext jooq = config.dsl();
            DDLQuery step = jooq.createSchemaIfNotExists(MOLGENIS);
            step.execute();
            jooq.execute("GRANT USAGE ON SCHEMA {0} TO PUBLIC", name(MOLGENIS));
            jooq.execute(
                "ALTER DEFAULT PRIVILEGES IN SCHEMA {0} GRANT ALL ON  TABLES  TO PUBLIC",
                name(MOLGENIS));

            // set version
            CreateTableElementListStep t = jooq.createTableIfNotExists(VERSION_METADATA);
            t.columns(VERSION_ID, VERSION).constraints(primaryKey(VERSION_ID)).execute();

            t = jooq.createTableIfNotExists(SCHEMA_METADATA);
            t.columns(TABLE_SCHEMA, SCHEMA_DESCRIPTION)
                .constraint(primaryKey(TABLE_SCHEMA))
                .execute();

            jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", SCHEMA_METADATA);

            jooq.execute(
                "DROP POLICY IF EXISTS {0} ON {1}",
                name(SCHEMA_METADATA.getName() + "_POLICY"), SCHEMA_METADATA);
            jooq.execute(
                "CREATE POLICY {0} ON {1} USING (pg_has_role(CONCAT({2},{3},'/Aggregator'),'MEMBER'))",
                name(SCHEMA_METADATA.getName() + "_POLICY"),
                SCHEMA_METADATA,
                MG_ROLE_PREFIX,
                TABLE_SCHEMA,
                SCHEMA_DESCRIPTION);

            t = jooq.createTableIfNotExists(TABLE_METADATA);
            int result =
                t.columns(
                        TABLE_SCHEMA,
                        TABLE_NAME,
                        TABLE_INHERITS,
                        TABLE_IMPORT_SCHEMA,
                        TABLE_DESCRIPTION,
                        TABLE_SEMANTICS,
                        TABLE_TYPE)
                    .constraints(
                        primaryKey(TABLE_SCHEMA, TABLE_NAME),
                        foreignKey(TABLE_SCHEMA)
                            .references(SCHEMA_METADATA)
                            .onUpdateCascade()
                            .onDeleteCascade())
                    .execute();
            if (result > 0) {
              createRowLevelPermissions(jooq, TABLE_METADATA);
            }

            t = jooq.createTableIfNotExists(COLUMN_METADATA);
            result =
                t.columns(
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
                        COLUMN_VISIBLE)
                    .constraints(
                        primaryKey(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME),
                        foreignKey(TABLE_SCHEMA, TABLE_NAME)
                            .references(TABLE_METADATA, TABLE_SCHEMA, TABLE_NAME)
                            .onUpdateCascade()
                            .onDeleteCascade())
                    .execute();
            if (result > 0) {
              createRowLevelPermissions(jooq, COLUMN_METADATA);
            }

            t = jooq.createTableIfNotExists(USERS_METADATA);
            t.columns(USER_NAME, USER_PASS).constraint(primaryKey(USER_NAME)).execute();

            t = jooq.createTableIfNotExists(SETTINGS_METADATA);
            t.columns(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME, SETTINGS_VALUE)
                .constraint(primaryKey(TABLE_SCHEMA, SETTINGS_TABLE_NAME, SETTINGS_NAME))
                .execute();
          });

      logger.info("INITIALIZING MOLGENIS METADATA SCHEMA COMPLETE");
    }
  }

  private static void createRowLevelPermissions(DSLContext jooq, org.jooq.Table table) {
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
    // we record the role name in as a column 'table_rls_manager' and 'table_rls_aggregator' and use
    // this to enforce policy of being able to change vs view table.
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(current_user, {2} || {3} || '/"
            + Privileges.MANAGER.toString()
            + "', 'member'))",
        name("TABLE_RLS_" + Privileges.MANAGER),
        table,
        MG_ROLE_PREFIX,
        TABLE_SCHEMA);

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(current_user, {2} || {3} || '/"
            + Privileges.VIEWER
            + "', 'member'))",
        name("TABLE_RLS_" + Privileges.VIEWER),
        table,
        MG_ROLE_PREFIX,
        TABLE_SCHEMA);
  }

  protected static void saveSchemaMetadata(DSLContext sql, SchemaMetadata schema) {
    try {
      String description = Objects.isNull(schema.getDescription()) ? "" : schema.getDescription();
      sql.insertInto(SCHEMA_METADATA)
          .columns(TABLE_SCHEMA, SCHEMA_DESCRIPTION, SETTINGS)
          .values(schema.getName(), description, schema.getSettings())
          .onConflict(TABLE_SCHEMA)
          .doUpdate()
          .set(SCHEMA_DESCRIPTION, schema.getDescription())
          .set(SETTINGS, schema.getSettings())
          .execute();
    } catch (Exception e) {
      throw new SqlMolgenisException("save of schema metadata failed", e);
    }
  }

  protected static Collection<String> loadSchemaNames(SqlDatabase db) {
    return db.getJooq().selectFrom(SCHEMA_METADATA).fetch().getValues(TABLE_SCHEMA, String.class);
  }

  protected static Collection<SchemaInfo> loadSchemaInfos(SqlDatabase db) {
    List<org.jooq.Record> schemaInfoRecords = db.getJooq().selectFrom(SCHEMA_METADATA).fetch();
    List<SchemaInfo> schemaInfos = new ArrayList<>();
    for (org.jooq.Record record : schemaInfoRecords) {
      schemaInfos.add(
          new SchemaInfo(
              record.get(TABLE_SCHEMA, String.class),
              record.get(SCHEMA_DESCRIPTION, String.class)));
    }
    return schemaInfos;
  }

  protected static SchemaMetadata loadSchemaMetadata(DSLContext jooq, SchemaMetadata schema) {
    org.jooq.Record schemaRecord =
        jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).fetchOne();
    if (schemaRecord == null) {
      return schema;
    } else {
      schema.setDescription(schemaRecord.get(SCHEMA_DESCRIPTION, String.class));
      schema.setSettingsWithoutReload(schemaRecord.get(SETTINGS, Map.class));
    }
    return schema;
  }

  protected static void deleteSchema(DSLContext jooq, String schemaName) {
    jooq.deleteFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(schemaName)).execute();
  }

  protected static void saveTableMetadata(DSLContext jooq, TableMetadata table) {
    // check default labels
    Map<String, String> labels = stripDefaultLabels(table.getLabels(), table.getTableName());
    try {
      jooq.insertInto(TABLE_METADATA)
          .columns(
              TABLE_SCHEMA,
              TABLE_NAME,
              TABLE_LABEL,
              TABLE_INHERITS,
              TABLE_IMPORT_SCHEMA,
              TABLE_DESCRIPTION,
              TABLE_SEMANTICS,
              TABLE_TYPE,
              SETTINGS)
          .values(
              table.getSchema().getName(),
              table.getTableName(),
              labels,
              table.getInherit(),
              table.getImportSchema(),
              table.getDescriptions(),
              table.getSemantics(),
              Objects.toString(table.getTableType(), null),
              table.getSettings())
          .onConflict(TABLE_SCHEMA, TABLE_NAME)
          .doUpdate()
          .set(TABLE_LABEL, labels)
          .set(TABLE_INHERITS, table.getInherit())
          .set(TABLE_IMPORT_SCHEMA, table.getImportSchema())
          .set(TABLE_DESCRIPTION, table.getDescriptions())
          .set(TABLE_SEMANTICS, table.getSemantics())
          .set(TABLE_TYPE, Objects.toString(table.getTableType(), null))
          .set(SETTINGS, table.getSettings())
          .execute();
    } catch (Exception e) {
      throw new SqlMolgenisException("save of table metadata failed", e);
    }
  }

  private static Map<String, String> stripDefaultLabels(Map<String, String> labels, String name) {
    if (labels.size() == 1 && convertToTitleCase(name).equals(labels.get("en"))) {
      labels = new TreeMap<>();
    }
    return labels;
  }

  protected static void alterTableName(DSLContext jooq, TableMetadata table, String newName) {
    jooq.update(TABLE_METADATA)
        .set(TABLE_NAME, newName)
        .where(TABLE_SCHEMA.eq(table.getSchemaName()), TABLE_NAME.eq(table.getTableName()))
        .execute();
  }

  protected static List<User> loadUsers(SqlDatabase db, int limit, int offset) {
    try {
      List<User> users = new ArrayList<>();
      for (org.jooq.Record user :
          db.getJooq()
              .select(USER_NAME, SETTINGS)
              .from(USERS_METADATA)
              .orderBy(USER_NAME)
              .limit(limit)
              .offset(offset)
              .fetchArray()) {
        users.add(new User(db, user.get(USER_NAME), user.get(SETTINGS, Map.class)));
      }
      return users;
    } catch (Exception e) {
      throw new SqlMolgenisException("loadUsers failed", e);
    }
  }

  @SuppressWarnings("ConstantConditions")
  public static int countUsers(DSLContext jooq) {
    return jooq.select(count()).from(USERS_METADATA).fetchOne(count());
  }

  protected static Collection<TableMetadata> loadTables(DSLContext jooq, SchemaMetadata schema) {
    try {
      Map<String, TableMetadata> result = new LinkedHashMap<>();
      // tables
      List<org.jooq.Record> tableRecords =
          jooq.selectFrom(TABLE_METADATA).where(TABLE_SCHEMA.eq(schema.getName())).fetch();

      // columns
      List<org.jooq.Record> columnRecords =
          jooq.selectFrom(COLUMN_METADATA)
              .where(TABLE_SCHEMA.eq(schema.getName()))
              .orderBy(COLUMN_POSITION.asc())
              .fetch();

      for (org.jooq.Record r : tableRecords) {
        TableMetadata table = recordToTable(r);
        result.put(table.getTableName(), table);
      }

      for (org.jooq.Record r : columnRecords) {
        result.get(r.get(TABLE_NAME, String.class)).add(recordToColumn(r));
      }
      return result.values();
    } catch (Exception e) {
      throw new SqlMolgenisException("load of table metadata failed", e);
    }
  }

  @SuppressWarnings("ConstantConditions")
  protected static TableMetadata loadTable(DSLContext jooq, String schemaName, String tableName) {
    org.jooq.Record tableRecord =
        jooq.selectFrom(TABLE_METADATA)
            .where(TABLE_SCHEMA.eq(schemaName).and(TABLE_NAME.eq(tableName)))
            .orderBy(TABLE_NAME)
            .fetchOne();

    List<org.jooq.Record> columnRecords =
        jooq.selectFrom(COLUMN_METADATA)
            .where(TABLE_SCHEMA.eq(schemaName).and(TABLE_NAME.eq(tableName)))
            .orderBy(COLUMN_POSITION.asc())
            .fetch();

    TableMetadata table = recordToTable(tableRecord);
    for (org.jooq.Record r : columnRecords) {
      table.add(recordToColumn(r));
    }

    return table;
  }

  private static TableMetadata recordToTable(org.jooq.Record r) {
    TableMetadata table = new TableMetadata(r.get(TABLE_NAME, String.class));
    table.setInherit(r.get(TABLE_INHERITS, String.class));
    table.setImportSchema(r.get(TABLE_IMPORT_SCHEMA, String.class));
    table.setLabels(r.get(TABLE_LABEL) != null ? r.get(TABLE_LABEL, Map.class) : new TreeMap<>());
    table.setDescriptions(
        r.get(TABLE_DESCRIPTION) != null ? r.get(TABLE_DESCRIPTION, Map.class) : new TreeMap<>());
    table.setSemantics(r.get(TABLE_SEMANTICS, String[].class));
    table.setSettingsWithoutReload(
        r.get(SETTINGS) != null ? r.get(SETTINGS, Map.class) : new TreeMap<>());
    if (r.get(TABLE_TYPE, String.class) != null) {
      table.setTableType(TableType.valueOf(r.get(TABLE_TYPE, String.class)));
    }
    return table;
  }

  protected static void deleteTable(DSLContext jooq, TableMetadata table) {
    jooq.deleteFrom(TABLE_METADATA)
        .where(TABLE_SCHEMA.eq(table.getSchema().getName()), TABLE_NAME.eq(table.getTableName()))
        .execute();
  }

  protected static void saveColumnMetadata(DSLContext jooq, Column column) {
    String refSchema =
        column.getRefSchema().equals(column.getSchemaName()) ? null : column.getRefSchema();
    Map<String, String> labels = stripDefaultLabels(column.getLabels(), column.getName());
    jooq.insertInto(COLUMN_METADATA)
        .columns(
            TABLE_SCHEMA,
            TABLE_NAME,
            COLUMN_NAME,
            COLUMN_LABEL,
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
            COLUMN_READONLY,
            COLUMN_SEMANTICS,
            COLUMN_VISIBLE)
        .values(
            column.getTable().getSchema().getName(),
            column.getTable().getTableName(),
            column.getName(),
            labels,
            Objects.toString(column.getColumnType(), null),
            column.getKey(),
            column.getPosition(),
            column.isRequired(),
            refSchema,
            column.getRefTableName(),
            column.getRefLink(),
            column.getRefLabel(),
            column.getRefBack(),
            column.getValidation(),
            column.getComputed(),
            column.isIndexed(),
            column.isCascadeDelete(),
            column.getDescriptions(),
            column.isReadonly(),
            column.getSemantics(),
            column.getVisible())
        .onConflict(TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME)
        .doUpdate()
        .set(COLUMN_LABEL, labels)
        .set(COLUMN_TYPE, Objects.toString(column.getColumnType(), null))
        .set(COLUMN_KEY, column.getKey())
        .set(COLUMN_POSITION, column.getPosition())
        .set(COLUMN_REQUIRED, column.isRequired())
        .set(COLUMN_REF_SCHEMA, refSchema)
        .set(COLUMN_REF_TABLE, column.getRefTableName())
        .set(COLUMN_REF_LINK, column.getRefLink())
        .set(COLUMN_REF_LABEL, column.getRefLabel())
        .set(COLUMN_REF_BACK, column.getRefBack())
        .set(COLUMN_VALIDATION, column.getValidation())
        .set(COLUMN_COMPUTED, column.getComputed())
        .set(COLUMN_INDEXED, column.isIndexed())
        .set(COLUMN_CASCADE, column.isCascadeDelete())
        .set(COLUMN_DESCRIPTION, column.getDescriptions())
        .set(COLUMN_READONLY, column.isReadonly())
        .set(COLUMN_SEMANTICS, column.getSemantics())
        .set(COLUMN_VISIBLE, column.getVisible())
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

  protected static boolean schemaExists(DSLContext jooq, String name) {
    return jooq.selectFrom(SCHEMA_METADATA).where(TABLE_SCHEMA.eq(name)).fetch().isNotEmpty();
  }

  private static Column recordToColumn(org.jooq.Record col) {
    Column c = new Column(col.get(COLUMN_NAME, String.class));
    c.setLabels(col.get(COLUMN_LABEL) != null ? col.get(COLUMN_LABEL, Map.class) : new TreeMap<>());
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
    c.setDescriptions(
        col.get(COLUMN_DESCRIPTION) != null
            ? col.get(COLUMN_DESCRIPTION, Map.class)
            : new TreeMap<>());
    c.setCascadeDelete(col.get(COLUMN_CASCADE, Boolean.class));
    c.setReadonly(col.get(COLUMN_READONLY, Boolean.class));
    c.setSemantics(col.get(COLUMN_SEMANTICS, String[].class));
    c.setVisible(col.get(COLUMN_VISIBLE, String.class));
    return c;
  }

  public static void setUserPassword(DSLContext jooq, String user, String password) {
    jooq.insertInto(USERS_METADATA)
        .columns(USER_NAME, USER_PASS)
        .values(
            field("{0}", String.class, user),
            field("crypt({0}, gen_salt('bf'))", String.class, password))
        .onConflict(USER_NAME)
        .doUpdate()
        .set(USER_PASS, field("crypt({0}, gen_salt('bf'))", String.class, password))
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

  public static void setVersion(DSLContext jooq, int newVersion) {
    jooq.insertInto(DATABASE_METADATA, DATABASE_ID, VERSION)
        .values(1, newVersion)
        .onConflict(DATABASE_ID)
        .doUpdate()
        .set(VERSION, newVersion)
        .execute();
    version = newVersion;
  }

  public static int getMaxPosition(DSLContext jooq, String schemaName) {
    Integer result =
        jooq.select(max(COLUMN_POSITION).as(COLUMN_POSITION.getName()))
            .from(COLUMN_METADATA)
            .where(TABLE_SCHEMA.eq(schemaName))
            .fetchOne()
            .get(COLUMN_POSITION.getName(), Integer.class);
    return Objects.requireNonNullElse(result, 0);
  }

  public static void saveDatabaseSettings(DSLContext jooq, Map<String, String> settings) {
    jooq.insertInto(DATABASE_METADATA)
        .columns(DATABASE_ID, VERSION, SETTINGS)
        .values(1, getVersion(jooq), settings)
        .onConflict(DATABASE_ID)
        .doUpdate()
        .set(VERSION, getVersion(jooq))
        .set(SETTINGS, settings)
        .execute();
  }

  public static Map<String, String> loadDatabaseSettings(DSLContext jooq) {
    org.jooq.Record databaseRecord =
        jooq.selectFrom(DATABASE_METADATA).where(DATABASE_ID.eq(1)).fetchOne();
    return databaseRecord.get(SETTINGS, Map.class);
  }

  public static void saveUserMetadata(DSLContext jooq, User user) {
    // don't update password via this route
    jooq.insertInto(USERS_METADATA)
        .columns(USER_NAME, SETTINGS)
        .values(user.getUsername(), user.getSettings())
        .onConflict(USER_NAME)
        .doUpdate()
        .set(SETTINGS, user.getSettings())
        .execute();
  }

  public static User loadUserMetadata(SqlDatabase db, String userName) {
    org.jooq.Record userRecord =
        db.getJooq().selectFrom(USERS_METADATA).where(USER_NAME.eq(userName)).fetchOne();
    if (userRecord != null) {
      User result = new User(db, userName);
      result.setSettings(userRecord.get(SETTINGS, Map.class));
      return result;
    }
    return null;
  }

  public static void resetVersion() {
    version = null;
  }
}
