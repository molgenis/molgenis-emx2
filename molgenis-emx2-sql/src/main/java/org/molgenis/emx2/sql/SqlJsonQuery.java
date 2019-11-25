package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.Filter.f;

/**
 * Todo:
 * <li>search
 * <li>where
 * <li>first
 * <li>after
 * <li>sort
 * <li>mref
 */
public class SqlJsonQuery {
  private SqlTableMetadata table;
  private List<Object> select;
  private Filter filter;

  public SqlJsonQuery(SqlTableMetadata table) {
    this.table = table;
  }

  public SqlJsonQuery(Table table) {
    this((SqlTableMetadata) table.getMetadata());
  }

  /**
   * you can create nested json selections following foreign relations, e.g.
   * ("name","status","category",List.of("name")) where category is a relationship to a table having
   * "name". Result will be like [{"name": ?, "status": ?, "category":{"name":?} }]
   */
  public SqlJsonQuery select(List select) {
    this.select = select;
    return this;
  }

  public SqlJsonQuery filter(Filter... filters) {
    if (filters.length > 0) {
      this.filter = f(null, filters);
    } else {
      this.filter = null;
    }
    return this;
  }

  public String retrieve() {

    SelectConditionStep step =
        createSubselect(
            table, table.getTableName(), select, filter, "json_strip_nulls(json_agg(item))", null);

    System.out.println(step.getSQL(ParamType.NAMED_OR_INLINED));

    return step.fetchOne().get(0, String.class);
  }

  private static Collection<Condition> getPathConditions(Filter filter, SqlTableMetadata table) {
    List<Condition> pathConditions = new ArrayList<>();
    if (filter != null) {
      for (Filter f : filter.getFilters()) {
        Column c = table.getColumn(f.getField());
        switch (c.getColumnType()) {
          case REF:
          case REF_ARRAY:
            pathConditions.add(field(name("item", c.getColumnName())).isNotNull());
          default:
            // todo other path such as mref
            break;
        }
      }
    }
    return pathConditions;
  }

  private static List<Field> getFields(
      String parent, List<Object> select, Filter filter, SqlTableMetadata table) {
    List<Field> fields = new ArrayList<>();
    for (int i = 0; i < select.size(); i++) {
      Column column = getColumn(table, select.get(i));
      switch (column.getColumnType()) {
        case REF:
          fields.add(
              createRefColumnSubselect(
                  column,
                  parent,
                  getList(column, select.get(++i)),
                  getColumnFilter(filter, column)));
          break;
        case REF_ARRAY:
          fields.add(
              createRefArrayColumnSubselect(
                  column,
                  parent,
                  getList(column, select.get(++i)),
                  getColumnFilter(filter, column)));
          break;
        default:
          fields.add(field(name(column.getColumnName()), SqlTypeUtils.jooqTypeOf(column)));
      }
    }
    return fields;
  }

  private static Filter getColumnFilter(Filter filter, Column column) {
    if (filter != null) return filter.getFilter(column.getColumnName());
    return null;
  }

  private static Field createRefColumnSubselect(
      Column column, String parentAlias, List userSelection, Filter userFilter) {
    return field(
            createSubselect(
                getRefTableMetadata(column),
                parentAlias + "/" + column.getColumnName(),
                userSelection,
                userFilter,
                "json_strip_nulls(row_to_json(item))",
                field(name(column.getRefColumnName()))
                    .eq(field(name(parentAlias, column.getColumnName())))))
        .as(column.getColumnName());
  }

  private static Field createRefArrayColumnSubselect(
      Column column, String parentAlias, List userSelection, Filter userFilter) {
    return field(
            createSubselect(
                getRefTableMetadata(column),
                parentAlias + "/" + column.getColumnName(),
                userSelection,
                userFilter,
                "json_strip_nulls(json_agg(item))",
                condition(
                    "{0} = ANY ({1})",
                    field(name(column.getRefColumnName())),
                    field(name(parentAlias, column.getColumnName())))))
        .as(column.getColumnName());
  }

  private static SelectConditionStep createSubselect(
      SqlTableMetadata fromTable,
      String fromAlias,
      List userSelection,
      Filter userFilters,
      String aggregationFunction,
      Condition optionalParentFilter) {

    // select
    SelectSelectStep selectStep =
        fromTable.getJooq().select(getFields(fromAlias, userSelection, userFilters, fromTable));
    // from
    SelectJoinStep from =
        selectStep.from(
            table(name(fromTable.getSchema().getName(), fromTable.getTableName())).as(fromAlias));
    // where
    List<Condition> where = new ArrayList<>();
    if (optionalParentFilter != null) where.add(optionalParentFilter);
    where.addAll(getValueConditions(userFilters, fromTable));

    return fromTable
        .getJooq()
        .select(field(aggregationFunction))
        .from(table(from.where(where)).as("item"))
        .where(getPathConditions(userFilters, fromTable));
  }

  private static Collection<Condition> getValueConditions(Filter filter, TableMetadata table) {
    List<Condition> conditions = new ArrayList<>();
    if (filter != null) {
      for (Filter f : filter.getFilters()) {
        Column c = table.getColumn(f.getField());
        // todo validation?
        if (f.getOperator() != null) {
          // todo improve
          if (c.getColumnType().equals(ColumnType.REF)
              || c.getColumnType().equals(ColumnType.REF_ARRAY)) {
            throw new RuntimeException(
                "cannot set filter condition on ref/refarray field '" + f.getField() + "'");
          }

          // pffff
          Object[] values = f.getValues();
          for (int i = 0; i < values.length; i++)
            values[i] = SqlTypeUtils.getTypedValue(values[i], c);

          conditions.add(field(name(f.getField())).in(values));
        } else {
          // path filter must be done in the aggregation query
        }
      }
    }
    return conditions;
  }

  private static SqlTableMetadata getRefTableMetadata(Column column) {
    return (SqlTableMetadata)
        column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  private static List getList(Column column, Object o) {
    if (!(o instanceof List))
      throw new MolgenisException(
          "",
          "",
          "select error: expected list to follow REF column "
              + column.getColumnName()
              + " but found "
              + o);
    return (List) o;
  }

  private static Column getColumn(SqlTableMetadata table, Object o) {
    String colName = checkIsString(o);
    Column column = table.getColumn(colName);
    if (column == null)
      throw new MolgenisException(
          "",
          "",
          "Selection error: Column " + colName + " not found in table " + table.getTableName());
    return column;
  }

  private static String checkIsString(Object o) {
    if (!(o instanceof String))
      throw new MolgenisException(
          "query error",
          "query error",
          "Query only accept string or list type. E.g. 'name','tag',List.of('name')");
    return (String) o;
  }

  private void validate(Object... objects) {
    for (Object o : objects) {
      if (o instanceof List) validate(((List) o).toArray());
      else checkIsString(o);
    }
  }
}
