package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.NOT_EQUALS;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;
import static org.molgenis.emx2.sql.SqlTypeUtils.jooqTypeOf;
import static org.molgenis.emx2.utils.TypeUtils.*;

/**
 * Todo:
 * <li>search - done
 * <li>where - done
 * <li>limit, offset - done
 * <li>sort - done
 * <li>sort search results
 * <li>mref
 * <li>inheritance !
 */
public class SqlGraphQuery extends Filter {
  public static final String COUNT_FIELD = "count";
  public static final String DATA_AGG_FIELD = "data_agg";
  public static final String DATA_FIELD = "data";
  public static final String MAX_FIELD = "max";
  public static final String MIN_FIELD = "min";
  public static final String AVG_FIELD = "avg";
  public static final String SUM_FIELD = "sum";

  private static final String ANY_SQL = "{0} = ANY ({1})";
  private static final String JSON_AGG_SQL = "json_strip_nulls(json_agg(item))";
  private static final String ROW_TO_JSON_SQL = "json_strip_nulls(row_to_json(item))";

  private static final String OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE =
      "Operator %s is not support for column '%s'";
  private static final String BETWEEN_ERROR_MESSAGE =
      "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: %s";
  private static final String ITEM = "item";

  private SqlTableMetadata table;
  private SelectColumn select;
  private String[] searchTerms = new String[0];
  private Logger logger = LoggerFactory.getLogger(SqlGraphQuery.class);

  private SqlGraphQuery(TableMetadata table) {
    super(table.getTableName());
    this.table = (SqlTableMetadata) table;
  }

  public SqlGraphQuery(Table table) {
    this(table.getMetadata());
  }

  public SelectColumn select(SelectColumn... select) {
    this.select = new SelectColumn(null, select);
    return this.select;
  }

  public void search(String... terms) {
    this.searchTerms = terms;
  }

  public String retrieve() {
    long start = System.currentTimeMillis();
    // global filter conditions
    Condition condition = condition("1=1");

    List<Field> fields = new ArrayList<>();

    if (this.select.has(DATA_FIELD)) {
      fields.add(
          field(
                  createSubselect(
                      table,
                      table.getTableName(),
                      select.get(DATA_FIELD),
                      this,
                      JSON_AGG_SQL,
                      condition,
                      true))
              .as(DATA_FIELD));
    }

    if (this.select.has(DATA_AGG_FIELD)) {
      fields.add(
          createAggregationField(
                  table, table.getTableName(), select.get(DATA_AGG_FIELD), this, condition)
              .as(DATA_AGG_FIELD));
    }

    SelectJoinStep query =
        table
            .getJooq()
            .select(field(ROW_TO_JSON_SQL))
            .from(table(table.getJooq().select(fields)).as(ITEM));
    String result = query.fetchOne().get(0, String.class);
    if (logger.isInfoEnabled()) {
      logger.info(query.getSQL(ParamType.INLINED));
      logger.info(String.format("Query completed in %sms", System.currentTimeMillis() - start));
    }
    return result;
  }

  private static SelectJoinStep createSubselect(
      SqlTableMetadata fromTable,
      String fromAlias,
      SelectColumn select,
      Filter filter,
      String aggregationFunction,
      Condition conditions,
      boolean includeLimitOfssetSort) {

    // fields
    SelectJoinStep from =
        fromTable
            .getJooq()
            .select(createSelectionFields(fromTable, fromAlias, select, filter))
            .from(getJooqTable(fromTable, fromAlias));

    // add user filter conditions to any parent filter
    conditions = createFiltersForColumns(conditions, fromTable, filter);

    // add to that search filters, only for 'root' query having the search fields
    if (filter instanceof SqlGraphQuery) {
      SqlGraphQuery root = (SqlGraphQuery) filter;
      if (root.searchTerms.length > 0) {
        Field pkey = field(name(fromAlias, fromTable.getPrimaryKey()[0]));
        // create subquery
        SelectJoinStep sub =
            fromTable.getJooq().select(pkey).from(getJooqTable(fromTable, fromAlias));
        sub = createJoins(sub, fromTable, fromAlias, select);
        conditions =
            conditions.and(
                pkey.in(
                    sub.where(
                        createFiltersForSearch(fromTable, fromAlias, select, root.searchTerms))));
      }
    }

    // add the filter conditions to the query
    SelectConditionStep<Record> where = from.where(conditions);

    // add limit and offset to that query if desired
    if (includeLimitOfssetSort) {
      where = createLimitOffsetOrderBy(select, where);
    }

    // create the aggregation of the subselect, e.g. into count or json
    return fromTable.getJooq().select(field(aggregationFunction)).from(table(where).as(ITEM));
  }

