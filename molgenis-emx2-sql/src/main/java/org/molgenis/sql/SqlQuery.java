package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.Query;
import org.molgenis.Row;
import org.molgenis.Select;
import org.molgenis.query.QueryBean;
import org.molgenis.Column;
import org.molgenis.TableMetadata;
import org.molgenis.Where;
import org.molgenis.utils.MolgenisException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.molgenis.query.Operator.OR;
import static org.molgenis.query.Operator.SEARCH;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;
import static org.molgenis.sql.SqlTable.MG_SEARCH_INDEX_COLUMN_NAME;

public class SqlQuery extends QueryBean implements Query {

  private TableMetadata from;
  private DSLContext sql;

  public SqlQuery(TableMetadata from, DSLContext sql) {
    this.from = from;
    this.sql = sql;
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {

    try {
      List<Row> result = new ArrayList<>();

      // createColumn the select

      SelectSelectStep selectStep;
      List<Field> fields = getFields(from);

      if (fields.isEmpty()) selectStep = sql.select(fields);
      else selectStep = sql.select();

      // createColumn the from
      SelectJoinStep fromStep = selectStep.from(getJooqTable(from));

      // createColumn the joins
      fromStep = createJoins(from.getTableName(), from, fromStep);
      // createColumn the where
      fromStep.where(createConditions(from.getTableName()));

      // createColumn the sort

      // retrieve
      Result<Record> fetch = fromStep.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }
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

  private org.jooq.Table getJooqTable(TableMetadata table) throws MolgenisException {

    // create all columns
    List<Field> fields = new ArrayList<>();
    for (Column column : table.getColumns()) {
      if (!MREF.equals(column.getType())) {
        fields.add(
            field(
                name(column.getMrefJoinTableName(), column.getColumnName()),
                SqlTypeUtils.jooqTypeOf(column)));
      } else {
        fields.add(
            field(
                name(table.getTableName(), column.getColumnName()),
                SqlTypeUtils.jooqTypeOf(column)));
      }
    }

    // check if search term is given then add search field too
    boolean search = false;
    for (Where w : getWhereLists()) {
      if (w.getOperator().equals(SEARCH)) search = true;
    }
    if (search) fields.add(field(name(MG_SEARCH_INDEX_COLUMN_NAME)));

    org.jooq.Table jooqTable =
        DSL.select(fields)
            .from(name(table.getSchema().getName(), table.getTableName()))
            .asTable(table.getTableName());

    // for mrefs join
    for (Column column : table.getColumns()) {
      if (MREF.equals(column.getType())) {
        jooqTable =
            jooqTable
                .leftJoin(
                    DSL.select(
                            field("array_agg({0})", name(column.getRefColumnName()))
                                .as(column.getColumnName()),
                            field(name(column.getReverseRefColumn())))
                        .from(
                            table(name(table.getSchema().getName(), column.getMrefJoinTableName())))
                        .groupBy(field(name(column.getReverseRefColumn())))
                        .asTable(column.getMrefJoinTableName()))
                .on(
                    field(name(column.getMrefJoinTableName(), column.getReverseRefColumn()))
                        .eq((field(name(table.getTableName(), column.getReverseRefColumn())))));
      }
    }
    return jooqTable;
  }

  private List<Field> getFields(TableMetadata from) {
    List<Field> fields = new ArrayList<>();
    List<Select> selectList = this.getSelectList();

    if (selectList.isEmpty()) {
      for (Column c : from.getColumns()) {
        this.select(c.getColumnName());
      }
    }

    for (Select select : selectList) {
      String[] path = select.getPath();
      if (path.length == 1) {
        fields.add(field(name(from.getTableName(), path[0])).as(name(path[0])));
      } else {
        String[] tablePath = Arrays.copyOfRange(path, 0, path.length - 1);
        String[] fieldPath = Arrays.copyOfRange(path, 0, path.length);

        String tableAlias = from.getTableName() + "/" + String.join("/", tablePath);
        fields.add(
            field(name(tableAlias, path[path.length - 1])).as(name(String.join("/", fieldPath))));
      }
    }
    return fields;
  }

  private Condition createConditions(String tableName) throws MolgenisException {
    Condition conditions = null;
    boolean or = false;
    for (Where w : this.getWhereLists()) {
      Condition newCondition = null;
      if (SEARCH.equals(w.getOperator())) {
        newCondition = createSearchCondition(w);
      } else if (OR.equals(w.getOperator())) {
        or = true;
      } else {
        newCondition = createFilterCondition(w, tableName);
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

  private Condition createFilterCondition(Where w, String tableName) throws MolgenisException {
    // in case of field operator
    String[] path = w.getPath();
    StringBuilder tableAlias = new StringBuilder(tableName);

    if (path.length > 1) {
      tableAlias.append("/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1)));
    }
    Name selector = name(tableAlias.toString(), path[path.length - 1]);
    switch (w.getOperator()) {
      case EQ:
        // type check
        Object[] values = w.getValues();
        for (int i = 0; i < values.length; i++)
          values[i] = SqlTypeUtils.getTypedValue(values[i], getColumn(from, path));
        return field(selector).in(values);
      case ANY:
        return condition(
            "{0} && {1}",
            SqlTypeUtils.getTypedValue(w.getValues(), getColumn(from, path)), field(selector));
      default:
        throw new MolgenisException(
            "invalid_query",
            "Creation of filter condiation failed",
            "Where clause '" + w.toString() + "' is not supported");
    }
  }

  private Condition createSearchCondition(Where w) {
    StringBuilder search = new StringBuilder();
    for (Object s : w.getValues()) search.append(s + ":* ");
    return condition(
        name(from.getTableName(), MG_SEARCH_INDEX_COLUMN_NAME)
            + " @@ to_tsquery('"
            + search
            + "' )");
  }

  private SelectJoinStep createJoins(String name, TableMetadata table, SelectJoinStep fromStep)
      throws MolgenisException {
    List<String> duplicatePaths = new ArrayList<>();

    for (Select s : this.getSelectList()) {
      String[] path = s.getPath();

      // in case of xref
      if (path.length >= 2) {
        String[] rightPath = Arrays.copyOfRange(path, 0, path.length - 1);
        String[] leftPath = Arrays.copyOfRange(path, 0, path.length - 2);
        Column c = getColumn(table, rightPath);

        String leftColumn = c.getColumnName();
        String leftAlias = path.length > 2 ? name + "/" + String.join("/", leftPath) : name;

        String rightTable = c.getRefTableName();
        String rightAlias = name + "/" + String.join("/", rightPath);
        String rightColumn = c.getRefColumnName();

        if (duplicatePaths.contains(rightAlias)) break; // only once needed

        // else
        duplicatePaths.add(rightAlias);

        switch (c.getType()) {
          case REF:
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), rightTable)).as(name(rightAlias)))
                    .on(
                        field(name(rightAlias, rightColumn))
                            .eq(field(name(leftAlias, leftColumn))));
            break;
          case REF_ARRAY:
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), rightTable)).as(name(rightAlias)))
                    .on(
                        "{0} = ANY ({1})",
                        field(name(rightAlias, rightColumn)), field(name(leftAlias, leftColumn)));
            break;
          case MREF:
            String joinTable = c.getMrefJoinTableName();

            // to link table
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), joinTable)).as(name(joinTable)))
                    .on(field(name(joinTable, rightColumn)).eq(field(name(leftAlias, MOLGENISID))));

            // to other end of the mref
            fromStep =
                fromStep
                    .leftJoin(
                        table(name(from.getSchema().getName(), rightTable)).as(name(rightAlias)))
                    .on(field(name(joinTable, leftColumn)).eq(field(name(rightAlias, MOLGENISID))));
            break;
          default:
            break;
        }
      }
    }
    return fromStep;
  }

  private Column getColumn(TableMetadata t, String[] path) throws MolgenisException {
    Column c = t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "undefined_column",
          "Column not found",
          "Column '" + path[0] + "' cannot be found in table " + t.getTableName());
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(
          t.getSchema().getTableMetadata(c.getRefTableName()),
          Arrays.copyOfRange(path, 1, path.length));
    }
  }
}
