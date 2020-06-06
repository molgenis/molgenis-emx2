package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jooq.impl.DSL.*;
// import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.sql.SqlTypeUtils.getRefArrayColumnType;
import static org.molgenis.emx2.sql.SqlTypeUtils.getRefColumnType;

class SqlTable implements Table {

  private SqlDatabase db;
  private TableMetadata metadata;
  private static Logger logger = LoggerFactory.getLogger(SqlTable.class);

  SqlTable(SqlDatabase db, TableMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public org.molgenis.emx2.Schema getSchema() {
    return new SqlSchema(db, (SqlSchemaMetadata) metadata.getSchema());
  }

  @Override
  public TableMetadata getMetadata() {
    return metadata;
  }

  public int insert(Iterable<Row> rows) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {

            // first update superclass
            if (getMetadata().getInherit() != null) {
              getSchema().getTable(getMetadata().getInherit()).insert(rows);
            }

            // get metadata
            List<String> fieldNames = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            List<Field> fields = new ArrayList<>();
            for (Column c : getMetadata().getLocalColumns()) {
              fieldNames.add(c.getName());
              columns.add(c);
              fields.add(getJooqField(c));
            }
            InsertValuesStepN step =
                db.getJooq().insertInto(getJooqTable(), fields.toArray(new Field[fields.size()]));
            for (Row row : rows) {
              step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
            }
            count.set(step.execute());
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException("Insert into table '" + getName() + "' failed.", e);
    }

    log(start, count, "inserted");

    return count.get();
  }

  private void log(long start, AtomicInteger count, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} rows into table {} in {}ms",
          user,
          message,
          count.get(),
          getJooqTable(),
          (System.currentTimeMillis() - start));
    }
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
    long start = System.currentTimeMillis();

    if (getMetadata().getPrimaryKey() == null)
      throw new MolgenisException(
          "Update failed",
          "Table "
              + getName()
              + " cannot process row update requests because no primary key is defined");

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          db2 -> {
            // first update superclass
            if (getMetadata().getInherit() != null) {
              getSchema().getTable(getMetadata().getInherit()).update(rows);
            }

            // keep batchsize smaller to limit memory footprint
            int batchSize = 1000;

            // execute in batches (batch by size or because columns set change)
            TableMetadata tableMetadata = getMetadata();
            List<Row> batch = new ArrayList<>();
            List<String> fieldNames = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            List<Field> fields = new ArrayList<>();
            for (Row row : rows) {

              // get the fields metadata for this row as far as known in this table
              Collection<String> rowFields = new ArrayList<>();
              for (String name : row.getColumnNames()) {
                Column c = tableMetadata.getColumn(name);
                if (tableMetadata.getColumn(name) != null && c.getTableName().equals(getName())) {
                  rowFields.add(name);
                }
              }

              // execute the batch if batchSize is reached or fields differ from previous
              if (!batch.isEmpty()
                  && (count.get() % batchSize == 0
                      || (fieldNames.containsAll(rowFields)
                          && rowFields.containsAll(fieldNames)))) {
                updateBatch(
                    batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields());
                batch.clear();
                fieldNames.clear();
                fields.clear();
                columns.clear();
              }

              // add field metadata if first row of this batch
              if (fieldNames.isEmpty()) {
                for (String name : rowFields) {
                  Column c = tableMetadata.getColumn(name);
                  fieldNames.add(name);
                  columns.add(c);
                  fields.add(getJooqField(c));
                }
              }

              // else simply keep on adding rows to the batch
              batch.add(row);
              count.set(count.get() + 1);
            }

            // execute the remaining batch
            updateBatch(batch, getJooqTable(), fieldNames, columns, fields, getPrimaryKeyFields());
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException("Update into table '" + getName() + "' failed.", e);
    }

    log(start, count, "updated");

    return count.get();
  }

  private void updateBatch(
      Collection<Row> rows,
      org.jooq.Table t,
      List<String> fieldNames,
      List<Column> columns,
      List<Field> fields,
      List<Field> keyField) {

    if (!rows.isEmpty()) {

      // createColumn multi-value insert
      InsertValuesStepN step = db.getJooq().insertInto(t, fields.toArray(new Field[fields.size()]));

      for (Row row : rows) {
        step.values(SqlTypeUtils.getValuesAsCollection(row, columns));
      }

      // on duplicate key update using same record via "excluded" keyword in postgres
      InsertOnDuplicateSetStep step2 = step.onConflict(keyField).doUpdate();
      for (String name : fieldNames) {
        step2 =
            step2.set(field(name(name)), (Object) field(unquotedName("excluded.\"" + name + "\"")));
      }
      step.execute();
    }
  }

  @Override
  public int delete(Iterable<Row> rows) {
    long start = System.currentTimeMillis();

    AtomicInteger count = new AtomicInteger(0);
    try {
      db.tx(
          config -> {
            // first update superclass
            if (getMetadata().getInherit() != null) {
              getSchema().getTable(getMetadata().getInherit()).delete(rows);
            }

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
      throw new SqlMolgenisException("Delete into table " + getName() + " failed.   ", e);
    }

    log(start, count, "deleted");

    return count.get();
  }

  @Override
  public Query select(String... path) {
    return query().select(path);
  }

  @Override
  public Query select(SelectColumn... columns) {
    return query().select(columns);
  }

  @Override
  public Query filter(String path, Operator operator, Serializable... values) {
    return query().filter(path, operator, values);
  }

  public Query search(String terms) {
    return query().search(terms);
  }

  @Override
  public int delete(Row... rows) {
    return delete(Arrays.asList(rows));
  }

  private void deleteBatch(Collection<Row> rows) {
    if (!rows.isEmpty()) {
      String[] keyNames = getMetadata().getPrimaryKey();

      // in case no primary key is defined, use all columns
      if (getMetadata().getPrimaryKey() == null) {
        List<Column> allColumns = getMetadata().getColumns();
        keyNames = new String[allColumns.size()];
        for (int i = 0; i < keyNames.length; i++) {
          keyNames[i] = allColumns.get(i).getName();
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
      columnType = getRefColumnType(key);
    } else if (REF_ARRAY.equals(columnType)) { // || MREF.equals(columnType)) {
      columnType = getRefArrayColumnType(key);
    }

    if (rowCondition == null) {
      return getJooqField(key).eq(cast(r.get(keyName, columnType), getJooqField(key)));
    } else {
      return rowCondition.and(
          getJooqField(key).eq(cast(r.get(keyName, columnType), getJooqField(key))));
    }
  }

  @Override
  public Query query() {
    return new SqlQuery((SqlTableMetadata) this.getMetadata());
  }

  @Override
  public List<Row> getRows() {
    return this.query().getRows();
  }

  //  @Override
  //  public <E> List<E> retrieve(String columnName, Class<E> klazz) {
  //    return query().retrieve(columnName, klazz);
  //  }

  @Override
  public String getName() {
    return getMetadata().getTableName();
  }

  private List<Field> getPrimaryKeyFields() {
    List<Field> result = new ArrayList<>();
    for (String name : getMetadata().getPrimaryKey()) {
      result.add(getJooqField(getMetadata().getColumn(name)));
    }
    return result;
  }

  protected org.jooq.Table getJooqTable() {
    return table(name(metadata.getSchema().getName(), metadata.getTableName()));
  }

  public static Field getJooqField(Column c) {
    return field(name(c.getName()), SqlTypeUtils.jooqTypeOf(c));
  }
}
