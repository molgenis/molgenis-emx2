package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Operator.IS;
import static org.molgenis.emx2.Operator.IS_NOT;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;
import static org.molgenis.emx2.utils.TypeUtils.*;

/**
 * Todo:
 * <li>search - done
 * <li>where - part, only equal
 * <li>limit, offset - done
 * <li>sort
 * <li>mref
 * <li>inheritance !
 */
public class SqlGraphQuery extends Filter {
  private static final String AGGREGATE_COUNT = "count(*)";
  private static final String AGGREGATE_ITEM = "json_agg(item)";
  private static final String ITEMS_FIELD = "items";
  private static final String COUNT_FIELD = "count";

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

    List<Field> fields = new ArrayList<>();

    // global filter conditions
    Condition condition = condition("1=1");

    // items
    if (this.select.has(ITEMS_FIELD)) {
      fields.add(
          field(
                  createSubselect(
                      table,
                      table.getTableName(),
                      select != null && select.has(ITEMS_FIELD) ? select.get(ITEMS_FIELD) : null,
                      this,
                      AGGREGATE_ITEM,
                      condition,
                      true))
              .as(ITEMS_FIELD));
    }
    // count
    if (select.has(COUNT_FIELD)) {
      Select step =
          createSubselect(
              table, table.getTableName(), null, this, AGGREGATE_COUNT, condition, false);
      fields.add(field(step).as(COUNT_FIELD));
    }
    // meta
    if (select.has("meta")) {
      // do this later
      // todo validation on other values
    }

    // from
    SelectJoinStep query =
        table
            .getJooq()
            .select(field("json_strip_nulls(row_to_json(item))"))
            .from(table(table.getJooq().select(fields)).as("item"));

    String result = query.fetchOne().get(0, String.class);