  private static List<Field> createSelectionFields(
      SqlTableMetadata table, String tableAlias, SelectColumn select, Filter filter) {
    List<Field> fields = new ArrayList<>();
    for (Column column : table.getLocalColumns()) {
      if (isSelectedOrFiltered(column, select, filter)) {
        if (REF.equals(column.getColumnType()) || REF_ARRAY.equals(column.getColumnType())) {
          fields.add(
              createSelectionFieldForRef(
                  column,
                  tableAlias,
                  select != null ? select.get(column.getName()) : null,
                  getFilterForRef(filter, column)));
        } else {
          fields.add(
              field(name(tableAlias, column.getName()), jooqTypeOf(column)).as(column.getName()));
        }
      }

      // check for aggregation
      String aggFieldName = column.getName() + "_agg";
      if (select != null && select.has(aggFieldName)) {
        fields.add(
            createAggregationField(
                    getRefTable(column),
                    tableAlias + "/Agg",
                    select.get(aggFieldName),
                    getFilterForRef(filter, column),
                    condition(
                        ANY_SQL,
                        field(name(column.getRefColumnName())),
                        field(name(tableAlias, column.getName()))))
                .as(aggFieldName));
      }
    }
    return fields;
  }

  private static Field createAggregationField(
      TableMetadata table,
      String tableAlias,
      SelectColumn select,
      Filter filter,
      Condition conditions) {
    DSLContext jooq = ((SqlTableMetadata) table).getJooq();

    org.jooq.Table<Record> jooqTable = getJooqTable(table, tableAlias);

    List<Field> fields = new ArrayList<>();

    // user filter
    conditions = createFiltersForColumns(conditions, table, filter);

    if (select.has(COUNT_FIELD)) {
      fields.add(count().as(COUNT_FIELD));
    }
    for (Column col : table.getColumns()) {
      if (select.has(col.getName())) {
        // todo: see if this is a huge performance hit or not to do all at same time
        if (select.has(MAX_FIELD)
            || select.has(MIN_FIELD)
            || select.has(AVG_FIELD)
            || select.has(SUM_FIELD)) {
          fields.add(
              field(
                      "json_build_object({0},{1},{2},{3},{4},{5},{6},{7})",
                      MAX_FIELD,
                      max(field(name(tableAlias, col.getName()))),
                      MIN_FIELD,
                      min(field(name(tableAlias, col.getName()))),
                      AVG_FIELD,
                      avg(field(name(tableAlias, col.getName()), SqlTypeUtils.jooqTypeOf(col))),
                      SUM_FIELD,
                      sum(field(name(tableAlias, col.getName()), SqlTypeUtils.jooqTypeOf(col))))
                  .as(col.getName()));
        }
      }
    }

    return field(
        jooq.select(field(ROW_TO_JSON_SQL))
            .from(table(jooq.select(fields).from(jooqTable).where(conditions)).as(ITEM)));
  }

