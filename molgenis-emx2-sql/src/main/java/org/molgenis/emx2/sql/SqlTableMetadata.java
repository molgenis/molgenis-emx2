package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.ColumnType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.Constants.MG_EDIT_ROLE;
import static org.molgenis.emx2.sql.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;

class SqlTableMetadata extends TableMetadata {
  private SqlDatabase db;

  SqlTableMetadata(SqlDatabase db, SqlSchemaMetadata schema, String name) {
    super(schema, name);
    this.db = db;
  }

  void load() {
    MetadataUtils.loadColumnMetadata(this, columns);
    MetadataUtils.loadTableMetadata(this);
    MetadataUtils.loadUniqueMetadata(this);
  }

  @Override
  public Column getColumn(String name) {
    try {
      return super.getColumn(name);
      // in case it has been made by other thread, try to load from backend
    } catch (Exception e) {
      this.load();
      return super.getColumn(name);
    }
  }

  void createTable() {
    db.transaction(
        dsl -> {
          Name tableName = name(getSchema().getName(), getTableName());
          DSLContext jooq = db.getJooq();
          jooq.createTable(tableName).columns().execute();
          MetadataUtils.saveTableMetadata(this);

          // grant rights to schema manager, editor and viewer roles
          jooq.execute(
              "GRANT SELECT ON {0} TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.VIEWER.toString()));
          jooq.execute(
              "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.EDITOR.toString()));
          jooq.execute(
              "ALTER TABLE {0} OWNER TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.MANAGER.toString()));

          enableSearch();
        });
  }

  String getRolePrefix() {
    return ((SqlSchemaMetadata) getSchema()).getRolePrefix();
  }

  public void dropTable() {
    try {
      db.getJooq().dropTable(name(getSchema().getName(), getTableName())).execute();
      MetadataUtils.deleteTable(this);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("drop_table_failed", "Drop table failed", dae);
    }
  }

  @Override
  public SqlTableMetadata setPrimaryKey(String... columnNames) {
    if (columnNames.length == 0)
      throw new MolgenisException(
          "invalid_primary_key",
          "Primary key creation failed",
          "Primary key requires 1 or more columns, however, 0 columns where provided");
    Name[] keyNames = Stream.of(columnNames).map(DSL::name).toArray(Name[]::new);

    // drop previous primary key if exists
    db.getJooq()
        .execute(
            "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}",
            getJooqTable(), name(getTableName() + "_pkey"));

    // createTableIfNotExists the new one
    db.getJooq().alterTable(getJooqTable()).add(constraint().primaryKey(keyNames)).execute();

    // update the decorated super
    super.setPrimaryKey(columnNames);
    MetadataUtils.saveTableMetadata(this);
    return this;
  }

  @Override
  public SqlColumn addColumn(String name, ColumnType columnType) {
    SqlColumn c = new SqlColumn(this, name, columnType);
    c.createColumn();
    super.addColumn(c);
    this.updateSearchIndexTriggerFunction();
    return c;
  }

  protected void addColumnWithoutCreate(Column metadata) {
    super.addColumn(metadata);
  }

  @Override
  public Column addColumn(Column metadata) {
    Column result = null;

    switch (metadata.getColumnType()) {
      case REF:
        result =
            addRef(
                metadata.getColumnName(), metadata.getRefTableName(), metadata.getRefColumnName());
        break;
      case REF_ARRAY:
        result =
            addRefArray(
                metadata.getColumnName(), metadata.getRefTableName(), metadata.getRefColumnName());
        break;
      case MREF:
        result =
            addMref(
                metadata.getColumnName(),
                metadata.getRefTableName(),
                metadata.getRefColumnName(),
                metadata.getReverseRefTableName(),
                metadata.getReverseRefColumn(),
                metadata.getMrefJoinTableName());
        break;
      default:
        result = addColumn(metadata.getColumnName(), metadata.getColumnType());
    }
    result.setDescription(metadata.getDescription());
    result.setNullable(metadata.getNullable());
    result.setDefaultValue(metadata.getDefaultValue());
    return result;
  }

  @Override
  public Column addRef(String name, String toTable, String toColumn) {
    RefSqlColumn c = new RefSqlColumn(this, name, toTable, toColumn);
    c.createColumn();
    super.addColumn(c);
    this.updateSearchIndexTriggerFunction();
    return c;
  }

  @Override
  public Column addRefArray(String name, String toTable, String toColumn) {
    RefArraySqlColumn c = new RefArraySqlColumn(this, name, toTable, toColumn);
    c.createColumn();
    super.addColumn(c);
    this.updateSearchIndexTriggerFunction();
    return c;
  }

  @Override
  public SqlReferenceMultiple addRefMultiple(String... names) {
    return new SqlReferenceMultiple(this, REF, names);
  }

  @Override
  public SqlReferenceMultiple addRefArrayMultiple(String... names) {
    return new SqlReferenceMultiple(this, REF_ARRAY, names);
  }

  @Override
  public MrefSqlColumn addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTable) {
    MrefSqlColumn c =
        new MrefSqlColumn(
            this, name, refTable, refColumn, reverseName, reverseRefColumn, joinTable);
    c.createColumn();
    super.addColumn(c);
    this.updateSearchIndexTriggerFunction();
    return c;
  }

  protected void addMrefReverse(MrefSqlColumn reverse) {
    columns.put(reverse.getColumnName(), reverse);
  }

  @Override
  public void removeColumn(String name) {
    db.getJooq().alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  org.jooq.Table getJooqTable() {
    return table(name(getSchema().getName(), getTableName()));
  }

  public boolean exists() {
    // first look at already loaded metadata, in case of no columns, check the underlying table
    if (!getColumns().isEmpty()) {
      return true;
    }
    // jooq doesn't have operator for this, so by hand. Might be slow
    return 0
        < db.getJooq()
            .select(count())
            .from(name("information_schema", "tables"))
            .where(
                field("table_schema")
                    .eq(getSchema().getName())
                    .and(field("table_name").eq(getTableName())))
            .fetchOne(0, Integer.class);
  }

  protected void loadPrimaryKey(String[] pkey) {
    super.setPrimaryKey(pkey);
  }

  protected void loadUnique(String[] columns) {
    super.addUnique(columns);
  }

  @Override
  public TableMetadata addUnique(String... columnNames) {
    if (getPrimaryKey().length == 0) {
      this.setPrimaryKey(columnNames); // default first unique is also primary key
    } else {
      String uniqueName = getTableName() + "_" + String.join("_", columnNames) + "_UNIQUE";
      db.getJooq()
          .alterTable(getJooqTable())
          .add(constraint(name(uniqueName)).unique(columnNames))
          .execute();
      MetadataUtils.saveUnique(this, columnNames);
      super.addUnique(columnNames);
    }
    return this;
  }

  @Override
  public void removeUnique(String... columnNames) {
    // try to find the right unique
    String[] correctOrderedNames = null;
    List list1 = Arrays.asList(columnNames);
    for (int i = 0; i < uniques.size(); i++) {
      List list2 = Arrays.asList(uniques.get(i));
      if (list1.containsAll(list2) && list2.containsAll(list1)) {
        correctOrderedNames = uniques.get(i);
      }
    }
    if (correctOrderedNames == null) {
      throw new MolgenisException(
          "unique_invalid",
          "Remove unique failed because the unique was unknown",
          "Unique constraint consisting of columns " + list1 + "could not be found. ");
    }

    String uniqueName = getTableName() + "_" + String.join("_", correctOrderedNames) + "_UNIQUE";
    db.getJooq().alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    MetadataUtils.deleteUnique(this, correctOrderedNames);
    super.removeUnique(correctOrderedNames);
  }

  private void enableSearch() {

    // 1. add tsvector column with index
    db.getJooq()
        .execute(
            "ALTER TABLE {0} ADD COLUMN {1} tsvector",
            getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));
    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    db.getJooq()
        .execute(
            "CREATE INDEX {0} ON {1} USING GIN( {2} )",
            name(getTableName() + "_search_idx"),
            getJooqTable(),
            name(MG_SEARCH_INDEX_COLUMN_NAME));

    // 3. createColumn the trigger function to automatically update the
    // MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction = updateSearchIndexTriggerFunction();

    // 4. add trigger to update the tsvector on each insert or update
    db.getJooq()
        .execute(
            "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
                + triggerfunction,
            name(MG_SEARCH_INDEX_COLUMN_NAME),
            getJooqTable());
  }

  private String updateSearchIndexTriggerFunction() {
    String triggerName = getTableName() + "search_vector_trigger";
    String triggerfunction = String.format("\"%s\".\"%s\"()", getSchema().getName(), triggerName);

    StringBuilder mgSearchVector = new StringBuilder("to_tsvector('english', ' '");
    for (Column c : getColumns()) {
      if (!c.getColumnName().startsWith("MG_"))
        mgSearchVector.append(
            String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getColumnName()));
    }
    mgSearchVector.append(")");

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.%s:=%s ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction, name(MG_SEARCH_INDEX_COLUMN_NAME), mgSearchVector);

    db.getJooq().execute(functionBody);
    db.getJooq()
        .execute(
            "ALTER FUNCTION " + triggerfunction + " OWNER TO {0}",
            name(getRolePrefix() + DefaultRoles.MANAGER.toString()));
    return triggerfunction;
  }

  @Override
  public void enableRowLevelSecurity() {
    SqlColumn c = this.addColumn(MG_EDIT_ROLE, STRING);
    c.setIndexed(true);

    db.getJooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    db.getJooq()
        .execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
            name("RLS/" + getSchema().getName() + "/" + getTableName()),
            getJooqTable(),
            name(MG_EDIT_ROLE));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  public DSLContext getJooq() {
    return db.getJooq();
  }
}
