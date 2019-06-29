package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Schema;
import org.molgenis.Select;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;
import org.molgenis.beans.RowBean;

import javax.sql.DataSource;
import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.unquotedName;
import static org.molgenis.Column.Type.MREF;
import static org.molgenis.Column.Type.REF;
import static org.molgenis.Operator.OR;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class SqlDatabase implements Database {
  private DSLContext sql;
  private SqlSchema schema;

  public SqlDatabase(DataSource source) throws MolgenisException {
    DSLContext context = DSL.using(source, SQLDialect.POSTGRES_10);
    this.sql = context;
    this.schema = new SqlSchema(sql);
    // todo, create a reload that reloads table and field metadata
  }

  public Query query(String table) {
    return new QueryBean(this, table);
  }

  public Schema getSchema() throws MolgenisException {
    return schema;
  }

  public void close() {
    sql.close();
  }

  private List<Field> getFields(String name, Query q) {
    List<Field> fields = new ArrayList<>();
    for (Select select : q.getSelectList()) {
      String[] path = select.getPath();
      if (path.length == 1) {
        fields.add(field(name(name, path[0])).as(name(path[0])));
      } else {
        String[] tablePath = Arrays.copyOfRange(path, 0, path.length - 1);
        String[] fieldPath = Arrays.copyOfRange(path, 0, path.length);

        String tableAlias = name + "/" + String.join("/", tablePath);
        fields.add(
            field(name(tableAlias, path[path.length - 1])).as(name(String.join("/", fieldPath))));
      }
    }
    return fields;
  }

  private Column getColumn(Table t, String[] path) throws MolgenisException {
    Column c = t.getColumn(path[0]);
    if (c == null) throw new MolgenisException("Column '" + path[0] + "' unknown");
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(c.getRefTable(), Arrays.copyOfRange(path, 1, path.length));
    }
  }

  @Override
  public List<Row> retrieve(String name, Query q) throws MolgenisException {
    try {
      List<Row> result = new ArrayList<>();
      Table table = this.getSchema().getTable(name);

      // create the select
      SelectSelectStep select = sql.select(getFields(name, q));

      // create the from
      SelectJoinStep from = select.from(name(name));

      // create the joins
      from = createJoins(name, q, table, from);

      // create the where
      from.where(createConditions(name, q));

      // create the sort

      // retrieve
      System.out.println(from.getSQL());
      Result<Record> fetch = from.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }

      // create the from & joins

      return result;
    } catch (Exception e) {
      throw new MolgenisException("Query failed:" + e.getCause().getMessage(), e);
    }
  }

  private Condition createConditions(String name, Query q) {
    Condition c = null;
    for (Where w : q.getWhereLists()) {
      String[] path = w.getPath();
      String tableAlias = name;
      if (path.length > 1) {
        tableAlias += "/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
      }
      String fieldName = path[path.length - 1];
      if (c == null) c = field(name(tableAlias, fieldName)).in(w.getValues());
      else c = c.and(field(name(tableAlias, fieldName)).in(w.getValues()));
    }
    return c;
  }

  public SelectJoinStep createJoins(String name, Query q, Table table, SelectJoinStep from)
      throws MolgenisException {
    List<String> duplicatePaths = new ArrayList<>();
    for (Select s : q.getSelectList()) {
      String[] path = s.getPath();

      // in case of xref
      if (path.length >= 2) {
        String[] rightPath = Arrays.copyOfRange(path, 0, path.length - 1);
        String[] leftPath = Arrays.copyOfRange(path, 0, path.length - 2);
        Column c = getColumn(table, rightPath);

        String leftColumn = c.getName();
        String leftAlias;
        if (path.length > 2) leftAlias = name + "/" + String.join("/", leftPath);
        else leftAlias = name;

        String rightTable = c.getRefTable().getName();
        String rightAlias = name + "/" + String.join("/", rightPath);
        String rightColumn = MOLGENISID;

        if (!duplicatePaths.contains(rightAlias)) {
          duplicatePaths.add(rightAlias);

          if (REF.equals(c.getType())) {
            from =
                from.leftJoin(table(name(rightTable)).as(name(rightAlias)))
                    .on(
                        field(name(rightAlias, rightColumn))
                            .eq(field(name(leftAlias, leftColumn))));
          }
          if (MREF.equals(c.getType())) {
            String mrefTable = c.getMrefTable();
            rightColumn = c.getMrefBack();

            // to link table
            from =
                from.leftJoin(table(name(mrefTable)).as(name(mrefTable)))
                    .on(field(name(mrefTable, rightColumn)).eq(field(name(leftAlias, MOLGENISID))));

            // to other end of the mref
            from =
                from.leftJoin(table(name(rightTable)).as(name(rightAlias)))
                    .on(field(name(mrefTable, leftColumn)).eq(field(name(rightAlias, MOLGENISID))));
          }
        }
      }
    }
    return from;
  }

  public List<Field> getFields(List<Field> fields) {
    return fields;
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