  private static Field createSelectionFieldForRef(
      Column column, String tableAlias, SelectColumn select, Filter filter) {
    if (select == null) select = new SelectColumn(column.getName());
    if (!select.has(column.getRefColumnName())) {
      select.select(column.getRefColumnName());
    }
    if (REF.equals(column.getColumnType())) {
      return field(
              createSubselect(
                  getRefTable(column),
                  tableAlias + "/" + column.getName(),
                  select,
                  filter,
                  ROW_TO_JSON_SQL,
                  field(name(column.getRefColumnName()))
                      .eq(field(name(tableAlias, column.getName()))),
                  false))
          .as(column.getName());
    } else {
      return field(
              createSubselect(
                  getRefTable(column),
                  tableAlias + "/" + column.getName(),
                  select,
                  filter,
                  JSON_AGG_SQL,
                  condition(
                      ANY_SQL,
                      field(name(column.getRefColumnName())),
                      field(name(tableAlias, column.getName()))),
                  true))
          .as(column.getName());
    }
  }

  private static SelectJoinStep createJoins(
      SelectJoinStep step, TableMetadata table, String leftAlias, SelectColumn select) {
    for (Column column : table.getLocalColumns()) {
      if (select != null && select.has(column.getName())) {
        String rightAlias = leftAlias + "/" + column.getName();
        ColumnType type = column.getColumnType();
        if (REF_ARRAY.equals(type)) {
          step =
              step.leftJoin(getJooqTable(getRefTable(column), rightAlias))
                  .on(
                      ANY_SQL,
                      field(name(rightAlias, column.getRefColumnName())),
                      field(name(leftAlias, column.getName())));
          createJoins(step, getRefTable(column), rightAlias, select.get(column.getName()));
        } else if (REF.equals(type)) {
          step =
              step.leftJoin(getJooqTable(getRefTable(column), rightAlias))
                  .on(
                      field(name(leftAlias, column.getName()))
                          .eq(field(name(rightAlias, column.getRefColumnName()))));
          createJoins(step, getRefTable(column), rightAlias, select.get(column.getName()));
        }
      }
    }
    return step;
  }

  private static Condition createFiltersForColumns(
      Condition condition, TableMetadata table, Filter filter) {
    if (filter == null) return condition;
    for (Column column : table.getLocalColumns()) {
      Filter f = getFilterForRef(filter, column);
      if (f != null) {
        if (REF.equals(column.getColumnType()) || REF_ARRAY.equals(column.getColumnType())) {
          // check that subtree exists
          condition = condition.and(field(name(column.getName())).isNotNull());
        } else {
          // add the column filter(s)
          for (Map.Entry<org.molgenis.emx2.Operator, Object[]> entry :
              f.getConditions().entrySet()) {
            condition =
                condition.and(createFilterCondition(column, entry.getKey(), entry.getValue()));
          }
        }
      }
    }
    return condition;
  }

  private static Condition createFiltersForSearch(
      TableMetadata table, String tableAlias, SelectColumn select, String[] searchTerms) {

    // create local filters
    List<Condition> local = new ArrayList<>();
    for (String term : searchTerms) {
      local.add(
          condition(
              name(tableAlias, MG_SEARCH_INDEX_COLUMN_NAME) + " @@ to_tsquery('" + term + ":*')"));
    }
    Condition searchCondition = and(local);

    // get from subpaths
    for (Column column : table.getLocalColumns()) {
      if (select != null && select.has(column.getName())) {
        String nextAlias = tableAlias + "/" + column.getName();
        ColumnType type = column.getColumnType();
        if (REF_ARRAY.equals(type) || REF.equals(type)) {
          searchCondition =
              searchCondition.or(
                  createFiltersForSearch(
                      getRefTable(column), nextAlias, select.get(column.getName()), searchTerms));
        }
      }
    }
    return searchCondition;
  }

