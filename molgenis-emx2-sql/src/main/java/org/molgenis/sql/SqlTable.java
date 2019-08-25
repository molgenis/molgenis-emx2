package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.metadata.*;
import org.molgenis.query.Query;
import org.molgenis.data.Row;
import org.molgenis.data.Table;
import org.molgenis.query.Select;
import org.molgenis.query.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jooq.impl.DSL.*;

class SqlTable implements Table {
  public static final String MG_EDIT_ROLE = "MG_EDIT_ROLE";
  public static final String MG_SEARCH_INDEX_COLUMN_NAME = "MG_SEARCH_VECTOR";
  public static final String MG_ROLE_PREFIX = "MG_ROLE_";
  public static final String DEFER_SQL = "SET CONSTRAINTS ALL DEFERRED";

  private SqlDatabase db;
  private TableMetadata metadata;

  SqlTable(SqlDatabase db, TableMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public org.molgenis.data.Schema getSchema() {
    return new SqlSchema(db, metadata.getSchema());
  }

  @Override
  public TableMetadata getMetadata() {
    return metadata;
  }

  public int insert(Collection<Row> rows) throws MolgenisException {
    return insert(rows.toArray(new Row[rows.size()]));
  }

  @Override
  public int insert(Row... rows) throws MolgenisException {
    AtomicInteger count = new AtomicInteger(0);
    try {
      db.getJooq()
          .transaction(
              config -> {
                DSL.using(config).execute(DEFER_SQL);
                // get metadata
                List<Field> fields = new ArrayList<>();
                List<String> fieldNames = new ArrayList<>();
                for (ColumnMetadata c : getMetadata().getColumns()) {
                  fieldNames.add(c.getColumnName());
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
      db.getJooq()
          .transaction(
              config -> {
                DSL.using(config).execute(DEFER_SQL);

                // keep batchsize smaller to limit memory footprint
                int batchSize = 1000;

                // get metadata
                ArrayList<Field> fields = new ArrayList<>();
                ArrayList<String> fieldNames = new ArrayList<>();
                for (ColumnMetadata c : getMetadata().getColumns()) {
                  fieldNames.add(c.getColumnName());
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
      InsertValuesStepN step = db.getJooq().insertInto(t, fields.toArray(new Field[fields.size()]));
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
  public Select select(String... path) {
    return null;
  }

  @Override
  public Where where(String... path) {
    return null;
  }

  @Override
  public int delete(Row... rows) throws MolgenisException {

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.getJooq()
          .transaction(
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
      String[] keyNames = getMetadata().getPrimaryKey();

      Condition whereCondition = null;
      for (Row r : rows) {
        Condition rowCondition = null;
        for (String keyName : keyNames) {
          ColumnMetadata key = getMetadata().getColumn(keyName);
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
      db.getJooq().deleteFrom(getJooqTable()).where(whereCondition).execute();
    }
  }

  @Override
  public Query query() {
    return new SqlQuery(this.getMetadata(), db.getJooq());
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    return this.query().retrieve();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException {
    return query().retrieve(columnName, klazz);
  }

  @Override
  public String getName() {
    return getMetadata().getTableName();
  }

  private List<Field> getPrimaryKeyFields() throws MolgenisException {
    ArrayList<Field> keyFields = new ArrayList<>();
    for (String key : getMetadata().getPrimaryKey()) {
      keyFields.add(getJooqField(getMetadata().getColumn(key)));
    }
    return keyFields;
  }

  private org.jooq.Table getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
  }

  private Field getJooqField(ColumnMetadata c) throws MolgenisException {
    return field(name(c.getColumnName()), SqlTypeUtils.jooqTypeOf(c));
  }
}
