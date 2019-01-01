package org.molgenis.sql.psql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.*;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class PsqlTable implements SqlTable {
  private DSLContext sql;
  private PsqlDatabase db;
  private String name;
  private Map<String, PsqlColumn> columnMap = new LinkedHashMap<>();
  private Map<String, SqlUnique> uniquesMap = new LinkedHashMap<>();

  PsqlTable(PsqlDatabase db, DSLContext sql, String name) {
    this.db = db;
    this.sql = sql;
    this.name = name;
    reloadMetaData();
  }

  private void reloadMetaData() {
    Table t = sql.meta().getTables(name).get(0);
    reloadColumns(t);
    reloadUniques(t);
    reloadReferences(t);
  }

  private void reloadColumns(Table t) {
    columnMap = new LinkedHashMap<>();
    for (Field field : t.fields()) {
      columnMap.put(field.getName(), new PsqlColumn(sql, this, field, null));
    }
  }

  private void reloadReferences(Table t) {
    for (Object o3 : t.getReferences()) {
      ForeignKey fk = (ForeignKey) o3;
      for (Field field : (List<Field>) fk.getFields()) {
        PsqlColumn temp = columnMap.get(field.getName());
        PsqlColumn fkey = null;
        String refTableName = fk.getKey().getTable().getName();
        if (refTableName.equals(name)) {
          fkey = new PsqlColumn(sql, this, field, this);
        } else {
          fkey = new PsqlColumn(sql, this, field, db.getTable(refTableName));
        }
        fkey.setNullable(temp.isNullable());
        columnMap.put(field.getName(), fkey);
      }
    }
  }

  @Override
  public Collection<SqlColumn> getColumns() {
    Collection<SqlColumn> cols = new ArrayList<>();
    cols.addAll(columnMap.values());
    return Collections.unmodifiableCollection(cols);
  }

  @Override
  public SqlColumn getColumn(String name) {
    return columnMap.get(name);
  }

  @Override
  public SqlColumn addColumn(String name, SqlType sqlType) throws SqlDatabaseException {
    if (SqlType.REF.equals(sqlType)) {
      throw new SqlDatabaseException(
          "addColumn(name,REF) not allowed. Use addColumn(name, otherTable) to add foreign key fields");
    }
    DataType type = PsqlTypeUtils.typeOf(sqlType);
    Field field = field(name(name), type.nullable(false));
    sql.alterTable(name(this.name)).addColumn(field).execute();
    columnMap.put(name, new PsqlColumn(sql, this, field, null));
    return getColumn(name);
  }

  @Override
  public SqlColumn addColumn(String name, SqlTable otherTable) {
    Field field = field(name(name), SQLDataType.UUID.nullable(false));

    sql.alterTable(name(this.name)).addColumn(field).execute();
    sql.alterTable(name(this.name))
        .add(
            constraint(name(this.name) + "_" + name(name) + "_FK")
                .foreignKey(name(name))
                .references(name(otherTable.getName()), name(MOLGENISID)))
        .execute();
    sql.createIndex(name(this.name) + "_" + name(name) + "_FKINDEX")
        .on(table(name(this.name)), field)
        .execute();
    columnMap.put(name, new PsqlColumn(sql, this, field, otherTable));
    return getColumn(name);
  }

  @Override
  public void removeColumn(String name) throws SqlDatabaseException {
    if (MOLGENISID.equals(name))
      throw new SqlDatabaseException(
          "You are not allowed to remove primary key column " + MOLGENISID);
    sql.alterTable(name(this.name)).dropColumn(field(name(name))).execute();
    columnMap.remove(name);
  }

  private void reloadUniques(Table t) {
    uniquesMap = new LinkedHashMap<>();
    for (Index i : (List<Index>) t.getIndexes()) {
      // skip non-unique indexes
      if (i.getUnique()) {
        List<SqlColumn> cols = new ArrayList<>();
        for (SortField sf : i.getFields()) {
          cols.add(getColumn(sf.getName()));
        }
        uniquesMap.put(i.getName(), new PsqlUnique(this, cols));
      }
    }
  }

  @Override
  public Collection<SqlUnique> getUniques() {
    return Collections.unmodifiableCollection(uniquesMap.values());
  }

  private String getUniqueName(String... keys) throws SqlDatabaseException {
    List<String> keyList = Arrays.asList(keys);
    for (Map.Entry<String, SqlUnique> el : this.uniquesMap.entrySet()) {
      if (el.getValue().getColumns().size() == keyList.size()
          && el.getValue().getColumnNames().containsAll(keyList)) {
        return el.getKey();
      }
    }
    throw new SqlDatabaseException(
        "getUniqueName(" + keyList + ") failed: constraint unknown in table " + this.name);
  }

  public SqlUnique addUnique(String... keys) throws SqlDatabaseException {
    List<SqlColumn> uniqueColumns = new ArrayList<>();
    for (String key : keys) {
      SqlColumn col = getColumn(key);
      if (col == null)
        throw new SqlDatabaseException(
            "addUnique(" + keys + ") failed: column '" + key + "' unknown in table " + this.name);
      uniqueColumns.add(col);
    }
    String uniqueName = name + "_" + String.join("_", keys) + "_UNIQUE";
    sql.alterTable(name(name)).add(constraint(name(uniqueName)).unique(keys)).execute();
    uniquesMap.put(uniqueName, new PsqlUnique(this, uniqueColumns));
    return this.uniquesMap.get(getUniqueName(keys));
  }

  @Override
  public void removeUnique(String... keys) throws SqlDatabaseException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new SqlDatabaseException(
          "You are not allowed to remove unique constraint on primary key column " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    sql.alterTable(name(this.name)).dropConstraint(name(uniqueName)).execute();
    uniquesMap.remove(uniqueName);
  }

  public String getName() {
    return name;
  }

  @Override
  public void insert(Collection<SqlRow> rows) throws SqlDatabaseException {
    try {
      // get metadata
      Field[] fields = new Field[columnMap.size()];
      String[] fieldNames = new String[columnMap.size()];
      int i = 0;
      for (PsqlColumn c : columnMap.values()) {
        fieldNames[i] = c.getName();
        fields[i] = c.getJooqField();
        i++;
      }
      InsertValuesStepN step = sql.insertInto(table(name(name)), fields);
      for (SqlRow row : rows) {
        step.values(row.values(fieldNames));
      }
      step.execute();
    } catch (DataAccessException e) {
      throw new SqlDatabaseException(e.getCause().getMessage());
    }
  }

  @Override
  public void insert(SqlRow row) throws SqlDatabaseException {
    this.insert(Arrays.asList(row));
  }

  public int update(Collection<SqlRow> rows) throws SqlDatabaseException {
    // keep batchsize smaller to limit memory footprint
    int batchSize = 1000;

    // get metadata
    Field[] fields = new Field[columnMap.size()];
    String[] fieldNames = new String[columnMap.size()];
    int i = 0;
    for (PsqlColumn c : columnMap.values()) {
      fieldNames[i] = c.getName();
      fields[i] = c.getJooqField();
      i++;
    }
    // execute in batches
    int count = 0;
    List<SqlRow> batch = new ArrayList<>();
    for (SqlRow row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        updateBatch(batch, table(name(name)), fields, fieldNames);
        batch.clear();
      }
    }
    updateBatch(batch, table(name(name)), fields, fieldNames);
    return count;
  }

  private void updateBatch(Collection<SqlRow> rows, Table t, Field[] fields, String[] fieldNames) {
    if (rows.size() > 0) {
      // create multi-value insert
      InsertValuesStepN step = sql.insertInto(t, fields);
      for (SqlRow row : rows) {
        step.values(row.values(fieldNames));
      }
      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep step2 = step.onConflict(field(MOLGENISID)).doUpdate();
      for (int i = 0; i < fieldNames.length; i++) {
        if (!MOLGENISID.equals(fieldNames[i])) {
          step2.set(
              field(fieldNames[i]), (Object) field(unquotedName("\"excluded\"." + fieldNames[i])));
        }
      }
      step.execute();
    }
  }

  @Override
  public void update(SqlRow row) throws SqlDatabaseException {
    this.update(Arrays.asList(row));
  }

  @Override
  public int delete(Collection<SqlRow> rows) {
    // because of expensive table scanning and smaller query string size this batch should be larger
    // than insert/update
    int batchSize = 100000;
    Table t = table(name(name));
    int count = 0;
    List<SqlRow> batch = new ArrayList<>();
    for (SqlRow row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        deleteBatch(batch, t);
        batch.clear();
      }
    }
    deleteBatch(batch, t);
    return count;
  }

  private void deleteBatch(Collection<SqlRow> rows, Table t) {
    if (rows.size() > 0) {
      Field field = field(name(MOLGENISID), SQLDataType.UUID);
      List<UUID> idList = new ArrayList<>();
      rows.forEach(row -> idList.add(row.getRowID()));
      sql.deleteFrom(t).where(field.in(idList)).execute();
    }
  }

  @Override
  public void delete(SqlRow row) {
    this.delete(Arrays.asList(row));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("TABLE(").append(name).append("){");
    for (SqlColumn c : getColumns()) {
      builder.append("\n\t").append(c.toString());
    }
    for (SqlUnique u : getUniques()) {
      builder.append("\n\t").append(u.toString());
    }
    builder.append("\n}");
    return builder.toString();
  }
}
