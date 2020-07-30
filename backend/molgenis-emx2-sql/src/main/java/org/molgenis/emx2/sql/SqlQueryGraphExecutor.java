package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.Table;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.Constants.MG_TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.sql.SqlQueryUtils.*;

public class SqlQueryGraphExecutor extends QueryBean {
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

    List<Field<Object>> fields = new ArrayList<>();
    DSLContext sql = table.getJooq();

    // root query
    CommonTableExpression<Record> with =
        name(table.getTableName()).as(rootQuery(table, filter, searchTerms));

    if (select.has(DATA_FIELD)) {
      // get the selected fields
      Collection<Field<?>> selectFields =
          getSelectFields(table, table.getTableName(), select.getSubselect(DATA_FIELD));
      // create query
      Table<Record> query =
          applyLimitOffsetOrderBy(
                  select.getSubselect(DATA_FIELD),
                  sql.select(selectFields).from(name(table.getTableName())))
              .asTable(ITEM);
      // limit/offset

      // aggregate into json
      fields.add(field(sql.select(field(JSON_AGG_SQL)).from(query)).as(DATA_FIELD));
    }
    if (select.has(DATA_AGG_FIELD)) {
      // get the selected fields
      Collection<Field<?>> aggFields =
          aggregateFields(table, table.getTableName(), select.getSubselect(DATA_AGG_FIELD));
      // create query
      Table<Record> query = sql.select(aggFields).from(name(table.getTableName())).asTable(ITEM);
      // aggregate into json
      fields.add(field(sql.select(field(ROW_TO_JSON_SQL)).from(query)).as(DATA_AGG_FIELD));
    }

    // asemble final query
    SelectJoinStep<Record1<Object>> query =
        sql.with(with).select(field(ROW_TO_JSON_SQL)).from(table(sql.select(fields)).as(ITEM));

    logger.info(query.getSQL(true));

