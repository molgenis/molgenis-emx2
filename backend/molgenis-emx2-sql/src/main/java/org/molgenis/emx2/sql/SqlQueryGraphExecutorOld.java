package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.SqlQueryUtils.*;

/**
 * Todo:
 * <li>search - done
 * <li>where - done
 * <li>limit, offset - done
 * <li>sort - done
 * <li>sort search results
 * <li>mref
 * <li>inheritance
 */
public class SqlQueryGraphExecutorOld extends QueryBean {
  public static final String COUNT_FIELD = "count";
  public static final String DATA_AGG_FIELD = "data_agg";
  public static final String DATA_FIELD = "data";
  public static final String MAX_FIELD = "max";
  public static final String MIN_FIELD = "min";
  public static final String AVG_FIELD = "avg";
  public static final String SUM_FIELD = "sum";
  public static final String GROUPBY_FIELD = "groupby";

  private static final String JSON_AGG_SQL = "json_strip_nulls(json_agg(item))";
  private static final String ROW_TO_JSON_SQL = "json_strip_nulls(row_to_json(item))";

  private static final String ITEM = "item";

  private static Logger logger = LoggerFactory.getLogger(SqlQueryGraphExecutor.class);

  public static String getJson(
      SqlTableMetadata table, SelectColumn select, Filter filter, String[] searchTerms) {
    long start = System.currentTimeMillis();

    // validate
    validateConnectionSelectFields(select);

    List<Field> fields = new ArrayList<>();

    if (select.has(DATA_FIELD)) {
      fields.add(
          field(
                  createSubselect(
                      table,
                      table.getTableName(),
                      null,
                      select.getSubselect(DATA_FIELD),
                      filter,
                      JSON_AGG_SQL,
                      null,
                      true,
                      searchTerms))
              .as(DATA_FIELD));
    }

    if (select.has(DATA_AGG_FIELD)) {
      fields.add(
          createAggregationField(
                  table,
                  table.getTableName(),
                  null,
                  select.getSubselect(DATA_AGG_FIELD),
                  filter,
                  null,
                  searchTerms)
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

  private static void validateConnectionSelectFields(SelectColumn select) {
    for (String name : select.getColumNames()) {
      if (!DATA_FIELD.equals(name) && !DATA_AGG_FIELD.equals(name)) {
        throw new MolgenisException(QUERY_FAILED, "Field '" + name + "' unknown");
      }
    }
  }

  private static Select createSubselect(
      SqlTableMetadata table,
      String tableAlias,
      String parentAlias,
      SelectColumn select,
      Filter filter,
      String aggregationFunction,
      Condition conditions,
      boolean includeLimitOfssetSort,
      String[] searchTerms) {

    // fields

    SelectJoinStep from =
        table
            .getJooq()
            .select(createSelectionFields(table, tableAlias, select, filter))
            .from(getJooqTable(table).as(tableAlias));
    try {
      // inner join the inherited classes
      from = createInheritanceJoin(table, tableAlias, from);

      // add user filter conditions to any parent filter
      conditions = mergeConditions(conditions, createFiltersForColumns(table, tableAlias, filter));

      // add to that search filters, only for 'root' query having the search fields
      if (searchTerms != null) {
        conditions =
            mergeConditions(
                conditions, createSearchCondition(table, tableAlias, select, searchTerms));
      }

      // add the filter conditions to the query
      SelectConditionStep<Record> where = from.where(conditions);

      // add limit and offset to that query if desired
      if (includeLimitOfssetSort) {
        where = createLimitOffsetOrderBy(select, where);
      }

      if (aggregationFunction == null) {
        return where;
      } else {
        // create the aggregation of the subselect, e.g. into count or json
        SelectJoinStep subselect =
            table.getJooq().select(field(aggregationFunction)).from(table(where).as(ITEM));
        Condition condition = createConditionsForRefs(parentAlias, table, filter);
        if (condition != null) return subselect.where(condition);
        else return subselect;
      }
    } finally {
      from.close();
    }
  }

  private static List<Field> createSelectionFields(
      TableMetadata table, String tableAlias, SelectColumn select, Filter filter) {
    List<Field> fields = new ArrayList<>();

    for (Column column : table.getColumns()) {

      // if selected and ref
      if (isSelectedOrFiltered(column, select, filter)) {

        // inheritance
        String inheritAlias = getSubclassAlias(table, tableAlias, column);

        // if a relationship but NOT the inheritance relationship (pkey)
        if ((REF.equals(column.getColumnType())
            || REF_ARRAY.equals(column.getColumnType())
            || REFBACK.equals(column.getColumnType())
            || MREF.equals(column.getColumnType()))
        //            && (table.getInherit() == null
        //                || !(table.getPrimaryKeys().contains(column.getName()))
        //                    && column.getRefTable().equals(column.getTable().getInherit()))
        ) {
          fields.add(
              createSelectionFieldForRef(
                      column,
                      inheritAlias,
                      select != null ? select.getSubselect(column.getName()) : null,
                      getFilterForRef(filter, column))
                  .as(column.getName()));
        } else {
          fields.add(field(name(inheritAlias, column.getName()), column.getJooqType()));
        }
      }

      // check for aggregation
      String aggFieldName = column.getName() + "_agg";
      if (select != null && select.has(aggFieldName)) {
        String subAlias = tableAlias + "/" + aggFieldName;
        fields.add(
            createAggregationField(
                    (SqlTableMetadata) getRefTable(column),
                    subAlias,
                    tableAlias,
                    select.getSubselect(aggFieldName),
                    getFilterForRef(filter, column),
                    getSubFieldCondition(column, tableAlias, subAlias),
                    null)
                .as(aggFieldName));
      }
    }
    return fields;
  }

  private static Field createAggregationField(
      SqlTableMetadata table,
      String tableAlias,
      String parentAlias,
      SelectColumn select,
      Filter filter,
      Condition conditions,
      String[] searchTerms) {
    DSLContext jooq = table.getJooq();

    // add user filter conditions to any parent filter
    conditions = mergeConditions(conditions, createFiltersForColumns(table, tableAlias, filter));

    // add to that search filters, only for 'root' query having the search fields
    conditions =
        mergeConditions(conditions, createSearchCondition(table, tableAlias, select, searchTerms));

    List<Field> fields = new ArrayList<>();
    List<Field> groupBy = new ArrayList<>();
    SelectColumn subSelect = new SelectColumn("item"); // will contain the subquery
    subSelect.select(table.getPrimaryKeys());

    if (select.has(COUNT_FIELD)) {
      fields.add(count().as(COUNT_FIELD));
    }
    if (select.has(GROUPBY_FIELD)) {
      // todo
    }

    for (Column col : table.getColumns()) {
      if (select.has(col.getName())) {
        SelectColumn aggField = select.getSubselect(col.getName());
        if (aggField.has(MAX_FIELD)
            || aggField.has(MIN_FIELD)
            || aggField.has(AVG_FIELD)
            || aggField.has(SUM_FIELD)) {
          // add to subselect as input for agg functions
          subSelect.select(col.getName());
          // add the agg functions
          fields.add(
              field(
                      "json_build_object({0},{1},{2},{3},{4},{5},{6},{7})",
                      MAX_FIELD,
                      max(field(name(col.getName()))),
                      MIN_FIELD,
                      min(field(name(col.getName()))),
                      AVG_FIELD,
                      avg(field(name(col.getName()), col.getJooqType())),
                      SUM_FIELD,
                      sum(field(name(col.getName()), col.getJooqType())))
                  .as(col.getName()));
        } else {
          groupBy.add(field(name(tableAlias, col.getName())));
        }
      }
    }

    Select source =
        createSubselect(
            table,
            tableAlias,
            parentAlias,
            subSelect,
            filter,
            null,
            conditions,
            false,
            searchTerms);
    Select aggregateQuery = null;
    if (!groupBy.isEmpty()) {
      //      aggregateQuery =
      //          jooq.select(fields)
      //              .from(table(source).as("aggs"))
      //              .where(createConditionsForRefs(condition("1=1"), table, filter))
      //              .groupBy(groupBy);
    } else {
      aggregateQuery = jooq.select(fields).from(table(source).as("aggs"));
      Condition subfilter = createConditionsForRefs(parentAlias, table, filter);
      if (subfilter != null) aggregateQuery = ((SelectJoinStep) aggregateQuery).where(subfilter);
    }
    return field(
        jooq.select(field("json_strip_nulls(row_to_json(agg_item))"))
            .from(table(aggregateQuery).as("agg_item")));
  }

  private static Field createSelectionFieldForRef(
      Column column, String tableAlias, SelectColumn select, Filter filter) {
    if (select == null) select = new SelectColumn(column.getName());

    // minimally select the key
    for (String key : column.getRefTable().getPrimaryKeys()) {
      if (!select.has(key)) {
        select.select(key);
      }
    }
    String subAlias = tableAlias + "/" + column.getName();
    return field(
        createSubselect(
            (SqlTableMetadata) getRefTable(column),
            subAlias,
            tableAlias,
            select,
            filter,
            REF.equals(column.getColumnType()) ? ROW_TO_JSON_SQL : JSON_AGG_SQL,
            getSubFieldCondition(column, tableAlias, subAlias),
            !REF.equals(column.getColumnType()),
            null));
  }

  /**
   * calculate the condition to link subfield to a parent field using ref, ref_array or refback
   * relations
   */
  private static Condition getSubFieldCondition(Column column, String tableAlias, String subAlias) {
    List<Condition> conditions = new ArrayList<>();
    if (REF.equals(column.getColumnType())) {
      for (Reference ref : column.getRefColumns()) {
        conditions.add(
            field(name(subAlias, ref.getTo())).eq(field(name(tableAlias, ref.getName()))));
      }
    } else if (REF_ARRAY.equals(column.getColumnType())) {
      String refs =
          column.getRefColumns().stream()
              .map(ref -> name(tableAlias, ref.getName()).toString())
              .collect(Collectors.joining(","));
      String to =
          column.getRefColumns().stream()
              .map(ref -> name(subAlias, ref.getTo()).toString())
              .collect(Collectors.joining(","));
      String as =
          column.getRefColumns().stream()
              .map(ref -> name(ref.getName()).toString())
              .collect(Collectors.joining(","));
      conditions.add(
          condition(
              "({0}) IN (SELECT * FROM UNNEST({1}) AS t({2}))",
              keyword(to), keyword(refs), keyword(as)));
      // keep this, may be faster for single column ref_array?
      //            condition(
      //                ANY_SQL,
      //                field(name(subAlias, ref.getTo())),
      //                field(name(tableAlias, ref.getName()))));

    } else if (REFBACK.equals(column.getColumnType())) {
      Column mappedBy = column.getMappedByColumn();
      if (REF.equals(mappedBy.getColumnType())) {
        for (Reference ref : mappedBy.getRefColumns()) {
          conditions.add(
              field(name(subAlias, ref.getName())).eq(field(name(tableAlias, ref.getTo()))));
        }
      } else if (REF_ARRAY.equals(mappedBy.getColumnType())) {
        for (Reference ref : mappedBy.getRefColumns()) {
          conditions.add(
              condition(
                  ANY_SQL,
                  field(name(tableAlias, ref.getTo())),
                  field(name(subAlias, ref.getName()))));
        }
      }
    } else if (MREF.equals(column.getColumnType())) {
      String joinTable = column.getTableName() + "-" + column.getName();
      List<Condition> where = new ArrayList<>();
      // MTM table should match on the remote key
      for (Reference ref : column.getRefColumns()) {
        where.add(field(name(subAlias, ref.getTo())).eq(field(name(joinTable, ref.getName()))));
      }
      // MTM table should match on primary key
      for (Column key : column.getTable().getPrimaryKeyColumns()) {
        where.add(field(name(tableAlias, key.getName())).eq(field(name(joinTable, key.getName()))));
      }
      conditions.add(exists(selectFrom(name(column.getSchemaName(), joinTable)).where(where)));
    } else {
      throw new SqlQueryGraphException(
          "Internal error",
          "For column " + column.getTable().getTableName() + "." + column.getName());
    }
    return and(conditions);
  }

  private static Condition createConditionsForRefs(
      String tableAlias, TableMetadata table, Filter filter) {
    Condition condition = null;
    if (filter == null) return condition;
    for (Column column : table.getLocalColumns()) {
      Filter f = getFilterForRef(filter, column);
      if (f != null) {
        ColumnType type = column.getColumnType();
        if (REF.equals(type)
            || REF_ARRAY.equals(type)
            || REFBACK.equals(type)
            || MREF.equals(type)) {
          // check that subtree exists
          condition =
              mergeConditions(condition, field(name(tableAlias, column.getName())).isNotNull());
        }
      }
    }
    return condition;
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
  private static org.jooq.Table<Record> getJooqTable(TableMetadata table) {
    return table(name(table.getSchema().getName(), table.getTableName()));
  }

  private static TableMetadata getRefTable(Column column) {
    return column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }
}