  private static Condition createFilterCondition(
      Column column, org.molgenis.emx2.Operator operator, Object[] values) {
    String name = column.getName();
    switch (column.getColumnType()) {
      case TEXT:
      case STRING:
        return createTextFilter(name, operator, toStringArray(values));
      case BOOL:
        return createEqualsFilter(name, operator, toBoolArray(values));
      case UUID:
        return createEqualsFilter(name, operator, toUuidArray(values));
      case INT:
        return createOrdinalFilter(name, operator, toIntArray(values));
      case DECIMAL:
        return createOrdinalFilter(name, operator, toDecimalArray(values));
      case DATE:
        return createOrdinalFilter(name, operator, toDateArrray(values));
      case DATETIME:
        return createOrdinalFilter(name, operator, toDateTimeArray(values));
      case REF:
      case REF_ARRAY:
      default:
        throw new SqlGraphQueryException(
            "Filter of '"
                + name
                + " failed: operator "
                + operator
                + " not supported for type "
                + column.getColumnType());
    }
  }

  private static Condition createEqualsFilter(
      String columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    if (EQUALS.equals(operator)) {
      return field(name(columnName)).in(values);
    } else if (NOT_EQUALS.equals(operator)) {
      return not(field(name(columnName)).in(values));
    } else {
      throw new SqlGraphQueryException(OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, columnName);
    }
  }

  private static Condition createTextFilter(
      String columnName, org.molgenis.emx2.Operator operator, String[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (String value : values) {
      switch (operator) {
        case EQUALS:
          conditions.add(field(name(columnName)).eq(value));
          break;
        case NOT_EQUALS:
          not = true;
          conditions.add(field(name(columnName)).eq(value));
          break;
        case NOT_CONTAINS:
          not = true;
          conditions.add(field(name(columnName)).likeIgnoreCase("%" + value + "%"));
          break;
        case CONTAINS:
          conditions.add(field(name(columnName)).likeIgnoreCase("%" + value + "%"));
          break;
        case TRIGRAM_MATCH:
          conditions.add(condition("word_similarity({0},{1}) > 0.6", value, name(columnName)));
          break;
        case LEXICAL_MATCH:
          conditions.add(
              condition(
                  "to_tsquery({0}) @@ to_tsvector({1})",
                  value.trim().replaceAll("\\s+", ":* & ") + ":*", name(columnName)));
          break;
        default:
          throw new SqlGraphQueryException(
              OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, operator, columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition createOrdinalFilter(
      String columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (int i = 0; i < values.length; i++) {
      switch (operator) {
        case EQUALS:
        case NOT_EQUALS:
          return createEqualsFilter(columnName, operator, values);
        case NOT_BETWEEN:
          not = true;
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
          conditions.add(field(name(columnName)).between(values[i], values[i + 1]));
          break;
        case BETWEEN:
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));

          conditions.add(field(name(columnName)).between(values[i], values[i + 1]));
          i++; // skip one NOSONAR
          break;
        default:
          throw new SqlGraphQueryException(
              OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, operator, columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static SelectConditionStep createLimitOffsetOrderBy(
      SelectColumn select, SelectConditionStep query) {
    if (select != null) {
      for (Map.Entry<String, Order> col : select.getOrderBy().entrySet()) {
        if (ASC.equals(col.getValue())) {
          query = (SelectConditionStep) query.orderBy(field(name(col.getKey())).asc());
        } else {
          query = (SelectConditionStep) query.orderBy(field(name(col.getKey())).desc());
        }
      }
      if (select.getLimit() > 0) query = (SelectConditionStep) query.limit(select.getLimit());
      if (select.getOffset() > 0) query = (SelectConditionStep) query.offset(select.getOffset());
    }
    return query;
  }

  // HELPER METHODS
  private static org.jooq.Table<Record> getJooqTable(TableMetadata table, String alias) {
    return table(name(table.getSchema().getName(), table.getTableName())).as(name(alias));
  }

  private static SqlTableMetadata getRefTable(Column column) {
    return (SqlTableMetadata)
        column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  private static boolean isSelectedOrFiltered(Column column, SelectColumn select, Filter filter) {
    return (select != null && select.has(column.getName()))
        || filter != null && filter.has(column.getName());
  }

  private static Filter getFilterForRef(Filter filter, Column column) {
    if (filter != null) return filter.getFilter(column.getName());
    return null;
  }
}
