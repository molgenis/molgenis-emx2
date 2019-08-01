package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.beans.TableBean;
import org.postgresql.util.PSQLException;

import java.util.*;

import static org.jooq.impl.DSL.*;

import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles._EDITOR;
import static org.molgenis.Database.Roles._MANAGER;
import static org.molgenis.Database.Roles._VIEWER;
import static org.molgenis.Database.RowLevelSecurity.MG_EDIT_ROLE;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;
import static org.molgenis.sql.MetadataUtils.*;

class SqlTable extends TableBean {
  public static final String MG_SEARCH_VECTOR = "mg_search_vector";
  private DSLContext jooq;

  SqlTable(SqlSchema schema, String name) throws MolgenisException {
    super(schema, name);
    this.jooq = schema.jooq;
    loadColumnMetadata(this, columns);
    loadUniqueMetadata(this, uniques);
  }

  void createTable() throws MolgenisException {
    Name tableName = name(getSchemaName(), getName());
    String uniqueName = "PK_" + getName();

    // createTable the table
    jooq.createTableIfNotExists(tableName)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint(uniqueName).primaryKey(MOLGENISID))
        .execute();

    // grant rights to schema manager, editor and viewer roles
    jooq.execute(
        "GRANT SELECT ON {0} TO {1}",
        tableName, name(MGROLE_ + getSchemaName().toUpperCase() + _VIEWER));
    jooq.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(MGROLE_ + getSchemaName().toUpperCase() + _EDITOR));
    jooq.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        tableName, name(MGROLE_ + getSchemaName().toUpperCase() + _MANAGER));

    // save the metdata
    saveTableMetadata(this);

    SqlColumn c = new SqlColumn(this, MOLGENISID, UUID, true);
    columns.put(MOLGENISID, c);
    saveColumn(c);

    super.addUnique(MOLGENISID);
  }

  @Override
  public void enableSearch() {

    // 1. add tsvector column with index
    jooq.execute("ALTER TABLE {0} ADD COLUMN {1} tsvector", getJooqTable(), name(MG_SEARCH_VECTOR));
    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    jooq.execute(
        "CREATE INDEX mg_search_vector_idx ON {0} USING GIN( {1} )",
        getJooqTable(), name(MG_SEARCH_VECTOR));

    // 3. createColumn the trigger function to automatically update the MG_SEARCH_VECTOR
    String triggerfunction =
        String.format("\"%s\".\"%s_search_vector_trigger\"()", getSchema().getName(), getName());

    String mgSearchVector = "to_tsvector('english', ' '";
    for (Column c : getColumns()) {
      if (!c.getName().startsWith("MG_"))
        mgSearchVector += String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName());
    }
    mgSearchVector += ")";

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.mg_search_vector:=%s ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction, mgSearchVector);

    jooq.execute(functionBody);

    // 4. add trigger to update the tsvector on each insert or update
    jooq.execute(
        "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
            + triggerfunction,
        name(MG_SEARCH_VECTOR),
        getJooqTable());
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    SqlColumn c = this.addColumn(MG_EDIT_ROLE.toString(), STRING);
    c.setIndexed(true);

    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
        name("RLS/" + getSchema().getName() + "/" + getName()),
        getJooqTable(),
        name(MG_EDIT_ROLE.toString()));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  @Override
  public SqlColumn addColumn(String name, Type type) throws MolgenisException {
    SqlColumn c = new SqlColumn(this, name, type, false);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public RefSqlColumn addRef(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    RefSqlColumn c = new RefSqlColumn(this, name, otherTable, otherColumn, false);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public RefArraySqlColumn addRefArray(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    RefArraySqlColumn c = new RefArraySqlColumn(this, name, otherTable, otherColumn, false);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public MrefSqlColumn addMref(String name, String toTable, String toColumn)
      throws MolgenisException {
    MrefSqlColumn c = new MrefSqlColumn(this, name, toTable, toColumn, false);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    if (MOLGENISID.equals(name))
      throw new MolgenisException("You are not allowed to remove primary key column " + MOLGENISID);
    jooq.alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  @Override
  public Unique addUnique(String... keys) throws MolgenisException {

    String uniqueName = getName() + "_" + String.join("_", keys) + "_UNIQUE";
    jooq.alterTable(getJooqTable()).add(constraint(name(uniqueName)).unique(keys)).execute();
    return super.addUnique(keys);
  }

  @Override
  public boolean isUnique(String... keys) {
    try {
      getUniqueName(keys);
      return true;
    } catch (MolgenisException e) {
      return false;
    }
  }

  @Override
  public void removeUnique(String... keys) throws MolgenisException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new MolgenisException(
          "You are not allowed to remove unique constraint on primary key column " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    jooq.alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    super.removeUnique(keys);
  }

  @Override
  public int insert(Collection<org.molgenis.Row> rows) throws MolgenisException {
    return insert(rows.toArray(new org.molgenis.Row[rows.size()]));
  }

  @Override
  public int insert(org.molgenis.Row... rows) throws MolgenisException {
    try {
      // get metadata
      List<Field> fields = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      for (Column c : getColumns()) {
        fieldNames.add(c.getName());
        fields.add(getJooqField(c));
      }
      InsertValuesStepN step =
          jooq.insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
      for (Row row : rows) {
        step.values(SqlTypeUtils.getValuesAsCollection(row, this));
      }
      return step.execute();
    } catch (DataAccessException e) {
      throw new MolgenisException(e.getCause(PSQLException.class).getMessage(), e);
    }
  }

  @Override
  public int update(org.molgenis.Row... rows) throws MolgenisException {

    // keep batchsize smaller to limit memory footprint
    int batchSize = 1000;

    // get metadata
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<String> fieldNames = new ArrayList<>();
    int i = 0;
    for (Column c : getColumns()) {
      fieldNames.add(c.getName());
      fields.add(getJooqField(c));
      i++;
    }
    // TODO update mref values

    // execute in batches
    int count = 0;
    List<org.molgenis.Row> batch = new ArrayList<>();
    for (org.molgenis.Row row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        updateBatch(batch, getJooqTable(), fields, fieldNames);
        batch.clear();
      }
    }
    updateBatch(batch, getJooqTable(), fields, fieldNames);
    return count;
  }

  @Override
  public int update(Collection<org.molgenis.Row> rows) throws MolgenisException {
    return update(rows.toArray(new org.molgenis.Row[rows.size()]));
  }

  private void updateBatch(
      Collection<org.molgenis.Row> rows,
      org.jooq.Table t,
      List<Field> fields,
      List<String> fieldNames)
      throws MolgenisException {
    if (!rows.isEmpty()) {
      // createColumn multi-value insert
      InsertValuesStepN step = jooq.insertInto(t, fields.toArray(new Field[fields.size()]));
      for (org.molgenis.Row row : rows) {
        step.values(SqlTypeUtils.getValuesAsCollection(row, this));
      }
      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep step2 = step.onConflict(field(MOLGENISID)).doUpdate();
      for (String name : fieldNames) {
        if (!MOLGENISID.equals(name)) {
          step2.set(
              field(name(name)), (Object) field(unquotedName("\"excluded\".\"" + name + "\"")));
        }
      }
      step.execute();
    }
  }

  @Override
  public int delete(Collection<org.molgenis.Row> rows) throws MolgenisException {
    return delete(rows.toArray(new org.molgenis.Row[rows.size()]));
  }

  @Override
  public int delete(org.molgenis.Row... rows) throws MolgenisException {

    // because of expensive jTable scanning and smaller queryOld string size this batch should be
    // larger
    // than insert/update
    int batchSize = 100000;
    int count = 0;
    List<org.molgenis.Row> batch = new ArrayList<>();
    for (org.molgenis.Row row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        deleteBatch(batch);
        batch.clear();
      }
    }
    deleteBatch(batch);
    return count;
  }

  private void deleteBatch(Collection<org.molgenis.Row> rows) throws MolgenisException {
    try {
      if (!rows.isEmpty()) {
        Field field = field(name(MOLGENISID), SQLDataType.UUID);
        List<UUID> idList = new ArrayList<>();
        rows.forEach(row -> idList.add(row.getMolgenisid()));
        jooq.deleteFrom(getJooqTable()).where(field.in(idList)).execute();
      }
    } catch (DataAccessException e) {
      throw new MolgenisException(e.getCause(PSQLException.class).getMessage(), e);
    }
  }

  @Override
  public Query query() {
    return new SqlQuery(this, jooq);
  }

  @Override
  public List<org.molgenis.Row> retrieve() throws MolgenisException {
    return this.query().retrieve();
  }

  org.jooq.Table getJooqTable() {
    return table(name(getSchema().getName(), getName()));
  }

  private Field getJooqField(Column c) throws MolgenisException {
    return field(name(c.getName()), SqlTypeUtils.jooqTypeOf(c));
  }

  public DSLContext getJooq() {
    return jooq;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public void dropTable() {
    jooq.dropTable(name(getSchemaName(), getName())).execute();
    deleteTable(this);
  }
}
