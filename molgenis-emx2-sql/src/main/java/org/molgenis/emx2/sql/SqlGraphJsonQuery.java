package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;
import static org.molgenis.emx2.sql.Filter.f;

/**
 * Todo:
 * <li>search - done
 * <li>where - done, only equal
 * <li>first
 * <li>after
 * <li>sort
 * <li>mref
 */
public class SqlGraphJsonQuery {
  private SqlTableMetadata table;
  private List<Object> select;
  private Filter filter;
  private String[] searchTerms = new String[0];
  private Logger logger = LoggerFactory.getLogger(SqlGraphJsonQuery.class);

  public SqlGraphJsonQuery(SqlTableMetadata table) {
    this.table = table;
  }

  public SqlGraphJsonQuery(Table table) {
    this((SqlTableMetadata) table.getMetadata());
  }

  /**
   * you can create nested json selections following foreign relations, e.g.
   * ("name","status","category",List.of("name")) where category is a relationship to a table having
   * "name". Result will be like [{"name": ?, "status": ?, "category":{"name":?} }]
   */
  public SqlGraphJsonQuery select(List select) {
    this.select = select;
    return this;
  }

  public SqlGraphJsonQuery filter(Filter... filters) {
    if (filters.length > 0) {
      this.filter = f(null, filters);
    } else {
      this.filter = null;
    }
    return this;
  }

  public void search(String... terms) {
    this.searchTerms = terms;
  }

  public String retrieve() {
    Long start = System.currentTimeMillis();

    // select
    String fromAlias = table.getTableName();
    SelectIntoStep selectStep =
        table
            .getJooq()
            .select(getFields(fromAlias, select, filter, table))
            .distinctOn(field(name(fromAlias, table.getPrimaryKey()[0])));

    // from
    SelectJoinStep from =
        selectStep.from(
            table(name(table.getSchema().getName(), table.getTableName())).as(fromAlias));

    // if search we join all selected linked in tables
    if (searchTerms != null) createLeftJoins(from, table, table.getTableName(), select);

    // where
    List<Condition> where = new ArrayList<>();
    Condition valueFilter = getValueConditions(filter, table, fromAlias);
    if (valueFilter != null) where.add(valueFilter);
    Condition searchTerms = createSearchConditions(table, fromAlias, select);
    if (searchTerms != null) where.add(searchTerms);

    SelectConditionStep step = from.where(where);

    // System.out.println(step.getSQL(ParamType.NAMED_OR_INLINED));

    String result =
        table
            .getJooq()
            .select(field("json_strip_nulls(json_agg(item))"))
            .from(table(step).as("item"))
            .where(getPathConditions(filter, table))
            .fetchOne()
            .get(0, String.class);

    logger.info("Query completed in " + (System.currentTimeMillis() - start) + "ms");

    return result;
  }

  private void createLeftJoins(
      SelectJoinStep step, TableMetadata table, String leftAlias, List<Object> select) {
    for (int i = 0; i < select.size(); i++) {
      Column column = getColumn(table, select.get(i));
      String schemaName = table.getSchema().getName();
      String rightAlias = leftAlias + "/" + column.getColumnName();
      switch (column.getColumnType()) {
        case REF:
          step.leftJoin(table(name(schemaName, column.getRefTableName())).as(rightAlias))
              .on(
                  field(name(leftAlias, column.getColumnName()))
                      .eq(field(name(rightAlias, column.getRefColumnName()))));
          createLeftJoins(
              step,
              table.getSchema().getTableMetadata(column.getRefTableName()),
              rightAlias,
              getList(column, select.get(++i)));
          break;
        case REF_ARRAY:
          step.leftJoin(table(name(schemaName, column.getRefTableName())).as(name(rightAlias)))
              .on(
                  "{0} = ANY ({1})",
                  field(name(rightAlias, column.getRefColumnName())),
                  field(name(leftAlias, column.getColumnName())));
          createLeftJoins(
              step,
              table.getSchema().getTableMetadata(column.getRefTableName()),
              rightAlias,
              getList(column, select.get(++i)));
          break;
        default:
          break;
      }
    }
  }

