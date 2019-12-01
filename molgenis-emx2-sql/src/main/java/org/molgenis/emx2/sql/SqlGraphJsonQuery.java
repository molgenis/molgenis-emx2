package org.molgenis.emx2.sql;

import org.jooq.*;
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

/**
 * Todo:
 * <li>search - done
 * <li>where - done, only equal
 * <li>limit, offset - done
 * <li>sort
 * <li>mref
 */
public class SqlGraphJsonQuery extends Filter {
  private SqlTableMetadata table;
  private List<Object> select;
  private String[] searchTerms = new String[0];
  private Logger logger = LoggerFactory.getLogger(SqlGraphJsonQuery.class);

  public SqlGraphJsonQuery(SqlTableMetadata table) {
    super(table.getTableName());
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

  public void search(String... terms) {
    this.searchTerms = terms;
  }

  public String retrieve() {
    Long start = System.currentTimeMillis();

    String result = "{}";

    if (select != null) {
      for (int i = 0; i < select.size(); i++) {
        String field = checkIsString(select.get(i));
        switch (field) {
          case "items":
            result = executeRootQuery(table, getList(null, select.get(++i)), this, searchTerms);
            break;
          case "count":
            // do this later
            break;
          case "meta":
            getList(null, select.get(++i));
            // do this later
            break;
          default:
            throw new MolgenisException("", "", "Unknown field '" + field + "'");
        }
      }
    }

    logger.info("Query completed in " + (System.currentTimeMillis() - start) + "ms");

    return result;
  }

  private static String executeRootQuery(
      SqlTableMetadata table, List<Object> select, Filter filter, String[] searchTerms) {

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
    Condition searchCondition = createSearchConditions(table, fromAlias, select, searchTerms);
    if (searchCondition != null) where.add(searchCondition);

    SelectConditionStep step = from.where(where);

    SelectConditionStep jsonQuery =
        table
            .getJooq()
            .select(
                field("json_strip_nulls(json_agg(item))").as("items"),
                field("count(item.*)").as("count"))
            .from(table(step).as("item"))
            .where(getPathConditions(filter, table));

    return table
        .getJooq()
        .select(field("row_to_json(item)"))
        .from(table(jsonQuery).as("item"))
        .fetchOne()
        .get(0, String.class);
  }

  private static void createLeftJoins(
      SelectJoinStep step, TableMetadata table, String leftAlias, List<Object> select) {
    for (int i = 0; i < select.size(); i++) {
      // must skip 'items' and 'count'
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
          List nested = getItemsSelect(select.get(++i));
          step.leftJoin(table(name(schemaName, column.getRefTableName())).as(name(rightAlias)))
              .on(
                  "{0} = ANY ({1})",
                  field(name(rightAlias, column.getRefColumnName())),
                  field(name(leftAlias, column.getColumnName())));
          createLeftJoins(
              step,
              table.getSchema().getTableMetadata(column.getRefTableName()),
              rightAlias,
              nested);
          break;
      }
    }
  }

  /** navigates select and returns the list given for 'items, i.e. "items,List.of()" */
  private static List getItemsSelect(Object o) {
    List nested = getList(null, o);
    for (int j = 0; j < nested.size(); j++) {
      String field = checkIsString(nested.get(j));
      if ("items".equals(field)) {
        return getList(null, nested.get(++j));
      }
    }
    return new ArrayList();
  }

