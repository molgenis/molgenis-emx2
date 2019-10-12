package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.utils.TypeUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;

class SqlTable implements Table {

  public static final String DEFER_SQL = "SET CONSTRAINTS ALL DEFERRED";

  private SqlDatabase db;
  private TableMetadata metadata;

  SqlTable(SqlDatabase db, TableMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public org.molgenis.emx2.Schema getSchema() {
    return new SqlSchema(db, metadata.getSchema());
  }

  @Override
  public TableMetadata getMetadata() {
    return metadata;
  }

  public int insert(Iterable<Row> rows) {
    AtomicInteger count = new AtomicInteger(0);
    try {
      db.getJooq()
          .transaction(
              config -> {
                using(config).execute(DEFER_SQL);
                // get metadata
                List<Field> fields = new ArrayList<>();
                List<String> fieldNames = new ArrayList<>();
                for (Column c : getMetadata().getColumns()) {
                  fieldNames.add(c.getColumnName());
                  fields.add(getJooqField(c));
                }
                InsertValuesStepN step =
                    using(config)
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
  public int insert(Row... rows) {
    return insert(Arrays.asList(rows));
  }

  @Override
  public int update(Row... rows) {
    return update(Arrays.asList(rows));
  }

  @Override
  public int update(Iterable<Row> rows) {

    if (getPrimaryKeyFields().isEmpty())
      throw new MolgenisException(
          "invalid_table_definition",
          "Cannot update because primary key is not defined",
          "Table " + getName() + " cannot process row update requests. First define primary key");

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.getJooq()
          .transaction(
              config -> {
                using(config).execute(DEFER_SQL);

                // keep batchsize smaller to limit memory footprint
                int batchSize = 1000;

                // get metadata
                ArrayList<Field> fields = new ArrayList<>();
                ArrayList<String> fieldNames = new ArrayList<>();
                for (Column c : getMetadata().getColumns()) {
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

  private void updateBatch(
      Collection<Row> rows,
      org.jooq.Table t,
      List<Field> fields,
      List<String> fieldNames,
      List<Field> keyFields) {
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
  public int delete(Iterable<Row> rows) {
    AtomicInteger count = new AtomicInteger(0);
    try {
      db.getJooq()
          .transaction(
              config -> {
                using(config).execute(DEFER_SQL);

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

  @Override
  public void deleteByPrimaryKey(Object... key) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public Query select(String... path) {
    return query().select(path);
  }

  @Override
  public Query where(String path, Operator operator, Serializable... values) {
    return query().where(path, operator, values);
  }

  @Override
  public int delete(Row... rows) {
    return delete(Arrays.asList(rows));
  }

  private void deleteBatch(Collection<Row> rows) {
    if (!rows.isEmpty()) {
      String[] keyNames = getMetadata().getPrimaryKey();

      // in case no primary key is defined, use all columns
      if (keyNames.length == 0) {
        List<Column> allColumns = getMetadata().getColumns();
        keyNames = new String[allColumns.size()];
        for (int i = 0; i < keyNames.length; i++) {
          keyNames[i] = allColumns.get(i).getColumnName();
        }
      }

      Condition whereCondition = getWhereConditionForBatchDelete(rows, keyNames);
      db.getJooq().deleteFrom(getJooqTable()).where(whereCondition).execute();
    }
  }

  private Condition getWhereConditionForBatchDelete(Collection<Row> rows, String[] keyNames) {
    Condition whereCondition = null;
    for (Row r : rows) {
      Condition rowCondition = null;
      for (String keyName : keyNames) {
        rowCondition = getRowConditionForBatchDelete(r, rowCondition, keyName);
      }
      if (whereCondition == null) {
        whereCondition = rowCondition;
      } else {
        whereCondition = whereCondition.or(rowCondition);
      }
    }
    return whereCondition;
  }

  private Condition getRowConditionForBatchDelete(Row r, Condition rowCondition, String keyName) {
    Column key = getMetadata().getColumn(keyName);
    // consider to move this to helper methods
    ColumnType columnType = key.getColumnType();
    if (REF.equals(columnType)) {
      columnType =
          key.getTable()
              .getSchema()
              .getTableMetadata(key.getRefTableName())
              .getColumn(key.getRefColumnName())
              .getColumnType();
    } else if (REF_ARRAY.equals(columnType) || MREF.equals(columnType)) {
      columnType =
          TypeUtils.getArrayType(
              key.getTable()
                  .getSchema()
                  .getTableMetadata(key.getRefTableName())
                  .getColumn(key.getRefColumnName())
                  .getColumnType());
    }

    if (rowCondition == null) {
      return getJooqField(key).eq(cast(r.get(keyName, columnType), getJooqField(key)));
    } else {
      return rowCondition.and(
          getJooqField(key).eq(cast(r.get(keyName, columnType), getJooqField(key))));
    }
  }

  @Override
  public org.molgenis.emx2.Query query() {
    return new SqlQuery(this.getMetadata(), db.getJooq());
  }

  @Override
  public List<Row> retrieve() {
    return this.query().retrieve();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> klazz) {
    return query().retrieve(columnName, klazz);
  }

  @Override
  public String getName() {
    return getMetadata().getTableName();
  }

  private List<Field> getPrimaryKeyFields() {
    ArrayList<Field> keyFields = new ArrayList<>();
    for (String key : getMetadata().getPrimaryKey()) {
      keyFields.add(getJooqField(getMetadata().getColumn(key)));
    }
    return keyFields;
  }

  private org.jooq.Table getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
  }

  private Field getJooqField(Column c) {
    return field(name(c.getColumnName()), SqlTypeUtils.jooqTypeOf(c));
  }
}