    if (logger.isInfoEnabled()) {
      logger.info(query.getSQL(ParamType.INLINED));
      logger.info(String.format("Query completed in %sms", System.currentTimeMillis() - start));
    }
    return result;
  }

  private static SelectJoinStep createJoins(
      SelectJoinStep step, TableMetadata table, String leftAlias, SelectColumn select) {
    for (Column column : table.getLocalColumns()) {
      if (select.has(column.getColumnName())) {
        String rightAlias = leftAlias + "/" + column.getColumnName();
        ColumnType type = column.getColumnType();
        if (REF_ARRAY.equals(type)) {
          step =
              step.leftJoin(getJooqTable(getRefTable(column), rightAlias))
                  .on(
                      "{0} = ANY ({1})",
                      field(name(rightAlias, column.getRefColumnName())),
                      field(name(leftAlias, column.getColumnName())));
          createJoins(step, getRefTable(column), rightAlias, getColumSelect(select, column));
        } else if (REF.equals(type)) {
          step =
              step.leftJoin(getJooqTable(getRefTable(column), rightAlias))
                  .on(
                      field(name(leftAlias, column.getColumnName()))
                          .eq(field(name(rightAlias, column.getRefColumnName()))));
          createJoins(step, getRefTable(column), rightAlias, getColumSelect(select, column));
        }
      }
    }
    return step;
  }

  private static Condition createSearchConditions(
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
      if (select.has(column.getColumnName())) {
        String nextAlias = tableAlias + "/" + column.getColumnName();
        ColumnType type = column.getColumnType();
        if (REF_ARRAY.equals(type)) {
          SelectColumn subSelect = getColumSelect(select, column);
          if (subSelect != null && subSelect.has(ITEMS_FIELD)) {
            searchCondition =
                searchCondition.or(
                    createSearchConditions(
                        getRefTable(column), nextAlias, subSelect.get(ITEMS_FIELD), searchTerms));
          }
        }
        if (REF.equals(type)) {
          searchCondition =
              searchCondition.or(
                  createSearchConditions(
                      getRefTable(column), nextAlias, getColumSelect(select, column), searchTerms));
        }
      }
    }

    return searchCondition;
  }

  private static List<Field> getFields(
      SqlTableMetadata table, String tableAlias, SelectColumn select, Filter filter) {
    List<Field> fields = new ArrayList<>();
    for (Column column : table.getLocalColumns()) {
      if (isSelected(column, select, filter)) {
        switch (column.getColumnType()) {
          case REF:
            SelectColumn subselect = getColumSelect(select, column);
            if (!subselect.has(column.getRefColumnName())) {
              subselect.select(column.getRefColumnName());
            }
            fields.add(
                field(
                        createSubselect(
                            getRefTable(column),
                            tableAlias + "/" + column.getColumnName(),
                            subselect,
                            getColumnFilter(filter, column),
                            "row_to_json(item)",
                            field(name(column.getRefColumnName()))
                                .eq(field(name(tableAlias, column.getColumnName()))),
                            false))
                    .as(column.getColumnName()));
            break;
          case REF_ARRAY:
            fields.add(
                createRefArrayColumnSubselect(
                        column,
                        tableAlias,
                        getColumSelect(select, column),
                        getColumnFilter(filter, column))
                    .as(column.getColumnName()));
            break;
          default:
            fields.add(
                field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column))
                    .as(column.getColumnName()));
        }
      }
    }
    return fields;
  }

  private static Field createRefArrayColumnSubselect(
      Column column, String parentAlias, SelectColumn select, Filter filter) {

    DSLContext dsl = ((SqlTableMetadata) column.getTable()).getJooq();
    List<Field> fields = new ArrayList<>();
    Condition condition =
        condition(
            "{0} = ANY ({1})",
            field(name(column.getRefColumnName())),
            field(name(parentAlias, column.getColumnName())));
    String fromAlias = parentAlias + "/" + column.getColumnName();

    // subselection should at least contain the reffed column for joining
    SelectColumn subselect =
        (select != null && select.has(ITEMS_FIELD)
            ? select.get(ITEMS_FIELD)
            : new SelectColumn(column.getColumnName()));
    if (!subselect.has(column.getRefColumnName())) {
      subselect.select(column.getRefColumnName());
    }

    // create subselect
    if (filter != null || select.has(ITEMS_FIELD)) {
      // make sure the link field is there
      fields.add(
          field(
                  createSubselect(
                      getRefTable(column),
                      fromAlias,
                      subselect,
                      filter,
                      AGGREGATE_ITEM,
                      condition,
                      true))
              .as(ITEMS_FIELD));
    }

    // create subselect to count, always include for filtering
    fields.add(
        field(
                createSubselect(
                    getRefTable(column),
                    fromAlias,
                    null,
                    filter,
                    AGGREGATE_COUNT,
                    condition,
                    false))
            .as(COUNT_FIELD));

    return field(
            dsl.select(field("row_to_json(conn)"))
                .from(table(dsl.select(fields)).as("conn"))
                .where(field(name("conn", COUNT_FIELD)).gt(0)))
        .as(column.getColumnName());
  }

  private static SelectConditionStep createSubselect(
      SqlTableMetadata fromTable,
      String fromAlias,
      SelectColumn select,
      Filter filter,
      String aggregationFunction,
      Condition condition,
      boolean limitOffset) {

    // add search filters, only for 'root' query
    Condition searchCondition = null;
    if (filter instanceof SqlGraphQuery) {
      SqlGraphQuery root = (SqlGraphQuery) filter;
      if (root.searchTerms.length > 0) {
        Field pkey = field(name(fromAlias, fromTable.getPrimaryKey()[0]));
        // create subquery
        SelectJoinStep sub =
            fromTable.getJooq().select(pkey).from(getJooqTable(fromTable, fromAlias));
        sub = createJoins(sub, fromTable, fromAlias, select);
        searchCondition =
            pkey.in(
                sub.where(createSearchConditions(fromTable, fromAlias, select, root.searchTerms)));
      }
    }

    // join the ref table
    SelectConditionStep<Record> from =
        fromTable
            .getJooq()
            .select(getFields(fromTable, fromAlias, select, filter))
            .from(getJooqTable(fromTable, fromAlias))
            .where(searchCondition);

    // limit offset sortby
    if (limitOffset) {
      for (Map.Entry<String, Order> col : select.getOrderBy().entrySet()) {
        if (ASC.equals(col.getValue())) {
          from = (SelectConditionStep) from.orderBy(field(name(col.getKey())).asc());
        } else {
          from = (SelectConditionStep) from.orderBy(field(name(col.getKey())).desc());
        }
      }
      if (select.getLimit() > 0) from = (SelectConditionStep) from.limit(select.getLimit());
      if (select.getOffset() > 0) from = (SelectConditionStep) from.offset(select.getOffset());
    }

    // add user filters
    condition = createFiltersForColumns(condition, fromTable, filter);

    // create the full query
    SelectConditionStep query =
        fromTable
            .getJooq()
            .select(field(aggregationFunction))
            .from(table(from).as("item"))
            .where(condition);

    // return
    return query;
  }

  private static org.jooq.Table<Record> getJooqTable(TableMetadata table, String alias) {
    return table(name(table.getSchema().getName(), table.getTableName())).as(name(alias));
  }

  private static Condition createFiltersForColumns(
      Condition condition, TableMetadata table, Filter filter) {
    if (filter == null) return condition;
    for (Column column : table.getLocalColumns()) {
      Filter f = getColumnFilter(filter, column);
      if (f != null) {
        if (REF.equals(column.getColumnType()) || REF_ARRAY.equals(column.getColumnType())) {
          // check that subtree exists
          condition = condition.and(field(name(column.getColumnName())).isNotNull());
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

  private static Condition createFilterCondition(
      Column column, org.molgenis.emx2.Operator operator, Object[] values) {
    String name = column.getColumnName();
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
    if (IS.equals(operator)) {
      return field(name(columnName)).in(values);
    } else if (IS_NOT.equals(operator)) {
      return not(field(name(columnName)).in(values));
    } else {
      throw new SqlGraphQueryException(
          "Operator " + operator + " is not support for column " + columnName);
    }
  }

  private static Condition createTextFilter(
      String columnName, org.molgenis.emx2.Operator operator, String[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (String value : values) {
      switch (operator) {
        case IS:
          conditions.add(field(name(columnName)).eq(value));
          break;
        case IS_NOT:
          not = true;
          conditions.add(field(name(columnName)).eq(value));
          break;
        case DOES_NOT_CONTAIN:
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
              "Operator " + operator + " is not support for column " + columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition createOrdinalFilter(
      String columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    Field field = field(name(columnName));
    boolean not = false;
    for (int i = 0; i < values.length; i++) {
      switch (operator) {
        case IS:
        case IS_NOT:
          return createEqualsFilter(columnName, operator, values);
        case NOT_BETWEEN:
          not = true;
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(
                "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: "
                    + SqlTypeUtils.toString(values));
          conditions.add(field(name(columnName)).between(values[i], values[i + 1]));
          break;
        case BETWEEN:
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(
                "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: "
                    + SqlTypeUtils.toString(values));

          conditions.add(field(name(columnName)).between(values[i], values[i + 1]));
          break;
        default:
          throw new SqlGraphQueryException(
              "Operator " + operator + " is not support for column " + columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static SqlTableMetadata getRefTable(Column column) {
    return (SqlTableMetadata)
        column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  private static boolean isSelected(Column column, SelectColumn select, Filter filter) {
    return (select != null && select.has(column.getColumnName()))
        || filter != null && filter.has(column.getColumnName());
  }

  private static SelectColumn getColumSelect(SelectColumn select, Column column) {
    if (select != null) return select.get(column.getColumnName());
    return null;
  }

  private static Filter getColumnFilter(Filter filter, Column column) {
    if (filter != null) return filter.getFilter(column.getColumnName());
    return null;
  }

  private static class SqlGraphQueryException extends MolgenisException {
    public SqlGraphQueryException(String detail) {
      super("QUERY_ERROR", "query error", detail);
    }
  }

  public static SelectColumn s(String column) {
    return new SelectColumn(column);
  }

  public static SelectColumn s(String column, SelectColumn... sub) {
    return new SelectColumn(column, sub);
  }

  public static class SelectColumn {
    private String column;
    private int limit = 0;
    private int offset = 0;
    private Map<String, SelectColumn> children = new LinkedHashMap<>();
    private Map<String, Order> orderBy = new LinkedHashMap<>();

    public SelectColumn(String column) {
      this.column = column;
    }

    public SelectColumn(String column, String... subselects) {
      this(column);
      for (String subName : subselects) {
        children.put(subName, new SelectColumn(subName));
      }
    }

    public SelectColumn(String column, SelectColumn... subselects) {
      this(column);
      for (SelectColumn s : subselects) {
        this.children.put(s.getColumn(), s);
      }
    }

    String getColumn() {
      return column;
    }

    boolean has(String name) {
      return children.containsKey(name);
    }

    SelectColumn get(String name) {
      return children.get(name);
    }

    public Collection<String> getColumNames() {
      return children.keySet();
    }

    public void setLimit(int limit) {
      this.limit = limit;
    }

    public void setOffset(int offset) {
      this.offset = offset;
    }

    public int getLimit() {
      return limit;
    }

    public int getOffset() {
      return offset;
    }

    public void orderBy(String column, Order order) {
      this.orderBy.put(column, order);
    }

    public void setOrderBy(Map<String, Order> values) {
      this.orderBy.putAll(values);
    }

    public Map<String, Order> getOrderBy() {
      return orderBy;
    }

    public void select(String columnName) {
      this.children.put(columnName, s(columnName));
    }
  }
}