  private static Condition createSearchConditions(
      SqlTableMetadata table, String tableAlias, List<Object> select, String[] searchTerms) {
    Condition searchCondition = null;

    if (searchTerms != null && searchTerms.length > 0) {
      for (String term : searchTerms) {
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
            case REF_ARRAY:
              searchCondition =
                  searchCondition.or(
                      createSearchConditions(
                          (SqlTableMetadata)
                              table.getSchema().getTableMetadata(column.getRefTableName()),
                          tableAlias + "/" + column.getColumnName(),
                          getList(column, getItemsSelect(select.get(++i))),
                          searchTerms));
              break;
            case REF:
              searchCondition =
                  searchCondition.or(
                      createSearchConditions(
                          (SqlTableMetadata)
                              table.getSchema().getTableMetadata(column.getRefTableName()),
                          tableAlias + "/" + column.getColumnName(),
                          getList(column, select.get(++i)),
                          searchTerms));
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

  private static List<Field> getFields(
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
                    .eq(field(name(parentAlias, column.getColumnName()))),
                0,
                0))
        .as(column.getColumnName());
  }

  private static Field createRefArrayColumnSubselect(
      Column column, String parentAlias, List userSelection, Filter userFilter) {
    DSLContext dsl = ((SqlTableMetadata) column.getTable()).getJooq();
    List<Field> fields = new ArrayList<>();
    if (userSelection != null) {
      Condition condition =
          condition(
              "{0} = ANY ({1})",
              field(name(column.getRefColumnName())),
              field(name(parentAlias, column.getColumnName())));
      String aggregatefunction = "json_strip_nulls(json_agg(item))";
      String fromAlias = parentAlias + "/" + column.getColumnName();
      int limit = userFilter != null ? userFilter.getLimit() : 0;
      int offset = userFilter != null ? userFilter.getOffset() : 0;
      for (int i = 0; i < userSelection.size(); i++) {
        String field = checkIsString(userSelection.get(i));
        switch (field) {
          case "items":
            fields.add(
                field(
                        createSubselect(
                            getRefTableMetadata(column),
                            fromAlias,
                            getList(column, userSelection.get(++i)),
                            userFilter,
                            aggregatefunction,
                            condition,
                            limit,
                            offset))
                    .as("items"));
            break;
          case "count":
            fields.add(
                field(
                        field(
                            createSubselect(
                                getRefTableMetadata(column),
                                fromAlias,
                                null,
                                userFilter,
                                "count(*)",
                                condition,
                                0,
                                0)))
                    .as("count"));
            break;
          case "meta":
            // skip selection, you get it all
            userSelection.get(++i);
            fields.add(inline("{\"name\":\"todo\"}").as("meta"));
          default:
            throw new MolgenisException(
                "",
                "",
                "query failed: field '" + field + "' unknown within " + column.getColumnName());
        }
      }
    }

    return field(dsl.select(field("row_to_json(conn)")).from(table(dsl.select(fields)).as("conn")))
        .as(column.getColumnName());
  }

  private static SelectConditionStep createSubselect(
      SqlTableMetadata fromTable,
      String fromAlias,
      List userSelection,
      Filter userFilters,
      String aggregationFunction,
      Condition optionalParentFilter,
      int limit,
      int offset) {

    if (userSelection == null || userSelection.size() == 0) {
      userSelection = List.of(fromTable.getPrimaryKey());
    }

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

    SelectConditionStep query =
        fromTable
            .getJooq()
            .select(field(aggregationFunction))
            .from(table(from.where(where)).as("item"))
            .where(getPathConditions(userFilters, fromTable));

    if (limit > 0) query.limit(limit);
    if (offset > 0) query.offset(offset);

    return query;
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
            throw new SqlGraphQueryException(
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
    if (o == null) return new ArrayList();
    if (!(o instanceof List))
      throw new SqlGraphQueryException(
          "select error: expected list to follow REF column "
              + (column != null ? column.getColumnName() : "root")
              + " but found "
              + o);
    return (List) o;
  }

  private static Column getColumn(TableMetadata table, Object o) {
    String colName = checkIsString(o);
    Column column = table.getColumn(colName);
    if (column == null)
      throw new SqlGraphQueryException(
          "Selection error: Column " + colName + " not found in table " + table.getTableName());
    return column;
  }

  private static String checkIsString(Object o) {
    if (!(o instanceof String))
      throw new SqlGraphQueryException(
          "Query only accept string or list type. E.g. 'name','tag',List.of('name'). Found: " + o);
    return (String) o;
  }

  private static class SqlGraphQueryException extends MolgenisException {

    public SqlGraphQueryException(String detail) {
      super("QUERY_ERROR", "query error", detail);
    }
  }
}
