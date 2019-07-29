package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.TableBean;
import org.postgresql.util.PSQLException;

import java.util.*;

import static org.jooq.impl.DSL.*;

import static org.molgenis.Database.RowLevelSecurity.MG_EDIT_ROLE;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;

class SqlTable extends TableBean {
  public static final String MG_SEARCH_VECTOR = "mg_search_vector";
  private DSLContext sql;
  private boolean isLoading = false;

  SqlTable(Schema schema, DSLContext sql, String name) throws MolgenisException {
    super(schema, name);
    this.sql = sql;
    // load default columns and uniques
    isLoading = true;
    this.addColumn(MOLGENISID, Type.UUID);
    this.addUnique(MOLGENISID);
    isLoading = false;
  }

  /** will be called from SqlSchema */
  protected void loadMrefs() throws MolgenisException {

    this.isLoading = true;

    // check all tables for mref tables, probably expensive
    for (String mrefTableName : getSchema().getTableNames()) {
      Table mrefTable = getSchema().getTable(mrefTableName);

      // test if it is 'our' mref jTable]
      boolean valid = true;
      Column self = null;
      Column other = null;
      for (Column c : mrefTable.getColumns()) {
        if (c.getRefTable() != null) {
          if (c.getRefTable().equals(this.getName())) {
            if (self != null) valid = false;
            else self = c;
          } else {
            if (other != null) valid = false;
            else other = c;
          }
        }
      }
      if (valid && self != null && other != null) {
        this.addMref(other.getName(), other.getRefTable(), other.getName());
      }
    }

    this.isLoading = false;
  }

  @Override
  public void enableSearch() {

    // 1. add tsvector column with index
    sql.execute("ALTER TABLE {0} ADD COLUMN {1} tsvector", getJooqTable(), name(MG_SEARCH_VECTOR));
    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    sql.execute(
        "CREATE INDEX mg_search_vector_idx ON {0} USING GIN( {1} )",
        getJooqTable(), name(MG_SEARCH_VECTOR));

    // 3. createColumn the trigger function to automatically update the MG_SEARCH_VECTOR
    String triggerfunction =
        String.format("\"%s\".\"%s_search_vector_trigger\"()", getSchema().getName(), getName());

    String mg_search_vector = "to_tsvector('english', ' '";
    for (Column c : getColumns()) {
      if (!c.getName().startsWith("MG_"))
        mg_search_vector += String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName());
    }
    mg_search_vector += ")";
    //     to_tsvector('english', coalesce(title,'') || ' ' || coalesce(body,''));

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.mg_search_vector:=%s ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction, mg_search_vector);

    System.out.println(functionBody);

    sql.execute(functionBody);

    // 4. add trigger to update the tsvector on each insert or update
    sql.execute(
        "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
            + triggerfunction,
        name(MG_SEARCH_VECTOR),
        getJooqTable());
    // TODO: retrospectively fill the tsv column

  }

  protected void loadColumn(SimpleSqlColumn c) {
    this.columns.put(c.getName(), c);
  }

  protected void loadUnique(List<String> columns) throws MolgenisException {
    super.addUnique(columns.toArray(new String[columns.size()]));
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    SimpleSqlColumn c = this.addColumn(MG_EDIT_ROLE.toString(), STRING);
    c.setIndexed(true);

    sql.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    sql.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
        name("RLS/" + getSchema().getName() + "/" + getName()),
        getJooqTable(),
        name(MG_EDIT_ROLE.toString()));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  @Override
  public SimpleSqlColumn addColumn(String name, Type type) throws MolgenisException {
    SimpleSqlColumn c = new SimpleSqlColumn(sql, this, name, type);
    if (!isLoading) {
      c.createColumn();
    }
    columns.put(name, c);
    return c;
  }

  @Override
  public RefSqlColumn addRef(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    RefSqlColumn c = new RefSqlColumn(sql, this, name, otherTable, otherColumn);
    if (!isLoading) {
      c.createColumn();
    }
    columns.put(name, c);
    return c;
  }

  @Override
  public RefArraySqlColumn addRefArray(String name, String otherTable, String otherColumn)
      throws MolgenisException {
    RefArraySqlColumn c = new RefArraySqlColumn(sql, this, name, otherTable, otherColumn);
    if (!isLoading) {
      c.createColumn();
    }
    columns.put(name, c);
    return c;
  }

  @Override
  public MrefSqlColumn addMref(String name, String toTable, String toColumn)
      throws MolgenisException {
    MrefSqlColumn c = new MrefSqlColumn(sql, this, name, MREF, toTable, toColumn);
    if (!isLoading) {
      c.createColumn();
    }
    columns.put(name, c);
    return c;
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    if (MOLGENISID.equals(name))
      throw new MolgenisException("You are not allowed to remove primary key column " + MOLGENISID);
    sql.alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  public Unique addUnique(String... keys) throws MolgenisException {
    if (!isLoading) {
      String uniqueName = getName() + "_" + String.join("_", keys) + "_UNIQUE";
      sql.alterTable(getJooqTable()).add(constraint(name(uniqueName)).unique(keys)).execute();
    }
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
    sql.alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    super.removeUnique(keys);
  }

  @Override
  public int insert(Collection<org.molgenis.Row> rows) throws MolgenisException {
    return insert(rows.toArray(new org.molgenis.Row[rows.size()]));
  }

  @Override
  public int insert(org.molgenis.Row... rows) throws MolgenisException {
    Row currentRow = null;
    try {
      // get metadata
      List<Field> fields = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      int i = 0;
      for (Column c : getColumns()) {
        fieldNames.add(c.getName());
        fields.add(getJooqField(c));
        i++;
      }
      InsertValuesStepN step =
          sql.insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
      for (Row row : rows) {
        currentRow = row;
        step.values(SqlTypeUtils.getValuesAsCollection(row, this));
      }
      step.execute();
      return i;
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
      InsertValuesStepN step = sql.insertInto(t, fields.toArray(new Field[fields.size()]));
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
        sql.deleteFrom(getJooqTable()).where(field.in(idList)).execute();
      }
    } catch (DataAccessException e) {
      throw new MolgenisException(e.getCause(PSQLException.class).getMessage(), e);
    }
  }

  @Override
  public Query query() {
    return new SqlQuery(this, sql);
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
}