  private Condition createSearchConditions(
      SqlTableMetadata table, String tableAlias, List<Object> select) {
    Condition searchCondition = null;
    if (this.searchTerms != null && this.searchTerms.length > 0) {
      for (String term : this.searchTerms) {
        Condition c =
            condition(
                name(tableAlias, MG_SEARCH_INDEX_COLUMN_NAME) + " @@ to_tsquery('" + term + ":*')");
        if (searchCondition == null) {
          searchCondition = c;
        } else {
          searchCondition = searchCondition.and(c);
        }
        // get from subpaths
        for (int i = 0; i < select.size(); i++) {
          Column column = getColumn(table, select.get(i));
          switch (column.getColumnType()) {
            case REF:
            case REF_ARRAY:
              searchCondition =
                  searchCondition.or(
                      createSearchConditions(
                          (SqlTableMetadata)
                              table.getSchema().getTableMetadata(column.getRefTableName()),
                          tableAlias + "/" + column.getColumnName(),
                          getList(column, select.get(++i))));
              break;
            default:
              break;
          }
        }
      }
    }
    return searchCondition;
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

  private List<Field> getFields(
      String tableAlias, List<Object> select, Filter filter, SqlTableMetadata table) {
    List<Field> fields = new ArrayList<>();
    List<String> fieldNames = new ArrayList<>();
    if (select != null) {
      for (int i = 0; i < select.size(); i++) {
        Column column = getColumn(table, select.get(i));
        fieldNames.add(column.getColumnName());
        switch (column.getColumnType()) {
          case REF:
            fields.add(
                createRefColumnSubselect(
                    column,
                    tableAlias,
                    getList(column, select.get(++i)),
                    getColumnFilter(filter, column)));
            break;
          case REF_ARRAY:
            fields.add(
                createRefArrayColumnSubselect(
                    column,
                    tableAlias,
                    getList(column, select.get(++i)),
                    getColumnFilter(filter, column)));
            break;
          default:
            fields.add(
                field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column)));
        }
      }
    }
    // also add fields for purpose of filters
    if (filter != null) {
      for (Filter f : filter.getFilters()) {
        Column column = getColumn(table, f.getField());
        if (!fieldNames.contains(column.getColumnName())) {
          switch (column.getColumnType()) {
            case REF:
              fields.add(
                  createRefColumnSubselect(
                      column, tableAlias, null, getColumnFilter(filter, column)));
              break;
            case REF_ARRAY:
              fields.add(
                  createRefArrayColumnSubselect(
                      column, tableAlias, null, getColumnFilter(filter, column)));
              break;
            default:
              fields.add(
                  field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column)));
          }
        }
      }
    }
    return fields;
  }

  private static Filter getColumnFilter(Filter filter, Column column) {
    if (filter != null) return filter.getFilter(column.getColumnName());
    return null;
  }

  private Field createRefColumnSubselect(
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

  private Field createRefArrayColumnSubselect(
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

  private SelectConditionStep createSubselect(
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
    Collection<Condition> where = new ArrayList<>();
    where.add(getValueConditions(userFilters, fromTable, fromAlias));
    where.add(optionalParentFilter);

    return fromTable
        .getJooq()
        .select(field(aggregationFunction))
        .from(table(from.where(where)).as("item"))
        .where(getPathConditions(userFilters, fromTable));
  }

  private static Condition getValueConditions(
      Filter filter, TableMetadata table, String tableAlias) {
    Condition condition = null;
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

          if (condition == null) condition = field(name(tableAlias, f.getField())).in(values);
          else condition.and(field(name(tableAlias, f.getField())).in(values));
        } else {
          // path filter must be done in the aggregation query
        }
      }
    }
    return condition;
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

  private static Column getColumn(TableMetadata table, Object o) {
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
