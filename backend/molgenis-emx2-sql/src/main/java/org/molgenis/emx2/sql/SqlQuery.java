package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.sql.SqlColumnExecutor.getJoinTableName;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;
import static org.molgenis.emx2.utils.TypeUtils.*;
import static org.molgenis.emx2.utils.TypeUtils.toJsonbArray;

public class SqlQuery extends QueryBean {
  public static final String COUNT_FIELD = "count";
  public static final String MAX_FIELD = "max";
  public static final String MIN_FIELD = "min";
  public static final String AVG_FIELD = "avg";
  public static final String SUM_FIELD = "sum";

  private static final String QUERY_FAILED = "Query failed";
  private static final String ANY_SQL = "{0} = ANY ({1})";
  private static final String JSON_AGG_SQL = "json_strip_nulls(json_agg(item))";
  private static final String ROW_TO_JSON_SQL = "json_strip_nulls(row_to_json(item))";
  private static final String ITEM = "item";
  private static final String OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE =
      "Operator %s is not support for column '%s'";
  private static final String BETWEEN_ERROR_MESSAGE =
      "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: %s";

  private static Logger logger = LoggerFactory.getLogger(SqlQuery.class);

  private SqlSchemaMetadata schema;

  public SqlQuery(SqlSchemaMetadata schema, String field) {
    super(field);
    this.schema = schema;
  }

  public SqlQuery(SqlSchemaMetadata schema, String field, SelectColumn[] selection) {
    super(field);
    this.schema = schema;
    this.select(selection);
  }

