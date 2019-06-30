package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Select;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.Column.Type.MREF;
import static org.molgenis.Column.Type.REF;
import static org.molgenis.Row.MOLGENISID;

public class SqlQuery extends QueryBean {

  private String name;
  private Database db;
  private DSLContext sql;

  public SqlQuery(String name, SqlDatabase db, DSLContext sql) {
    this.name = name;
    this.db = db;
    this.sql = sql;
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {

    try {
      List<Row> result = new ArrayList<>();
      Table table = db.getSchema().getTable(name);

      // create the select
      SelectSelectStep select = sql.select(getFields(name));

      // create the from
      SelectJoinStep from = select.from(name(name));

      // create the joins
      from = createJoins(name, table, from);

      // create the where
      from.where(createConditions(name));

      // create the sort

      // retrieve
      System.out.println(from.getSQL());
      Result<Record> fetch = from.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }

      // create the from & joins

      return result;
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed:" + e2.getCause().getMessage(), e2);
      else throw new MolgenisException(e2);
    }
  }

  private List<Field> getFields(String name) {
    List<Field> fields = new ArrayList<>();
    for (Select select : this.getSelectList()) {
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

  private Condition createConditions(String name) {
    Condition c = null;
    for (Where w : this.getWhereLists()) {
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

  private SelectJoinStep createJoins(String name, Table table, SelectJoinStep from)
      throws MolgenisException {
    List<String> duplicatePaths = new ArrayList<>();
    for (Select s : this.getSelectList()) {
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

  private Column getColumn(Table t, String[] path) throws MolgenisException {
    Column c = t.getColumn(path[0]);
    if (c == null) throw new MolgenisException("Column '" + path[0] + "' unknown");
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(c.getRefTable(), Arrays.copyOfRange(path, 1, path.length));
    }
  }
}
