// package org.molgenis.emx2.sql;
//
// import org.jooq.DSLContext;
// import org.molgenis.emx2.*;
//
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.List;
//
// import static org.jooq.impl.DSL.name;
// import static org.molgenis.emx2.Column.column;
//
// public class MetadataUtils2 {
//
//  private static final String MOLGENIS = "MOLGENIS";
//
//  // tables
//  private static final String SCHEMA_METADATA = "schema_metadata";
//  private static final String TABLE_METADATA = "table_metadata";
//  private static final String COLUMN_METADATA = "column_metadata";
//  private static final String UNIQUE_METADATA = "unique_metadata";
//  private static final String USERS_METADATA = "users_metadata";
//
//  // fields
//  private static final String TABLE_SCHEMA = "table_schema";
//  private static final String TABLE_NAME = "table_name";
//  private static final String TABLE_PKEY = "table_pkey";
//  private static final String TABLE_INHERITS = "table_inherits";
//  private static final String TABLE_DESCRIPTION = "table_description";
//  private static final String COLUMN_NAME = "column_name";
//  private static final String COLUMN_DESCRIPTION = "column_description";
//
//  private static final String DATA_TYPE = "data_type";
//  private static final String NULLABLE = "nullable";
//  private static final String PKEY = "pkey";
//  private static final String REF_TABLE = "ref_table";
//  private static final String REF_COLUMN = "ref_column";
//  private static final String REF_BACK = "refBack";
//  private static final String VALIDATION_SCRIPT = "validationScript";
//  private static final String UNIQUE_COLUMNS = "unique_columns";
//  private static final String INDEXED = "indexed";
//
//  private static final String USER_NAME = "username";
//  private static final String USER_PASS = "password";
//
//  private MetadataUtils2() {
//    // to hide the public constructor
//  }
//
//  protected static void createMetadataSchemaIfNotExists(DSLContext jooq) {
//
//    // if exists then skip
//    if (!jooq.meta().getSchemas(MOLGENIS).isEmpty()) return;
//
//    // create
//    // fields
//
//    TableMetadata schema = new TableMetadata(SCHEMA_METADATA);
//    schema.add(column(TABLE_SCHEMA)).pkey(TABLE_SCHEMA);
//
//    TableMetadata table = new TableMetadata(TABLE_METADATA);
//    table.add(column(TABLE_SCHEMA).refTable(TABLE_SCHEMA)); // should we cascade?
//    table.add(column(TABLE_NAME));
//    table.add(column(TABLE_INHERITS).nullable(true));
//    table.add(column(TABLE_DESCRIPTION).nullable(true));
//    table.pkey(TABLE_SCHEMA, TABLE_NAME);
//
//    TableMetadata column = new TableMetadata(COLUMN_METADATA);
//    column.add(column(TABLE_SCHEMA));
//    column.add(column(TABLE_NAME));
//    column.add(column(COLUMN_NAME));
//    column.add(column(DATA_TYPE));
//    column.add(column(NULLABLE));
//    column.add(column(PKEY));
//    column.add(column(REF_TABLE).nullable(true));
//    column.add(column(REF_COLUMN).nullable(true));
//    column.add(column(MAPPED_BY).nullable(true));
//    column.add(column(VALIDATION_SCRIPT).nullable(true));
//    column.add(column(INDEXED).nullable(true));
//    column.add(column(COLUMN_DESCRIPTION).nullable(true));
//    column.pkey(
//        TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME); // should be foreign key too with cascade delete!
//
//    TableMetadata unique = new TableMetadata(UNIQUE_METADATA);
//    unique.add(column(TABLE_SCHEMA));
//    unique.add(column(TABLE_NAME));
//    unique.add(column(UNIQUE_COLUMNS).type(ColumnType.STRING_ARRAY).nullable(true));
//    unique.pkey(TABLE_SCHEMA, TABLE_NAME, UNIQUE_COLUMNS); // implicit also true
//
//    TableMetadata users = new TableMetadata(USERS_METADATA);
//    users.add(column(USER_NAME)).pkey(USER_NAME);
//    users.add(column(USER_PASS));
//  }
//
//  private static void createRowLevelPermissions(DSLContext jooq, org.jooq.Table table) {
//    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
//    // we record the role name in as a column 'table_rls_manager' and 'table_rls_viewer' and use
//    // this to enforce policy of being able to change vs view table.
//    jooq.execute(
//        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2} || upper({3}) || '/"
//            + DefaultRoles.MANAGER.toString()
//            + "', 'member'))",
//        name("TABLE_RLS_" + DefaultRoles.MANAGER),
//        table,
//        Constants.MG_ROLE_PREFIX,
//        TABLE_SCHEMA);
//
//    jooq.execute(
//        "CREATE POLICY {0} ON {1} FOR SELECT USING (pg_has_role(session_user, {2} || upper({3}) ||
// '/"
//            + DefaultRoles.VIEWER
//            + "', 'member'))",
//        name("TABLE_RLS_" + DefaultRoles.VIEWER),
//        table,
//        Constants.MG_ROLE_PREFIX,
//        TABLE_SCHEMA);
//  }
//
//  protected static Collection<String> loadSchemaNames(SqlDatabase db) {
//    return db.getJooq().selectFrom(SCHEMA_METADATA).fetch().getValues(TABLE_SCHEMA, String.class);
//  }
//
//  protected static void saveSchemaMetadata(Database db, SchemaMetadata schema) {
//    db.getSchema(MOLGENIS)
//        .getTable(SCHEMA_METADATA)
//        .update(new Row().set(TABLE_SCHEMA, TABLE_SCHEMA));
//  }
//
//  protected static void deleteSchema(Database db, SchemaMetadata schema) {
//    db.getSchema(MOLGENIS)
//        .getTable(SCHEMA_METADATA)
//        .delete(new Row().set(TABLE_SCHEMA, TABLE_SCHEMA));
//  }
//
//  protected static Collection<String> loadTableNames(Database db, SchemaMetadata sqlSchema) {
//    List<Row> result =
//        db.getSchema(MOLGENIS)
//            .query(TABLE_METADATA)
//            .filter(TABLE_SCHEMA, Operator.EQUALS, sqlSchema.getName())
//            .getRows();
//    List<String> names = new ArrayList<>();
//    for (Row r : result) {
//      names.add(r.getString(TABLE_NAME));
//    }
//    return names;
//  }
//
//  protected static void loadTableMetadata(Database db, TableMetadata table) {
//    List<Row> result =
//        db.getSchema(MOLGENIS)
//            .query(TABLE_METADATA)
//            .filter(TABLE_SCHEMA, Operator.EQUALS, table.getSchema().getName())
//            .filter(TABLE_NAME, Operator.EQUALS, table.getTableName())
//            .getRows();
//    if (result.size() != 1) {
//      return;
//    }
//    table.setInherit(result.get(0).getString(TABLE_INHERITS));
//    table.setDescription(result.get(0).getString(TABLE_DESCRIPTION));
//    for (Column c : MetadataUtils2.loadColumnMetadata(db, table)) {
//      table.add(c);
//    }
//    table.pkey(result.get(0).getStringArray(TABLE_PKEY));
//    MetadataUtils2.loadUniqueMetadata(db, table);
//  }
//
//  protected static void saveTableMetadata(Database db, TableMetadata table) {
//    db.getSchema(MOLGENIS).getTable(TABLE_METADATA).update(getTableRow(table));
//  }
//
//  protected static void deleteTable(Database db, TableMetadata table) {
//    db.getSchema(MOLGENIS).getTable(TABLE_METADATA).delete(getTableRow(table));
//  }
//
//  private static Row getTableRow(TableMetadata table) {
//    Row row = new Row();
//    row.set(TABLE_SCHEMA, table.getSchema().getName());
//    row.set(TABLE_NAME, table.getTableName());
//    row.set(TABLE_PKEY, table.getPrimaryKey());
//    row.set(TABLE_INHERITS, table.getInherit());
//    row.set(TABLE_DESCRIPTION, table.getDescription());
//    return row;
//  }
//
//  protected static void saveColumnMetadata(Database db, Column column) {
//    db.getSchema(MOLGENIS).getTable(COLUMN_METADATA).update(getColumnRow(column));
//  }
//
//  private static Row getColumnRow(Column column) {
//    Row row = new Row();
//    row.set(TABLE_SCHEMA, column.getTable().getSchema().getName());
//    row.set(TABLE_NAME, column.getTable().getTableName());
//    row.set(COLUMN_NAME, column.getName());
//    row.set(DATA_TYPE, column.getColumnType());
//    row.set(NULLABLE, column.isNullable());
//    row.set(REF_TABLE, column.getRefTableName());
//    row.set(REF_COLUMN, column.getRefColumnNameRaw());
//    row.set(REF_BACK, column.getRefBack());
//    row.set(VALIDATION_SCRIPT, column.getValidation());
//    row.set(INDEXED, column.isIndexed());
//    row.set(COLUMN_DESCRIPTION, column.getDescription());
//    return row;
//  }
//
//  protected static void deleteColumn(Database db, Column column) {
//    db.getSchema(MOLGENIS).getTable(COLUMN_METADATA).delete(getColumnRow(column));
//  }
//
//  private static Row getUniqueRow(TableMetadata table, String[] columnNames) {
//    Row row = new Row();
//    row.set(TABLE_SCHEMA, table.getSchemaName());
//    row.set(TABLE_NAME, table.getTableName());
//    row.set(UNIQUE_COLUMNS, columnNames);
//    return row;
//  }
//
//  protected static void saveUnique(Database db, TableMetadata table, String... columnNames) {
//    db.getSchema(MOLGENIS).getTable(UNIQUE_METADATA).update(getUniqueRow(table, columnNames));
//  }
//
//  protected static void deleteUnique(Database db, TableMetadata table, String... columnNames) {
//    db.getSchema(MOLGENIS).getTable(UNIQUE_METADATA).delete(getUniqueRow(table, columnNames));
//  }
//
//  protected static boolean schemaExists(Database db, String name) {
//    List<Row> results =
//        db.getSchema(MOLGENIS)
//            .query(SCHEMA_METADATA)
//            .filter(TABLE_SCHEMA, Operator.EQUALS, name)
//            .getRows();
//    return results.size() == 1;
//  }
//
//  protected static void loadUniqueMetadata(Database db, TableMetadata table) {
//    List<Row> result =
//        db.getSchema(MOLGENIS)
//            .query(TABLE_SCHEMA)
//            .filter(TABLE_SCHEMA, Operator.EQUALS, table.getSchema().getName())
//            .filter(TABLE_NAME, Operator.EQUALS, table.getTableName())
//            .getRows();
//    for (Row r : result) {
//      table.pkey(r.getStringArray(TABLE_PKEY));
//      table.addUnique(r.getString(UNIQUE_COLUMNS));
//      table.setDescription(r.getString(TABLE_DESCRIPTION));
//    }
//  }
//
//  protected static List<Column> loadColumnMetadata(Database db, TableMetadata table) {
//
//    List<Column> columnList = new ArrayList<>();
//
//    Query q =
//        db.getSchema(MOLGENIS)
//            .query(COLUMN_METADATA)
//            .filter(TABLE_SCHEMA, Operator.EQUALS, table.getSchema().getName())
//            .filter(TABLE_NAME, Operator.EQUALS, table.getTableName());
//
//    ;
//    for (Row col : q.getRows()) {
//      Column c = new Column(col.getString(COLUMN_NAME));
//      c.type(ColumnType.valueOf(col.getString(DATA_TYPE)));
//      c.nullable(col.get(NULLABLE, Boolean.class));
//      c.refTable(col.get(REF_TABLE, String.class));
//      c.refColumn(col.get(REF_COLUMN, String.class));
//      c.refBack(col.get(MAPPED_BY, String.class));
//      c.validation(col.get(VALIDATION_SCRIPT, String.class));
//      c.setDescription(col.get(COLUMN_DESCRIPTION, String.class));
//      columnList.add(new Column(table, c));
//    }
//
//    return columnList;
//  }
//
//  public static void setUserPassword(Database db, String user, String password) {
//    // todo encrypt password
//    db.getSchema(MOLGENIS)
//        .getTable(USERS_METADATA)
//        .update(new Row().set(USER_NAME, user).set(password, password));
//  }
//
//  public static boolean checkUserPassword(Database db, String username, String password) {
//    List<Row> result =
//        db.getSchema(MOLGENIS)
//            .query(USERS_METADATA)
//            .filter(USER_NAME, Operator.EQUALS, username)
//            .getRows();
//    // todo check encrypted password
//    return result.size() == 1;
//  }
// }
