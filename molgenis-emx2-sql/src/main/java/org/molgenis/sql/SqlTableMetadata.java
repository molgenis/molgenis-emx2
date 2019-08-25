package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.MolgenisException;
import org.molgenis.metadata.ColumnMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Permission.*;
import static org.molgenis.metadata.Type.*;
import static org.molgenis.sql.MetadataUtils.*;

class SqlTableMetadata extends TableMetadata {
  public static final String MG_EDIT_ROLE = "MG_EDIT_ROLE";
  public static final String MG_SEARCH_INDEX_COLUMN_NAME = "MG_SEARCH_VECTOR";
  public static final String MG_ROLE_PREFIX = "MG_ROLE_";
  private SqlDatabase db;

  SqlTableMetadata(SqlDatabase db, SqlSchemaMetadata schema, String name) {
    super(schema, name);
    this.db = db;
  }

  void load() throws MolgenisException {
    loadColumnMetadata(this, columns);
    loadTableMetadata(this);
    loadUniqueMetadata(this);
  }

  @Override
  public ColumnMetadata getColumn(String name) throws MolgenisException {
    try {
      return super.getColumn(name);
      // in case it has been made by other thread, try to load from backend
    } catch (Exception e) {
      this.load();
      return super.getColumn(name);
    }
  }

  void createTable() throws MolgenisException {
    Name tableName = name(getSchema().getName(), getTableName());
    DSLContext jooq = db.getJooq();
    jooq.createTable(tableName).columns().execute();
    saveTableMetadata(this);

    // grant rights to schema manager, editor and viewer roles
    String prefix = MG_ROLE_PREFIX + getSchema().getName().toUpperCase();
    jooq.execute("GRANT SELECT ON {0} TO {1}", tableName, name(prefix + VIEW));
    jooq.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(prefix + EDIT));
    jooq.execute("ALTER TABLE {0} OWNER TO {1}", tableName, name(prefix + MANAGE));

    // add default molgenisid primary key column
    this.addColumn(MOLGENISID, UUID).primaryKey();
  }

  @Override
  public SqlTableMetadata setPrimaryKey(String... columnNames) throws MolgenisException {
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

    // create the new one
    db.getJooq().alterTable(getJooqTable()).add(constraint().primaryKey(keyNames)).execute();

    // update the decorated super
    super.setPrimaryKey(columnNames);
    saveTableMetadata(this);
    return this;
  }

  @Override
  public SqlColumnMetadata addColumn(String name, Type type) throws MolgenisException {
    SqlColumnMetadata c = new SqlColumnMetadata(this, name, type);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public ColumnMetadata addRef(String name, String toTable, String toColumn)
      throws MolgenisException {
    RefSqlColumnMetadata c = new RefSqlColumnMetadata(this, name, toTable, toColumn);
    c.createColumn();
    this.addColumn(c);
    return c;
  }

  @Override
  public ColumnMetadata addRefArray(String name, String toTable, String toColumn)
      throws MolgenisException {
    RefArraySqlColumnMetadata c = new RefArraySqlColumnMetadata(this, name, toTable, toColumn);
    c.createColumn();
    this.addColumn(c);
    return c;
  }

  @Override
  public SqlReferenceMultiple addRefMultiple(String... name) throws MolgenisException {
    return new SqlReferenceMultiple(this, REF, name);
  }

  @Override
  public SqlReferenceMultiple addRefArrayMultiple(String... name) throws MolgenisException {
    return new SqlReferenceMultiple(this, REF_ARRAY, name);
  }

  @Override
  public MrefSqlColumnMetadata addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTable)
      throws MolgenisException {
    MrefSqlColumnMetadata c =
        new MrefSqlColumnMetadata(
            this, name, refTable, refColumn, reverseName, reverseRefColumn, joinTable);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  protected void addMrefReverse(MrefSqlColumnMetadata reverse) {
    columns.put(reverse.getColumnName(), reverse);
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    db.getJooq().alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  org.jooq.Table getJooqTable() {
    return table(name(getSchema().getName(), getTableName()));
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public void dropTable() {
    db.getJooq().dropTable(name(getSchema().getName(), getTableName())).execute();
    deleteTable(this);
  }

  protected void loadPrimaryKey(String[] pkey) throws MolgenisException {
    super.setPrimaryKey(pkey);
  }

  protected void loadUnique(String[] columns) throws MolgenisException {
    super.addUnique(columns);
  }

  @Override
  public TableMetadata addUnique(String... columnNames) throws MolgenisException {
    String uniqueName = getTableName() + "_" + String.join("_", columnNames) + "_UNIQUE";
    db.getJooq()
        .alterTable(getJooqTable())
        .add(constraint(name(uniqueName)).unique(columnNames))
        .execute();
    saveUnique(this, columnNames);
    super.addUnique(columnNames);
    return this;
  }

  @Override
  public void removeUnique(String... columnNames) throws MolgenisException {
    // try to find the right unique
    String[] correctOrderedNames = null;
    List list1 = Arrays.asList(columnNames);
    for (int i = 0; i < uniques.size(); i++) {
      List list2 = Arrays.asList(uniques.get(i));
      if (list1.containsAll(list2) && list2.containsAll(list1)) {
        correctOrderedNames = uniques.get(i);
      }
    }
    if (correctOrderedNames == null)
      throw new MolgenisException(
          "unique_invalid",
          "Remove unique failed because the unique was unknown",
          "Unique constraint consisting of columns " + list1 + "could not be found. ");

    String uniqueName = getTableName() + "_" + String.join("_", correctOrderedNames) + "_UNIQUE";
    db.getJooq().alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    MetadataUtils.deleteUnique(this, correctOrderedNames);
    super.removeUnique(correctOrderedNames);
  }

  @Override
  public void enableSearch() {

    // 1. add tsvector column with index
    db.getJooq()
        .execute(
            "ALTER TABLE {0} ADD COLUMN {1} tsvector",
            getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));
    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    db.getJooq()
        .execute(
            "CREATE INDEX mg_search_vector_idx ON {0} USING GIN( {1} )",
            getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));

    // 3. createColumn the trigger function to automatically update the
    // MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction =
        String.format(
            "\"%s\".\"%s_search_vector_trigger\"()", getSchema().getName(), getTableName());

    StringBuilder mgSearchVector = new StringBuilder("to_tsvector('english', ' '");
    for (ColumnMetadata c : getColumns()) {
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

    // 4. add trigger to update the tsvector on each insert or update
    db.getJooq()
        .execute(
            "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
                + triggerfunction,
            name(MG_SEARCH_INDEX_COLUMN_NAME),
            getJooqTable());
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    SqlColumnMetadata c = this.addColumn(MG_EDIT_ROLE, STRING);
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
