package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;
import org.molgenis.beans.RowBean;

import javax.sql.DataSource;
import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.unquotedName;
import static org.molgenis.Column.Type.MREF;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class SqlDatabase implements Database {
  private DSLContext sql;
  private SqlSchema schema;

  public SqlDatabase(DataSource source) throws MolgenisException {
    DSLContext context = DSL.using(source, SQLDialect.POSTGRES_10);
    this.sql = context;
    this.schema = new SqlSchema(sql);
  }

  public Query query(String table) {
    return new SqlQuery(table, this, sql);
  }

  public Schema getSchema() throws MolgenisException {
    return schema;
  }

  @Override
  public void insert(String table, Collection<org.molgenis.Row> rows) throws MolgenisException {
    try {
      Table t = getSchema().getTable(table);
      // get metadata
      List<Field> fields = new ArrayList<>();
      List<String> fieldNames = new ArrayList<>();
      int i = 0;
      for (Column c : t.getColumns()) {
        if (!MREF.equals(c.getType())) {
          fieldNames.add(c.getName());
          fields.add(getJooqField(c));
        }
        i++;
      }
      InsertValuesStepN step =
          sql.insertInto(table(name(t.getName())), fields.toArray(new Field[fields.size()]));
      for (org.molgenis.Row row : rows) {
        step.values(row.values(fieldNames.toArray(new String[fieldNames.size()])));
      }
      step.execute();
      // save the mrefs
      for (Column c : t.getColumns()) {
        if (MREF.equals(c.getType())) {
          saveUpdatedMrefs(rows, c);
        }
      }
    } catch (DataAccessException e) {
      throw new MolgenisException(e);
    }
  }

  @Override
  public void insert(String table, org.molgenis.Row row) throws MolgenisException {
    this.insert(table, Arrays.asList(row));
  }

  @Override
  public int update(String table, Collection<org.molgenis.Row> rows) throws MolgenisException {
    Table t = getSchema().getTable(table);

    // keep batchsize smaller to limit memory footprint
    int batchSize = 1000;

    // get metadata
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<String> fieldNames = new ArrayList<>();
    int i = 0;
    for (Column c : t.getColumns()) {
      if (!MREF.equals(c.getType())) {
        fieldNames.add(c.getName());
        fields.add(getJooqField(c));
        i++;
      }
    }
    // TODO update mref values

    // execute in batches
    int count = 0;
    List<org.molgenis.Row> batch = new ArrayList<>();
    for (org.molgenis.Row row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        updateBatch(batch, table(name(t.getName())), fields, fieldNames);
        batch.clear();
      }
    }
    updateBatch(batch, table(name(t.getName())), fields, fieldNames);
    return count;
  }

  private void updateBatch(
      Collection<org.molgenis.Row> rows,
      org.jooq.Table t,
      List<Field> fields,
      List<String> fieldNames) {
    if (!rows.isEmpty()) {
      // create multi-value insert
      InsertValuesStepN step = sql.insertInto(t, fields.toArray(new Field[fields.size()]));
      for (org.molgenis.Row row : rows) {
        step.values(row.values(fieldNames.toArray(new String[fieldNames.size()])));
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
  public void update(String table, org.molgenis.Row row) throws MolgenisException {
    this.update(table, Arrays.asList(row));
  }

  @Override
  public int delete(String table, Collection<org.molgenis.Row> rows) throws MolgenisException {
    Table t = getSchema().getTable(table);

    // because of expensive table scanning and smaller queryOld string size this batch should be
    // larger
    // than insert/update
    int batchSize = 100000;
    int count = 0;
    List<org.molgenis.Row> batch = new ArrayList<>();
    for (org.molgenis.Row row : rows) {
      batch.add(row);
      count++;
      if (count % batchSize == 0) {
        deleteBatch(t, batch);
        batch.clear();
      }
    }
    deleteBatch(t, batch);
    return count;
  }

  private void deleteBatch(Table table, Collection<org.molgenis.Row> rows)
      throws MolgenisException {
    if (!rows.isEmpty()) {
      // remove the mrefs first
      for (Column c : table.getColumns()) {
        if (MREF.equals(c.getType())) {
          this.deleteOldMrefs(rows, c);
        }
      }
      Field field = field(name(MOLGENISID), SQLDataType.UUID);
      List<UUID> idList = new ArrayList<>();
      rows.forEach(row -> idList.add(row.getMolgenisid()));
      sql.deleteFrom(table(name(table.getName()))).where(field.in(idList)).execute();
    }
  }

  @Override
  public void delete(String table, org.molgenis.Row row) throws MolgenisException {
    this.delete(table, Arrays.asList(row));
  }

  private void deleteOldMrefs(Collection<org.molgenis.Row> rows, Column column)
      throws MolgenisException {
    String joinTable = column.getMrefTable();
    List<UUID> oldMrefIds = new ArrayList<>();
    for (org.molgenis.Row r : rows) {
      oldMrefIds.add(r.getMolgenisid());
    }
    List<org.molgenis.Row> oldMrefs =
        query(joinTable)
            .where(column.getMrefBack())
            .eq(oldMrefIds.toArray(new UUID[oldMrefIds.size()]))
            .retrieve();
    delete(joinTable, oldMrefs);
  }

  private void saveUpdatedMrefs(Collection<org.molgenis.Row> rows, Column column)
      throws MolgenisException {
    String joinTable = column.getMrefTable();
    String colName = column.getName();
    String otherColname = column.getMrefBack();

    List<org.molgenis.Row> newMrefs = new ArrayList<>();
    for (org.molgenis.Row r : rows) {
      for (UUID uuid : r.getMref(colName)) {
        org.molgenis.Row join =
            new RowBean().setRef(colName, uuid).setRef(otherColname, r.getMolgenisid());
        newMrefs.add(join);
      }
    }
    update(joinTable, newMrefs);
  }

  private Field getJooqField(Column c) {
    return field(name(c.getName()), SqlTypeUtils.typeOf(c.getType()));
  }
}