  @Override
  public List<Row> retrieveRows() {
    SelectColumn select = getSelect();
    Filter filter = getFilter();
    String[] searchTerms = getSearchTerms();

    SqlTableMetadata table = schema.getTableMetadata(select.getColumn());
    if (table == null) {
      throw new MolgenisException(
          "Query failed",
          "Field "
              + select.getColumn()
              + " unknown for retrieve rows in schema "
              + schema.getName());
    }
    String tableAlias = "root-" + table.getTableName();

    // if empty selection, we will add the default selection here
    if (select == null || select.getColumNames().isEmpty()) {
      select = new SelectColumn(table.getTableName(), table.getColumnNames());
    }

    // basequery
    SelectJoinStep<Record> from =
        table
            .getJooq()
            .select(rowSelectFields(table, tableAlias, "", select))
            .from(tableWithInheritanceJoin(table).as(tableAlias));

    // joins, only filtered tables
    from = refJoins(table, tableAlias, from, filter, select, new ArrayList<>());

    // where
    Condition condition = whereConditions(table, tableAlias, filter, searchTerms);
    SelectConnectByStep<Record> where = condition != null ? from.where(condition) : from;
    SelectConnectByStep<Record> query = limitOffsetOrderBy(select, where);

    // execute
    try {
      List<Row> result = new ArrayList<>();
      if (logger.isInfoEnabled()) {
        logger.info(query.getSQL(ParamType.INLINED));
      }
      Result<Record> fetch = query.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }
      return result;
    } catch (DataAccessException | SQLException e) {
      throw new MolgenisException(QUERY_FAILED, e);
    }
  }

  private static List<Field<?>> rowSelectFields(
      TableMetadata table, String tableAlias, String prefix, SelectColumn selection) {

    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn select : selection.getSubselect()) {
      Column column = isValidColumn(table, select.getColumn());
      String columnAlias = prefix.equals("") ? column.getName() : prefix + "-" + column.getName();
      if (FILE.equals(column.getColumnType())) {
        // check what they want to get, contents, mimetype, size and/or extension
        if (select.has("id")) {
          fields.add(field(name(column.getName() + "-id")));
        }
        if (select.has("contents")) {
          fields.add(field(name(column.getName() + "-contents")));
        }
        if (select.has("size")) {
          fields.add(field(name(column.getName() + "-size")));
        }
        if (select.has("mimetype")) {
          fields.add(field(name(column.getName() + "-mimetype")));
        }
        if (select.has("extension")) {
          fields.add(field(name(column.getName() + "-extension")));
        }
      } else if (column.isReference()
          // if subselection, then we will add it as subselect
          && !select.getSubselect().isEmpty()) {
        fields.addAll(
            rowSelectFields(
                column.getRefTable(),
                tableAlias + "-" + column.getName(),
                columnAlias,
                selection.getSubselect(column.getName())));
      } else if (MREF.equals(column.getColumnType())) {
        fields.add(rowMrefSubselect(column, tableAlias).as(columnAlias));
      } else if (REFBACK.equals(column.getColumnType())) {
        fields.add(
            field("array({0})", rowBackrefSubselect(column, tableAlias)).as(column.getName()));
      } else if (REF.equals(column.getColumnType()) || REF_ARRAY.equals(column.getColumnType())) {
        fields.addAll(
            column.getReferences().stream()
                .filter(ref -> !ref.isExisting())
                .map(
                    ref ->
                        field(name(tableAlias, ref.getName()), ref.getJooqType()).as(columnAlias))
                .collect(Collectors.toList()));
      } else {
        fields.add(field(name(tableAlias, column.getName()), column.getJooqType()).as(columnAlias));
      }
    }
    if (fields.isEmpty()) {
      fields.addAll(
          table.getColumnNames().stream()
              .map(c -> field(name(tableAlias, c)))
              .collect(Collectors.toList()));
    }
    return fields;
  }

  private static Field<Object[]> rowMrefSubselect(Column column, String tableAlias) {
    Column reverseToColumn = column.getTable().getPrimaryKeyColumns().get(0);
    // reverse column = primaryKey of 'getTable()' or in case of REFBACK it needs to found by
    // mappedBy
    for (Column c : column.getRefTable().getColumns()) {
      if (column.getName().equals(c.getMappedBy())) {
        reverseToColumn = c;
        break;
      }
    }
    return PostgresDSL.array(
        DSL.select(field(name(getJoinTableName(column), column.getName())))
            .from(name(column.getTable().getSchema().getName(), getJoinTableName(column)))
            .where(
                field(name(getJoinTableName(column), reverseToColumn.getName()))
                    .eq(field(name(tableAlias, reverseToColumn.getName())))));
  }

  private static SelectConditionStep<Record> rowBackrefSubselect(Column column, String tableAlias) {
    Column mappedBy = column.getMappedByColumn();
    List<Condition> where = new ArrayList<>();
    for (Reference ref : mappedBy.getReferences()) {
      switch (mappedBy.getColumnType()) {
        case REF:
          where.add(
              field(name(mappedBy.getTable().getTableName(), ref.getName()))
                  .eq(field(name(tableAlias, ref.getTo()))));
          break;
        case REF_ARRAY:
          where.add(
              condition(
                  ANY_SQL,
                  field(name(tableAlias, ref.getTo())),
                  field(name(mappedBy.getTable().getTableName(), ref.getTo()))));
          break;
        default:
          throw new MolgenisException(
              "Internal error", "Refback for type not matched for column " + column.getName());
      }
    }
    return DSL.select(column.getRefTable().getPrimaryKeyFields())
        .from(name(mappedBy.getTable().getSchemaName(), mappedBy.getTable().getTableName()))
        .where(where);
  }

  @Override
  public String retrieveJSON() {
    SelectColumn select = getSelect();
    Filter filter = getFilter();
    String[] searchTerms = getSearchTerms();

    List<Field<Object>> fields = new ArrayList<>();
    DSLContext sql = schema.getJooq();

    // get the table from root select
    SqlTableMetadata table = schema.getTableMetadata(select.getColumn());
    if (table == null & select.getColumn().endsWith("_agg")) {
      table =
          schema.getTableMetadata(select.getColumn().substring(0, select.getColumn().length() - 4));
    }
    if (table == null) {
      throw new MolgenisException(
          "RetrieveJSON failed",
          "Field "
              + select.getColumn()
              + " unknown for JSON queries in schema "
              + schema.getName());
    }

    // root query
    CommonTableExpression<Record> with =
        name(table.getTableName()).as(jsonRootQuery(table, filter, searchTerms));

    if (!select.getColumn().endsWith("_agg")) {
      // get the selected fields
      Collection<Field<?>> selectFields = jsonSubselectFields(table, table.getTableName(), select);
      // create query
      Table<Record> query =
          limitOffsetOrderBy(select, sql.select(selectFields).from(name(table.getTableName())))
              .asTable(ITEM);
      // limit/offset

      // aggregate into json
      fields.add(field(sql.select(field(JSON_AGG_SQL)).from(query)).as(select.getColumn()));
    } else {
      // get the selected fields
      Collection<Field<?>> aggFields = jsonAggregateFields(table, table.getTableName(), select);
      // create query
      Table<Record> query = sql.select(aggFields).from(name(table.getTableName())).asTable(ITEM);
      // aggregate into json
      fields.add(field(sql.select(field(ROW_TO_JSON_SQL)).from(query)).as(select.getColumn()));
    }

    // asemble final query
    SelectJoinStep<Record1<Object>> query =
        sql.with(with).select(field(ROW_TO_JSON_SQL)).from(table(sql.select(fields)).as(ITEM));

    if (logger.isInfoEnabled()) {
      logger.info(query.getSQL(ParamType.INLINED));
    }

    return query.fetchOne().get(0, String.class);
  }

  private static SelectConnectByStep<Record> jsonRootQuery(
      SqlTableMetadata table, Filter filter, String[] searchTerms) {
    String tableAlias = table.getTableName();

    SelectJoinStep<Record> subquery =
        table
            .getJooq()
            .select(List.of(field("{0}.*", name(tableAlias)))) // expensive for binary data!!!
            .from(tableWithInheritanceJoin(table).as(tableAlias));

    // joins, only filtered tables
    subquery =
        refJoins(table, table.getTableName(), subquery, filter, null, new ArrayList<String>());

    // where
    Condition condition = whereConditions(table, tableAlias, filter, searchTerms);
    return condition != null ? subquery.where(condition) : subquery;
  }

  private static Field<?> jsonSubselect(
      SqlTableMetadata table, Column column, String tableAlias, SelectColumn select) {

    // in case the root subselect then column == null
    DSLContext jooq = table.getJooq();
    String subAlias = tableAlias + "-" + column.getName();

    SelectConditionStep<Record> from =
        jooq
            // select
            .select(jsonSubselectFields(table, subAlias, select))
            // from
            .from(tableWithInheritanceJoin(table).as(subAlias))
            // where
            .where(refJoinCondition(column, tableAlias, subAlias));

    from = (SelectConditionStep<Record>) limitOffsetOrderBy(select, from);

    String agg = REF.equals(column.getColumnType()) ? ROW_TO_JSON_SQL : JSON_AGG_SQL;

    return field(jooq.select(field(agg)).from(from.asTable(ITEM))).as(select.getColumn());
  }

  private static Collection<Field<?>> jsonSubselectFields(
      TableMetadata table, String tableAlias, SelectColumn selection) {
    List<Field<?>> fields = new ArrayList<>();

    // include primary key
    for (Field<?> pkey : table.getPrimaryKeyFields()) {
      if (!selection.has(pkey.getName())) {
        fields.add(field(name(tableAlias, pkey.getName())));
      }
    }

    for (SelectColumn select : selection.getSubselect()) {
      Column column = table.getColumn(select.getColumn());

      // validate that column exists
      if (column == null) {
        // aggregation column?
        column = isValidColumn(table, select.getColumn().replace("_agg", ""));
      }

      // add the fields, using subselects for references
      if (FILE.equals(column.getColumnType())) {
        DSLContext jooq = ((SqlTableMetadata) table).getJooq();
        List<Field> subFields = new ArrayList<>();
        for (String ext : new String[] {"id", "contents", "size", "extension", "mimetype"}) {
          if (select.has(ext)) {
            subFields.add(field(name(tableAlias, column.getName() + "-" + ext)).as(ext));
          }
        }
        fields.add(
            field((jooq.select(field(ROW_TO_JSON_SQL)).from(jooq.select(subFields).asTable(ITEM))))
                .as(select.getColumn()));
      } else if (column.isReference() && select.getColumn().endsWith("_agg")) {
        // aggregation
        fields.add(
            jsonAggregateSelect(
                (SqlTableMetadata) column.getRefTable(), column, tableAlias, select));
      } else if (column.isReference()) {
        // ref subselect
        fields.add(
            jsonSubselect((SqlTableMetadata) column.getRefTable(), column, tableAlias, select));
      } else {
        // primitive fields
        fields.add(field(name(tableAlias, column.getName())));
      }
    }
    return fields;
  }

  private static Field<?> jsonAggregateSelect(
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
                                .select(jsonAggregateFields(table, subAlias, select))
                                .from(tableWithInheritanceJoin(table).as(subAlias))
                                // where
                                .where(refJoinCondition(column, tableAlias, subAlias)))
                        .as(ITEM)))
        .as(select.getColumn());
  }

  private static Collection<Field<?>> jsonAggregateFields(
      SqlTableMetadata table, String tableAlias, SelectColumn select) {
    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn field : select.getSubselect()) {

      if (COUNT_FIELD.equals(field.getColumn())) {
        fields.add(count().as(COUNT_FIELD));
      } else {
        Column column = isValidColumn(table, field.getColumn());
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

  private static Table<Record> tableWithInheritanceJoin(TableMetadata table) {
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

  private static SelectJoinStep<Record> refJoins(
      TableMetadata table,
      String tableAlias,
      SelectJoinStep<Record> join,
      Filter filters,
      SelectColumn selection,
      List<String> aliasList) {

    // filter based joins
    if (filters != null) {
      for (Filter filter : filters.getSubfilters()) {
        if (OR.equals(filter.getOperator()) || AND.equals(filter.getOperator())) {
          join = refJoins(table, tableAlias, join, filter, selection, aliasList);
        } else {
          Column column = isValidColumn(table, filter.getColumn());
          if (column.isReference() && !filter.getSubfilters().isEmpty()) {
            String subAlias = tableAlias + "-" + column.getName();
            if (!aliasList.contains(subAlias)) {
              // to ensure only join once
              aliasList.add(subAlias);
              // the join
              join.leftJoin(tableWithInheritanceJoin(column.getRefTable()).as(subAlias))
                  .on(refJoinCondition(column, tableAlias, subAlias));
              // recurse
              join =
                  refJoins(
                      column.getRefTable(),
                      subAlias,
                      join,
                      filter,
                      selection != null ? selection.getSubselect(column.getName()) : null,
                      aliasList);
            }
          }
        }
      }
    }
    // add missing selection joins, only used for row based queries
    if (selection != null) {
      for (SelectColumn select : selection.getSubselect()) {
        // then do same as above
        Column column = isValidColumn(table, select.getColumn());
        if (column.isReference()) {
          String subAlias = tableAlias + "-" + column.getName();
          // only join if subselection extists
          if (!aliasList.contains(subAlias) && !select.getSubselect().isEmpty()) {
            aliasList.add(subAlias);
            join.leftJoin(tableWithInheritanceJoin(column.getRefTable()).as(subAlias))
                .on(refJoinCondition(column, tableAlias, subAlias));
            // recurse
            join =
                refJoins(
                    column.getRefTable(),
                    subAlias,
                    join,
                    filters != null ? filters.getSubfilter(column.getName()) : null,
                    select,
                    aliasList);
          }
        }
      }
    }
    return join;
  }

  private static Condition refJoinCondition(Column column, String tableAlias, String subAlias) {
    List<Condition> foreignKeyMatch = new ArrayList<>();

    if (REF.equals(column.getColumnType())) {
      foreignKeyMatch.addAll(
          column.getReferences().stream()
              .map(
                  ref ->
                      field(name(subAlias, ref.getTo())).eq(field(name(tableAlias, ref.getName()))))
              .collect(Collectors.toList()));
    } else if (REF_ARRAY.equals(column.getColumnType())) {
      String refs =
          column.getReferences().stream()
              .map(ref -> name(tableAlias, ref.getName()).toString())
              .collect(Collectors.joining(","));
      String to =
          column.getReferences().stream()
              .map(ref -> name(subAlias, ref.getTo()).toString())
              .collect(Collectors.joining(","));
      String as =
          column.getReferences().stream()
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
            mappedBy.getReferences().stream()
                .map(
                    ref ->
                        field(name(subAlias, ref.getName()))
                            .eq(field(name(tableAlias, ref.getTo()))))
                .collect(Collectors.toList()));
      } else if (REF_ARRAY.equals(mappedBy.getColumnType())) {
        foreignKeyMatch.addAll(
            mappedBy.getReferences().stream()
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
      for (Reference ref : column.getReferences()) {
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
      throw new SqlQueryException(
          "Internal error",
          "For column " + column.getTable().getTableName() + "." + column.getName());
    }
    return and(foreignKeyMatch);
  }

  private static Condition whereConditions(
      TableMetadata table, String tableAlias, Filter filter, String[] searchTerms) {
    Condition searchCondition = whereConditionSearch(table, tableAlias, searchTerms);
    Condition filterCondition = whereConditionsFilter(table, tableAlias, filter);

    if (searchCondition != null && filterCondition != null) {
      return and(searchCondition, filterCondition);
    } else if (searchCondition != null) {
      return searchCondition;
    } else if (filterCondition != null) {
      return filterCondition;
    } else {
      return null;
    }
  }

  private static Condition whereConditionsFilter(
      TableMetadata table, String tableAlias, Filter filters) {
    List<Condition> conditions = new ArrayList<>();
    for (Filter filter : filters.getSubfilters()) {
      if (Operator.OR.equals(filter.getOperator())) {
        conditions.add(
            or(
                filter.getSubfilters().stream()
                    .map(f -> whereConditionsFilter(table, tableAlias, f))
                    .collect(Collectors.toList())));
      } else if (Operator.AND.equals(filter.getOperator())) {
        conditions.add(
            and(
                filter.getSubfilters().stream()
                    .map(f -> whereConditionsFilter(table, tableAlias, f))
                    .collect(Collectors.toList())));
      } else {
        Column column = isValidColumn(table, filter.getColumn());
        if (column.isReference()) {
          conditions.add(
              whereConditionsFilter(
                  column.getRefTable(), tableAlias + "-" + column.getName(), filter));
        } else if (FILE.equals(column.getColumnType())) {
          Filter sub = filter.getSubfilter("id");
          // todo expand properly
          if (sub != null && EQUALS.equals(sub.getOperator())) {
            conditions.add(field(name(column.getName() + "-id")).in(sub.getValues()));
          } else {
            throw new MolgenisException("Invalid filter for file", "");
          }
        } else {
          conditions.add(
              whereCondition(
                  tableAlias,
                  column.getName(),
                  column.getColumnType(),
                  filter.getOperator(),
                  filter.getValues()));
        }
      }
    }
    return conditions.isEmpty() ? null : and(conditions);
  }

  private static Condition whereCondition(
      String tableAlias,
      String columnName,
      ColumnType type,
      org.molgenis.emx2.Operator operator,
      Object[] values) {
    Name name = name(tableAlias, columnName);
    switch (type) {
      case TEXT:
      case STRING:
        return whereConditionText(name, operator, toStringArray(values));
      case BOOL:
        return whereConditionEquals(name, operator, toBoolArray(values));
      case UUID:
        return whereConditionEquals(name, operator, toUuidArray(values));
      case JSONB:
        return whereConditionEquals(name, operator, toJsonbArray(values));
      case INT:
        return whereConditionOrdinal(name, operator, toIntArray(values));
      case DECIMAL:
        return whereConditionOrdinal(name, operator, toDecimalArray(values));
      case DATE:
        return whereConditionOrdinal(name, operator, toDateArray(values));
      case DATETIME:
        return whereConditionOrdinal(name, operator, toDateTimeArray(values));
      case STRING_ARRAY:
      case TEXT_ARRAY:
        return whereConditionTextArray(name, operator, toStringArray(values));
      case BOOL_ARRAY:
        return whereCondtionArrayEquals(name, operator, toBoolArray(values));
      case UUID_ARRAY:
        return whereCondtionArrayEquals(name, operator, toUuidArray(values));
      case INT_ARRAY:
        return whereCondtionArrayEquals(name, operator, toIntArray(values));
      case DECIMAL_ARRAY:
        return whereCondtionArrayEquals(name, operator, toDecimalArray(values));
      case DATE_ARRAY:
        return whereCondtionArrayEquals(name, operator, toDateArray(values));
      case DATETIME_ARRAY:
        return whereCondtionArrayEquals(name, operator, toDateTimeArray(values));
      case JSONB_ARRAY:
        return whereCondtionArrayEquals(name, operator, toJsonbArray(values));
      default:
        throw new SqlQueryException(
            SqlQuery.QUERY_FAILED,
            "Filter of '"
                + name
                + " failed: operator "
                + operator
                + " not supported for type "
                + type);
    }
  }

  private static Condition whereConditionEquals(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    if (EQUALS.equals(operator)) {
      return field(columnName).in(values);
    } else if (NOT_EQUALS.equals(operator)) {
      return not(field(columnName).in(values));
    } else {
      throw new SqlQueryException(
          SqlQuery.QUERY_FAILED, SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, columnName);
    }
  }

  private static Condition whereCondtionArrayEquals(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    switch (operator) {
      case EQUALS:
        conditions.add(condition("{0} && {1}", values, field(columnName)));
        break;
      case NOT_EQUALS:
        not = true;
        conditions.add(condition("{0} && {1}", values, field(columnName)));
        break;
      default:
        throw new SqlQueryException(
            SqlQuery.QUERY_FAILED,
            SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
            operator,
            columnName);
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition whereConditionTextArray(
      Name columnName, org.molgenis.emx2.Operator operator, String[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (String value : values) {
      switch (operator) {
        case EQUALS:
          conditions.add(condition("{0} = ANY({1})", value, field(columnName)));
          break;
        case NOT_EQUALS:
          not = true;
          conditions.add(condition("{0} = ANY({1})", value, field(columnName)));
          break;
        case NOT_LIKE:
          not = true;
          conditions.add(
              condition(
                  "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE v ILIKE {0})",
                  "%" + value + "%", field(columnName)));
          break;
        case LIKE:
          conditions.add(
              condition(
                  "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE v ILIKE {0})",
                  "%" + value + "%", field(columnName)));
          break;
        case TRIGRAM_SEARCH:
          conditions.add(
              condition(
                  "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE word_similarity({0},v) > 0.6",
                  value, field(columnName)));
          break;
        case TEXT_SEARCH:
          conditions.add(
              condition(
                  "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE to_tsquery({0}) @@ to_tsvector(v)",
                  value.trim().replaceAll("\\s+", ":* & ") + ":*", field(columnName)));
          break;
        default:
          throw new SqlQueryException(
              SqlQuery.QUERY_FAILED,
              SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
              operator,
              columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition whereConditionText(
      Name columnName, org.molgenis.emx2.Operator operator, String[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (String value : values) {
      switch (operator) {
        case EQUALS:
          conditions.add(field(columnName).eq(value));
          break;
        case NOT_EQUALS:
          not = true;
          conditions.add(field(columnName).eq(value));
          break;
        case NOT_LIKE:
          not = true;
          conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          break;
        case LIKE:
          conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          break;
        case TRIGRAM_SEARCH:
          conditions.add(condition("word_similarity({0},{1}) > 0.6", value, field(columnName)));
          break;
        case TEXT_SEARCH:
          conditions.add(
              condition(
                  "to_tsquery({0}) @@ to_tsvector({1})",
                  value.trim().replaceAll("\\s+", ":* & ") + ":*", field(columnName)));
          break;
        default:
          throw new SqlQueryException(
              SqlQuery.QUERY_FAILED,
              SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
              operator,
              columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition whereConditionOrdinal(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (int i = 0; i < values.length; i++) {
      switch (operator) {
        case EQUALS:
        case NOT_EQUALS:
          return whereConditionEquals(columnName, operator, values);
        case NOT_BETWEEN:
          not = true;
          if (i + 1 > values.length)
            throw new SqlQueryException(
                SqlQuery.QUERY_FAILED, SqlQuery.BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
          if (values[i] != null && values[i + 1] != null) {
            conditions.add(field(columnName).notBetween(values[i], values[i + 1]));
          } else if (values[i] != null && values[i + 1] == null) {
            conditions.add(field(columnName).lessOrEqual(values[i]));
          } else if (values[i] == null && values[i + 1] != null) {
            conditions.add(field(columnName).greaterOrEqual(values[i + 1]));
          } else {
            // nothing to do
          }
          i++; // NOSONAR
          break;
        case BETWEEN:
          if (i + 1 > values.length)
            throw new SqlQueryException(
                SqlQuery.QUERY_FAILED, SqlQuery.BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
          if (values[i] != null && values[i + 1] != null) {
            conditions.add(field(columnName).between(values[i], values[i + 1]));
          } else if (values[i] != null && values[i + 1] == null) {
            conditions.add(field(columnName).greaterOrEqual(values[i]));
          } else if (values[i] == null && values[i + 1] != null) {
            conditions.add(field(columnName).lessOrEqual(values[i + 1]));
          } else {
            // nothing to do
          }
          i++; // NOSONAR
          break;
        default:
          throw new SqlQueryException(
              SqlQuery.QUERY_FAILED,
              SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
              operator,
              columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition whereConditionSearch(
      TableMetadata table, String tableAlias, String[] searchTerms) {
    List<Condition> searchConditions = new ArrayList<>();
    while (table != null) {
      List<Condition> subConditions = new ArrayList<>();
      // will get inherit tables too
      for (String term : searchTerms) {
        for (String subTerm : term.split(" ")) {
          subTerm = subTerm.trim();
          Field<Object> field = field(name(tableAlias, searchColumnName(table)));
          // short terms with 'like', longer with trigram
          if (subTerm.length() <= 3) subConditions.add(field.likeIgnoreCase("%" + subTerm + "%"));
          else {
            subConditions.add(condition("word_similarity({0},{1}) > 0.6", subTerm, field));
          }
        }
      }
      table = table.getInheritedTable();
      if (!subConditions.isEmpty()) {
        searchConditions.add(and(subConditions));
      }
    }
    return searchConditions.isEmpty() ? null : or(searchConditions);
  }

  private static SelectConnectByStep<Record> limitOffsetOrderBy(
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

  private static Column isValidColumn(TableMetadata table, String columnName) {
    Column column = table.getColumn(columnName);
    if (column == null) {
      throw new MolgenisException(
          "Query failed",
          "Column '" + columnName + "' is unknown in table " + table.getTableName());
    }
    return column;
  }
}
