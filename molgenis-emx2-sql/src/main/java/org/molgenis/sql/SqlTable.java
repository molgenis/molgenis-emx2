package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Table;
import org.molgenis.beans.TableMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Role.*;
import static org.molgenis.Type.*;
import static org.molgenis.sql.MetadataUtils.*;

class SqlTable extends TableMetadata implements Table {
  public static final String MG_EDIT_ROLE = "MG_EDIT_ROLE";
  public static final String MG_SEARCH_INDEX_COLUMN_NAME = "MG_SEARCH_VECTOR";
  public static final String MG_ROLE_PREFIX = "MG_ROLE_";
  public static final String MG_USER = "MG_USER_";
  public static final String DEFER_SQL = "SET CONSTRAINTS ALL DEFERRED";
  private DSLContext jooq;

  SqlTable(SqlSchema schema, String name) {
    super(schema, name);
    this.jooq = schema.getJooq();
  }

  void load() throws MolgenisException {
    loadColumnMetadata(this, columns);
    loadTableMetadata(this);
    loadUniqueMetadata(this);
  }

  void createTable() throws MolgenisException {
    Name tableName = name(getSchemaName(), getName());
    jooq.createTable(tableName).columns().execute();
    saveTableMetadata(this);

    // grant rights to schema manager, editor and viewer roles
    jooq.execute(
        "GRANT SELECT ON {0} TO {1}",
        tableName, name(MG_ROLE_PREFIX + getSchemaName().toUpperCase() + VIEWER));
    jooq.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(MG_ROLE_PREFIX + getSchemaName().toUpperCase() + EDITOR));
    jooq.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        tableName, name(MG_ROLE_PREFIX + getSchemaName().toUpperCase() + MANAGER));

    // add default molgenisid primary key column
    this.addColumn(MOLGENISID, UUID).primaryKey();
  }

  @Override
  public SqlTable setPrimaryKey(String... columnNames) throws MolgenisException {
    if (columnNames.length == 0)
      throw new MolgenisException("Primary key requires 1 or more columns");
    Name[] keyNames = Stream.of(columnNames).map(DSL::name).toArray(Name[]::new);

    // drop primary
    jooq.execute(
        "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}", getJooqTable(), name(getName() + "_pkey"));

    jooq.alterTable(getJooqTable()).add(constraint().primaryKey(keyNames)).execute();
    super.setPrimaryKey(columnNames);
    saveTableMetadata(this);
    return this;
  }

  public List<Field> getPrimaryKeyFields() throws MolgenisException {
    ArrayList<Field> keyFields = new ArrayList<>();
    for (String key : getPrimaryKey()) {
      keyFields.add(getJooqField(getColumn(key)));
    }
    return keyFields;
  }

  @Override
  public void enableSearch() {

    // 1. add tsvector column with index
    jooq.execute(
        "ALTER TABLE {0} ADD COLUMN {1} tsvector",
        getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));
    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    jooq.execute(
        "CREATE INDEX mg_search_vector_idx ON {0} USING GIN( {1} )",
        getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));

    // 3. createColumn the trigger function to automatically update the
    // MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction =
        String.format("\"%s\".\"%s_search_vector_trigger\"()", getSchema().getName(), getName());

    StringBuilder mgSearchVector = new StringBuilder("to_tsvector('english', ' '");
    for (Column c : getColumns()) {
      if (!c.getName().startsWith("MG_"))
        mgSearchVector.append(
            String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName()));
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

    jooq.execute(functionBody);

    // 4. add trigger to update the tsvector on each insert or update
    jooq.execute(
        "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
            + triggerfunction,
        name(MG_SEARCH_INDEX_COLUMN_NAME),
        getJooqTable());
  }

  @Override
  public void enableRowLevelSecurity() throws MolgenisException {
    SqlColumn c = this.addColumn(MG_EDIT_ROLE, STRING);
    c.setIndexed(true);

    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    jooq.execute(
        "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
        name("RLS/" + getSchema().getName() + "/" + getName()), getJooqTable(), name(MG_EDIT_ROLE));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  @Override
  public SqlColumn addColumn(String name, Type type) throws MolgenisException {
    SqlColumn c = new SqlColumn(this, name, type);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  @Override
  public Column addRef(String name, String toTable, String toColumn) throws MolgenisException {
    RefSqlColumn c = new RefSqlColumn(this, name, toTable, toColumn);
    c.createColumn();
    this.addColumn(c);
    return c;
  }

  @Override
  public Column addRefArray(String name, String toTable, String toColumn) throws MolgenisException {
    RefArraySqlColumn c = new RefArraySqlColumn(this, name, toTable, toColumn);
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
  public MrefSqlColumn addMref(
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTable)
      throws MolgenisException {
    MrefSqlColumn c =
        new MrefSqlColumn(
            this, name, refTable, refColumn, reverseName, reverseRefColumn, joinTable);
    c.createColumn();
    columns.put(name, c);
    return c;
  }

  protected void addMrefReverse(MrefSqlColumn reverse) {
    columns.put(reverse.getName(), reverse);
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    jooq.alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  @Override
  public Unique addUnique(String... columnNames) throws MolgenisException {

    String uniqueName = getName() + "_" + String.join("_", columnNames) + "_UNIQUE";
    jooq.alterTable(getJooqTable()).add(constraint(name(uniqueName)).unique(columnNames)).execute();

    Unique unique = super.addUnique(columnNames);
    saveUnique(jooq, unique);
    return unique;
  }

  @Override
  public boolean unique(String... keys) {
    try {
      getUniqueName(keys);
      return true;
    } catch (MolgenisException e) {
      return false;
    }
  }

  @Override
  public void removeUnique(String... keys) throws MolgenisException {
    String uniqueName = getUniqueName(keys);
    jooq.alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    super.removeUnique(keys);
  }

  @Override
  public int insert(Collection<Row> rows) throws MolgenisException {
    return insert(rows.toArray(new Row[rows.size()]));
  }

  @Override
  public int insert(Row... rows) throws MolgenisException {
    AtomicInteger count = new AtomicInteger(0);
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute(DEFER_SQL);
            // get metadata
            List<Field> fields = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();
            for (Column c : getColumns()) {
              fieldNames.add(c.getName());
              fields.add(getJooqField(c));
            }
            InsertValuesStepN step =
                DSL.using(config)
                    .insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
            for (Row row : rows) {
              step.values(SqlTypeUtils.getValuesAsCollection(row, this));
            }
            count.set(step.execute());
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(e);
    }
    return count.get();
  }

  @Override
  public int update(Row... rows) throws MolgenisException {
    AtomicInteger count = new AtomicInteger(0);
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute(DEFER_SQL);

            // keep batchsize smaller to limit memory footprint
            int batchSize = 1000;

            // get metadata
            ArrayList<Field> fields = new ArrayList<>();
            ArrayList<String> fieldNames = new ArrayList<>();
            for (Column c : getColumns()) {
              fieldNames.add(c.getName());
              fields.add(getJooqField(c));
            }

            List<Field> keyFields = getPrimaryKeyFields();

            // execute in batches
            List<Row> batch = new ArrayList<>();
            for (Row row : rows) {
              batch.add(row);
              count.set(count.get() + 1);
              if (count.get() % batchSize == 0) {
                updateBatch(batch, getJooqTable(), fields, fieldNames, keyFields);
                batch.clear();
              }
            }
            updateBatch(batch, getJooqTable(), fields, fieldNames, keyFields);
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(e);
    }
    return count.get();
  }

  @Override
  public int update(Collection<Row> rows) throws MolgenisException {
    return update(rows.toArray(new Row[rows.size()]));
  }

  private void updateBatch(
      Collection<Row> rows,
      org.jooq.Table t,
      List<Field> fields,
      List<String> fieldNames,
      List<Field> keyFields)
      throws MolgenisException {
    if (!rows.isEmpty()) {
      // createColumn multi-value insert
      InsertValuesStepN step = jooq.insertInto(t, fields.toArray(new Field[fields.size()]));
      for (Row row : rows) {
        step.values(SqlTypeUtils.getValuesAsCollection(row, this));
      }
      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep step2 = step.onConflict(keyFields).doUpdate();
      for (String name : fieldNames) {
        step2 =
            step2.set(field(name(name)), (Object) field(unquotedName("excluded.\"" + name + "\"")));
      }
      step.execute();
    }
  }

  @Override
  public int delete(Collection<Row> rows) throws MolgenisException {
    return delete(rows.toArray(new Row[rows.size()]));
  }

  @Override
  public void deleteByPrimaryKey(Object... key) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public int delete(Row... rows) throws MolgenisException {

    AtomicInteger count = new AtomicInteger(0);
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute(DEFER_SQL);

            // because of expensive jTable scanning and smaller queryOld string size this batch
            // should be
            // larger
            // than insert/update
            int batchSize = 100000;
            List<Row> batch = new ArrayList<>();
            for (Row row : rows) {
              batch.add(row);
              count.set(count.get() + 1);
              if (count.get() % batchSize == 0) {
                deleteBatch(batch);
                batch.clear();
              }
            }
            deleteBatch(batch);
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(e);
    }
    return count.get();
  }

  private void deleteBatch(Collection<Row> rows) throws MolgenisException {
    if (!rows.isEmpty()) {
      String[] keyNames = getPrimaryKey();

      Condition whereCondition = null;
      for (Row r : rows) {
        Condition rowCondition = null;
        for (String keyName : keyNames) {
          Column key = getColumn(keyName);
          if (rowCondition == null) {
            rowCondition = getJooqField(key).eq(r.get(key.getType(), keyName));
          } else {
            rowCondition = rowCondition.and(getJooqField(key).eq(r.get(key.getType(), keyName)));
          }
        }
        if (whereCondition == null) {
          whereCondition = rowCondition;
        } else {
          whereCondition = whereCondition.or(rowCondition);
        }
      }
      jooq.deleteFrom(getJooqTable()).where(whereCondition).execute();
    }
  }

  @Override
  public Query query() {
    return new SqlQuery(this, jooq);
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    return this.query().retrieve();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException {
    return query().retrieve(columnName, klazz);
  }

  org.jooq.Table getJooqTable() {
    return table(name(getSchema().getName(), getName()));
  }

  private Field getJooqField(Column c) throws MolgenisException {
    return field(name(c.getName()), SqlTypeUtils.jooqTypeOf(c));
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  public boolean exists() {
    return !getColumns().isEmpty();
  }

  public void dropTable() {
    jooq.dropTable(name(getSchemaName(), getName())).execute();
    deleteTable(this);
  }

  protected void loadPrimaryKey(String[] pkey) throws MolgenisException {
    super.setPrimaryKey(pkey);
  }

  protected void loadUnique(String[] columns) throws MolgenisException {
    super.addUnique(columns);
  }
}
