package org.molgenis.sql;

import org.jooq.*;
import org.molgenis.*;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Select;
import org.molgenis.Table;
import org.molgenis.beans.QueryBean;
import org.molgenis.utils.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Type.MREF;
import static org.molgenis.Type.REF;
import static org.molgenis.Operator.OR;
import static org.molgenis.Operator.SEARCH;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.sql.SqlTable.MG_SEARCH_VECTOR;

public class SqlQuery extends QueryBean implements Query {

  private Table from;
  private DSLContext sql;

  public SqlQuery(Table from, DSLContext sql) {
    this.from = from;
    this.sql = sql;
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {

    try {
      StopWatch.print("start SqlQuery.retrieve");

      List<Row> result = new ArrayList<>();

      // createColumn the select

      SelectSelectStep selectStep;
      List<Field> fields = getFields(from);
      StopWatch.print("getFields complete");

      if (fields.size() > 0) selectStep = sql.select(fields);
      else selectStep = sql.select();
      StopWatch.print("selectStep complete");

      // createColumn the from
      SelectJoinStep fromStep =
          selectStep.from(
              table(name(from.getSchema().getName(), from.getName())).as(from.getName()));

      StopWatch.print("fromStep complete");

      // createColumn the joins
      fromStep = createJoins(from.getName(), from, fromStep);
      StopWatch.print("createJoins complete");

      // createColumn the where
      fromStep.where(createConditions(from.getName()));

      StopWatch.print("createWhere complete");

      // createColumn the sort

      // retrieve
      System.out.println(fromStep.getSQL());
      StopWatch.print("print query complete");

      StopWatch.print("begin execute retrieve");
      Result<Record> fetch = fromStep.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }
      StopWatch.print("execute retrieve complete");

      // createColumn the from & joins

      return result;
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed:" + e2.getCause().getMessage(), e2);
      else throw new MolgenisException(e2);
    }
  }

  private List<Field> getFields(Table from) {
    List<Field> fields = new ArrayList<>();
    List<Select> selectList = this.getSelectList();
    for (Select select : selectList) {
      String[] path = select.getPath();
      if (path.length == 1) {
        fields.add(field(name(from.getName(), path[0])).as(name(path[0])));
      } else {
        String[] tablePath = Arrays.copyOfRange(path, 0, path.length - 1);
        String[] fieldPath = Arrays.copyOfRange(path, 0, path.length);

        String tableAlias = from.getName() + "/" + String.join("/", tablePath);
        fields.add(
            field(name(tableAlias, path[path.length - 1])).as(name(String.join("/", fieldPath))));
      }
    }
    return fields;
  }

  private Condition createConditions(String name) throws MolgenisException {
    Condition conditions = null;
    boolean or = false;
    for (Where w : this.getWhereLists()) {
      Condition newCondition = null;
      if (SEARCH.equals(w.getOperator())) {
        String search = "";
        for (Object s : w.getValues()) search += s + ":* ";
        newCondition = condition(MG_SEARCH_VECTOR + " @@ to_tsquery('" + search + "' )");
      } else if (OR.equals(w.getOperator())) {
        or = true;
      } else {
        // in case of field operator
        String[] path = w.getPath();
        String tableAlias = name;

        if (path.length > 1) {
          tableAlias += "/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
        }
        Name selector = selector = name(tableAlias, path[path.length - 1]);
        switch (w.getOperator()) {
          case EQ:
            // type check
            Object[] values = w.getValues();
            for (int i = 0; i < values.length; i++)
              values[i] = SqlTypeUtils.getTypedValue(values[i], getColumn(from, path));
            newCondition = field(selector).in(values);
            break;
          case ANY:
            newCondition =
                condition(
                    "{0} && {1}",
                    SqlTypeUtils.getTypedValue(w.getValues(), getColumn(from, path)),
                    field(selector));
            break;
          case SEARCH:

          default:
            throw new MolgenisException("Where clause not supported: " + w.toString());
        }
      }
      if (newCondition != null) {
        if (conditions == null) conditions = newCondition;
        else if (or) {
          conditions = conditions.or(newCondition);
          or = false;
        } else {
          conditions = conditions.and(newCondition);
        }
      }
    }
    return conditions;
  }

  private SelectJoinStep createJoins(String name, Table table, SelectJoinStep fromStep)
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

        String rightTable = c.getRefTable();
        String rightAlias = name + "/" + String.join("/", rightPath);
        String rightColumn = MOLGENISID;

        if (!duplicatePaths.contains(rightAlias)) {
          duplicatePaths.add(rightAlias);

          if (REF.equals(c.getType())) {
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), rightTable)).as(name(rightAlias)))
                    .on(
                        field(name(rightAlias, rightColumn))
                            .eq(field(name(leftAlias, leftColumn))));
          }
          if (MREF.equals(c.getType())) {
            String mrefTable = "TODO"; // c.getMrefTableName();
            rightColumn = "TODO"; // c.getRefColumnBack();

            // to link table
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), mrefTable)).as(name(mrefTable)))
                    .on(field(name(mrefTable, rightColumn)).eq(field(name(leftAlias, MOLGENISID))));

            // to other end of the mref
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), rightTable)).as(name(rightAlias)))
                    .on(field(name(mrefTable, leftColumn)).eq(field(name(rightAlias, MOLGENISID))));
          }
        }
      }
    }
    return fromStep;
  }

  private Column getColumn(Table t, String[] path) throws MolgenisException {
    Column c = t.getColumn(path[0]);
    if (c == null) throw new MolgenisException("Column '" + path[0] + "' unknown");
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(
          t.getSchema().getTable(c.getRefTable()), Arrays.copyOfRange(path, 1, path.length));
    }
  }
}