    return query.fetchOne().get(0, String.class);
  }

  private static SelectConnectByStep rootQuery(
      SqlTableMetadata table, Filter filter, String[] searchTerms) {
    String tableAlias = table.getTableName();

    SelectJoinStep<Record1<Object>> subquery =
        table.getJooq().select(field("{0}.*", name(tableAlias))).from(from(table).as(tableAlias));

    subquery = filterJoins(table, table.getTableName(), subquery, filter);

    // where
    Condition searchCondition = filterSearchConditions(tableAlias, searchTerms);
    Condition filterCondition = filterConditions(table, table.getTableName(), filter);
    Condition condition = mergeConditions(searchCondition, filterCondition);

    return condition != null ? subquery.where(condition) : subquery;

    //    return table
    //        .getJooq()
    //        .select()
    //        .from(from(table))
    //        .where(
    //            condition(
    //                "({0}) in ({1})",
    //                keyword(pkeyColumns), condition != null ? subquery.where(condition) :
    // subquery));
  }

  private static Collection<Field<?>> getSelectFields(
      TableMetadata table, String tableAlias, SelectColumn selection) {
    List<Field<?>> fields = new ArrayList<>();

    // include primary key
    for (Field pkey : table.getPrimaryKeyFields()) {
      if (!selection.has(pkey.getName())) {
        fields.add(field(name(tableAlias, pkey.getName())));
      }
    }

    for (SelectColumn select : selection.getSubselect()) {
      Column column = table.getColumn(select.getColumn());

      // validate that column exists
      if (column == null) {
        // aggregation column?
        column = table.getColumn(select.getColumn().replace("_agg", ""));
        if (column == null) {
          throw new MolgenisException(
              "Select failed",
              "Column '" + select.getColumn() + "' unknown in table " + table.getTableName());
        }
      }

      // add the fields, using subselects for references
      if (column.isReference() && select.getColumn().endsWith("_agg")) {
        // aggregation
        fields.add(aggregate((SqlTableMetadata) column.getRefTable(), column, tableAlias, select));
      } else if (column.isReference()) {
        // ref subselect
        fields.add(subselect((SqlTableMetadata) column.getRefTable(), column, tableAlias, select));
      } else {
        // primitive fields
        fields.add(field(name(tableAlias, column.getName())));
      }
    }
    return fields;
  }

  private static Table<Record> from(TableMetadata table) {
    Table<Record> result = table.getJooqTable();
    TableMetadata inheritedTable = table.getInheritedTable();
    while (inheritedTable != null) {
      result =
          result
              .leftJoin(inheritedTable.getJooqTable())
              .using(inheritedTable.getPrimaryKeyFields());
      inheritedTable = inheritedTable.getInheritedTable();
    }
    return result;
  }

  private static Condition where(Column column, String tableAlias, String subAlias) {
    List<Condition> foreignKeyMatch = new ArrayList<>();

    if (REF.equals(column.getColumnType())) {
      foreignKeyMatch.addAll(
          column.getRefColumns().stream()
              .map(
                  ref ->
                      field(name(subAlias, ref.getTo())).eq(field(name(tableAlias, ref.getName()))))
              .collect(Collectors.toList()));
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
      foreignKeyMatch.add(
          condition(
              "({0}) IN (SELECT * FROM UNNEST({1}) AS t({2}))",
              keyword(to), keyword(refs), keyword(as)));
    } else if (REFBACK.equals(column.getColumnType())) {
      Column mappedBy = column.getMappedByColumn();
      if (REF.equals(mappedBy.getColumnType())) {
        foreignKeyMatch.addAll(
            mappedBy.getRefColumns().stream()
                .map(
                    ref ->
                        field(name(subAlias, ref.getName()))
                            .eq(field(name(tableAlias, ref.getTo()))))
                .collect(Collectors.toList()));
      } else if (REF_ARRAY.equals(mappedBy.getColumnType())) {
        foreignKeyMatch.addAll(
            mappedBy.getRefColumns().stream()
                .map(
                    ref ->
                        condition(
                            ANY_SQL,
                            field(name(tableAlias, ref.getTo())),
                            field(name(subAlias, ref.getName()))))
                .collect(Collectors.toList()));
      }
    } else if (MREF.equals(column.getColumnType())) {
      String joinTable = column.getTableName() + "-" + column.getName();
      String joinTableAlias = "joinTable";
      List<Condition> where = new ArrayList<>();
      // MTM table should match on the remote key
      for (Reference ref : column.getRefColumns()) {
        where.add(
            field(name(subAlias, ref.getTo())).eq(field(name(joinTableAlias, ref.getName()))));
      }
      // MTM table should match on primary key
      for (Column key : column.getTable().getPrimaryKeyColumns()) {
        where.add(
            field(name(tableAlias, key.getName())).eq(field(name(joinTableAlias, key.getName()))));
      }
      foreignKeyMatch.add(
          exists(
              selectFrom(table(name(column.getSchemaName(), joinTable)).as(joinTableAlias))
                  .where(where)));
    } else {
      throw new SqlQueryGraphException(
          "Internal error",
          "For column " + column.getTable().getTableName() + "." + column.getName());
    }
    return and(foreignKeyMatch);
  }

  private static Field<?> subselect(
      SqlTableMetadata table, Column column, String tableAlias, SelectColumn select) {

    // in case the root subselect then column == null
    DSLContext jooq = table.getJooq();
    String subAlias = tableAlias + "-" + column.getName();

    SelectConditionStep<Record> from =
        jooq
            // select
            .select(getSelectFields(table, subAlias, select))
            // from
            .from(from(table).as(subAlias))
            // where
            .where(where(column, tableAlias, subAlias));

    from = (SelectConditionStep<Record>) applyLimitOffsetOrderBy(select, from);

    String agg = REF.equals(column.getColumnType()) ? ROW_TO_JSON_SQL : JSON_AGG_SQL;

    return field(jooq.select(field(agg)).from(from.asTable(ITEM))).as(select.getColumn());
  }

  private static Field aggregate(
      SqlTableMetadata table, Column column, String tableAlias, SelectColumn select) {
    String subAlias = tableAlias + "-" + column.getName() + "_agg";
    return field(
            table
                .getJooq()
                .select(field(ROW_TO_JSON_SQL))
                .from(
                    table(
                            table
                                .getJooq()
                                .select(aggregateFields(table, subAlias, select))
                                .from(from(table).as(subAlias))
                                // where
                                .where(where(column, tableAlias, subAlias)))
                        .as(ITEM)))
        .as(select.getColumn());
  }

  private static Collection<Field<?>> aggregateFields(
      SqlTableMetadata table, String tableAlias, SelectColumn select) {
    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn field : select.getSubselect()) {

      if (COUNT_FIELD.equals(field.getColumn())) {
        fields.add(count().as(COUNT_FIELD));
      } else {
        Column column = table.getColumn(field.getColumn());
        if (column == null) {
          throw new MolgenisException(
              "Aggregation failed",
              "Column " + field.getColumn() + " unknown in table " + table.getTableName());
        }
        if (field.has(MAX_FIELD)
            || field.has(MIN_FIELD)
            || field.has(AVG_FIELD)
            || field.has(SUM_FIELD)) {
          // add to subselect as input for agg functions
          // add the agg functions
          fields.add(
              field(
                      "json_build_object({0},{1},{2},{3},{4},{5},{6},{7})",
                      MAX_FIELD,
                      max(field(name(tableAlias, column.getName()))),
                      MIN_FIELD,
                      min(field(name(tableAlias, column.getName()))),
                      AVG_FIELD,
                      avg(field(name(tableAlias, column.getName()), column.getJooqType())),
                      SUM_FIELD,
                      sum(field(name(tableAlias, column.getName()), column.getJooqType())))
                  .as(field.getColumn()));
        }
      }
    }
    return fields;
  }

  private static SelectJoinStep<Record1<Object>> filterJoins(
      TableMetadata table,
      String tableAlias,
      SelectJoinStep<Record1<Object>> join,
      Filter filters) {

    for (Filter filter : filters.getSubfilters()) {
      Column column = table.getColumn(filter.getColumn());
      if (column == null) {
        throw new MolgenisException(
            "Filter failed",
            "Column " + filter.getColumn() + " unknown in table " + table.getTableName());
      }

      if (column.isReference()) {
        String subAlias = tableAlias + "-" + column.getName();
        join.leftJoin(from(column.getRefTable()).as(subAlias))
            .on(where(column, tableAlias, subAlias));
        // recurse
        join = filterJoins(column.getRefTable(), subAlias, join, filter);
      }
    }
    return join;
  }

  private static Condition filterConditions(
      TableMetadata table, String tableAlias, Filter filters) {
    List<Condition> conditions = new ArrayList<>();
    for (Filter filter : filters.getSubfilters()) {
      Column column = table.getColumn(filter.getColumn());

      if (column.isReference()) {
        conditions.add(
            filterConditions(column.getRefTable(), tableAlias + "-" + column.getName(), filter));
      } else {
        conditions.add(
            createFilterCondition(
                tableAlias,
                column.getName(),
                column.getColumnType(),
                filter.getOperator(),
                filter.getValues()));
      }
    }
    return conditions.isEmpty() ? null : and(conditions);
  }

  private static Condition filterSearchConditions(String tableAlias, String[] searchTerms) {
    List<Condition> searchConditions = new ArrayList<>();
    for (String term : searchTerms) {
      for (String subTerm : term.split(" ")) {
        subTerm = subTerm.trim();
        // short terms with 'like', longer with trigram
        if (subTerm.length() <= 3)
          searchConditions.add(
              field(name(tableAlias, MG_TEXT_SEARCH_COLUMN_NAME))
                  .likeIgnoreCase("%" + subTerm + "%"));
        else {
          searchConditions.add(
              condition(
                  "word_similarity({0},{1}) > 0.6",
                  subTerm, field(name(tableAlias, MG_TEXT_SEARCH_COLUMN_NAME))));
        }
      }
    }
    return searchConditions.isEmpty() ? null : or(searchConditions);
  }

  private static SelectConnectByStep<Record> applyLimitOffsetOrderBy(
      SelectColumn select, SelectConnectByStep<Record> query) {
    for (Map.Entry<String, Order> col : select.getOrderBy().entrySet()) {
      if (ASC.equals(col.getValue())) {
        query = (SelectConditionStep) query.orderBy(field(name(col.getKey())).asc());
      } else {
        query = (SelectConditionStep) query.orderBy(field(name(col.getKey())).desc());
      }
    }
    if (select.getLimit() > 0) query = (SelectConditionStep) query.limit(select.getLimit());
    if (select.getOffset() > 0) query = (SelectConditionStep) query.offset(select.getOffset());
    return query;
  }
}
