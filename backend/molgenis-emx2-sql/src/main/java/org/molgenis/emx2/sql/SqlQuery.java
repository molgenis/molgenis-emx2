package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.sql.SqlColumnExecutor.getJoinTableName;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;
import static org.molgenis.emx2.utils.TypeUtils.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

public class SqlQuery extends QueryBean {
  public static final String COUNT_FIELD = "count";
  public static final String GROUPBY_FIELD = "groupBy";
  public static final String MAX_FIELD = "max";
  public static final String MIN_FIELD = "min";
  public static final String AVG_FIELD = "avg";
  public static final String SUM_FIELD = "sum";

  private static final String QUERY_FAILED = "Query failed: ";
  private static final String ANY_SQL = "{0} = ANY ({1})";
  private static final String JSON_AGG_SQL = "json_agg(item)";
  private static final String ROW_TO_JSON_SQL = "row_to_json(item)";
  private static final String ITEM = "item";
  private static final String OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE =
      "Operator %s is not support for column '%s'";
  private static final String BETWEEN_ERROR_MESSAGE =
      "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: %s";

  private static final Logger logger = LoggerFactory.getLogger(SqlQuery.class);

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
          "Query failed: Field "
              + select.getColumn()
              + " unknown for retrieve rows in schema "
              + schema.getName());
    }
    String tableAlias = "root-" + table.getTableName();

    // if empty selection, we will add the default selection here, excl File and Refback
    if (select == null || select.getColumNames().isEmpty()) {
      for (Column c : table.getColumns()) {
        // currently we don't download refBack (good) and files (that is bad)
        if (c.isFile()) {
          select.select(c.getName());
        } else if (!c.isRefback()) {
          if (c.isReference()) {
            for (Reference ref : c.getReferences()) {
              select.select(ref.getName());
            }
          } else {
            // don't include refBack or files or mg_ columns
            select.select(c.getName());
          }
        }
      }
    }

    // basequery
    SelectJoinStep<org.jooq.Record> from =
        table
            .getJooq()
            .select(rowSelectFields(table, tableAlias, "", select))
            .from(tableWithInheritanceJoin(table).as(tableAlias));

    // joins, only filtered tables
    from = refJoins(table, tableAlias, from, filter, select, new ArrayList<>());

    // where
    Condition condition = whereConditions(table, tableAlias, filter, searchTerms);
    SelectConnectByStep<org.jooq.Record> where = condition != null ? from.where(condition) : from;
    SelectConnectByStep<org.jooq.Record> query = limitOffsetOrderBy(select, where);

    // execute
    try {
      List<Row> result = new ArrayList<>();
      if (logger.isInfoEnabled()) {
        logger.info(query.getSQL(ParamType.INLINED));
      }
      Result<org.jooq.Record> fetch = query.fetch();
      for (org.jooq.Record r : fetch) {
        result.add(new SqlRow(r));
      }
      return result;
    } catch (DataAccessException | SQLException e) {
      throw new MolgenisException(QUERY_FAILED, e);
    }
  }

  // todo: allow query expansion accross tables?
  private static List<Field<?>> rowSelectFields(
      TableMetadata table, String tableAlias, String prefix, SelectColumn selection) {

    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn select : selection.getSubselect()) {
      Column column = isValidColumn(table, select.getColumn());
      String columnAlias = prefix.equals("") ? column.getName() : prefix + "-" + column.getName();
      if (column.isFile()) {
        // check what they want to get, contents, mimetype, size and/or extension
        if (select.getSubselect().size() == 0 || select.has("id")) {
          fields.add(field(name(column.getName())));
        }
        if (select.has("contents")) {
          fields.add(field(name(column.getName() + "_contents")));
        }
        if (select.has("size")) {
          fields.add(field(name(column.getName() + "_size")));
        }
        if (select.has("mimetype")) {
          fields.add(field(name(column.getName() + "_mimetype")));
        }
        if (select.has("extension")) {
          fields.add(field(name(column.getName() + "_extension")));
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
      } else if (column.isRefback()) {
        fields.add(
            field("array({0})", rowBackrefSubselect(column, tableAlias)).as(column.getName()));
      } else if (column.isReference()) { // REF and REF_ARRAY
        // might be composite column with same name
        Reference ref = null;
        for (Reference r : column.getReferences()) {
          if (r.getName().equals(column.getName())) {
            ref = r;
          }
        }
        if (ref == null) {
          throw new MolgenisException(
              "Select of column '"
                  + column.getName()
                  + "' failed: composite foreign key requires subselection or explicit naming of underlying fields");
        } else {
          fields.add(field(name(tableAlias, column.getName()), ref.getJooqType()).as(columnAlias));
        }
      } else if (!column.isHeading()) {
        fields.add(field(name(tableAlias, column.getName()), column.getJooqType()).as(columnAlias));
      }
    }
    return fields;
  }

  private static Field<Object[]> rowMrefSubselect(Column column, String tableAlias) {
    Column reverseToColumn = column.getTable().getPrimaryKeyColumns().get(0);
    // reverse column = primaryKey of 'getTable()' or in case of REFBACK it needs to found by
    // refBack
    for (Column c : column.getRefTable().getColumns()) {
      if (column.getName().equals(c.getRefBack())) {
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

  private static SelectConditionStep<org.jooq.Record> rowBackrefSubselect(
      Column column, String tableAlias) {
    Column refBack = column.getRefBackColumn();
    List<Condition> where = new ArrayList<>();

    // might be composite
    for (Reference ref : refBack.getReferences()) {
      if (refBack.isRef()) {
        where.add(
            field(name(refBack.getTable().getTableName(), ref.getName()))
                .eq(field(name(tableAlias, ref.getRefTo()))));
      } else if (refBack.isRefArray()) {
        where.add(
            condition(
                ANY_SQL,
                field(name(tableAlias, ref.getRefTo())),
                field(name(refBack.getTable().getTableName(), ref.getName()))));
      } else {
        throw new MolgenisException(
            "Internal error: Refback for type not matched for column " + column.getName());
      }
    }
    return DSL.select(column.getRefTable().getPrimaryKeyFields())
        .from(name(refBack.getSchemaName(), refBack.getTableName()))
        .where(where);
  }

  @Override
  public String retrieveJSON() {
    SelectColumn select = getSelect();
    List<Field<?>> fields = new ArrayList<>();
    DSLContext sql = schema.getJooq();

    // get the table from root select
    SqlTableMetadata table = schema.getTableMetadata(select.getColumn());
    if (table == null && select.getColumn().endsWith("_agg")) {
      table =
          schema.getTableMetadata(select.getColumn().substring(0, select.getColumn().length() - 4));
    }
    if (table == null) {
      throw new MolgenisException(
          "RetrieveJSON failed: Field "
              + select.getColumn()
              + " unknown for JSON queries in schema "
              + schema.getName());
    }
    if (select.getColumn().endsWith("_agg")) {
      fields.add(
          jsonAggregateSelect(
              table, null, table.getTableName(), select, getFilter(), getSearchTerms()));
    } else {
      // select all on root level as default
      if (select.getSubselect().size() == 0) {
        for (Column c : table.getColumns()) {
          select.select(c.getName());
        }
      }
      fields.add(
          jsonSubselect(table, null, table.getTableName(), select, getFilter(), getSearchTerms()));
    }

    // asemble final query
    SelectJoinStep<Record1<Object>> query =
        sql.select(field(ROW_TO_JSON_SQL)).from(table(sql.select(fields)).as(ITEM));

    long start = System.currentTimeMillis();
    String result = query.fetchOne().get(0, String.class);
    if (logger.isInfoEnabled()) {
      logger.info(
          "query in {}ms: {}", System.currentTimeMillis() - start, query.getSQL(ParamType.INLINED));
    }
    return result;
  }

  private static Field<?> jsonSubselect(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      SelectColumn select,
      Filter filters,
      String[] searchTerms) {

    // in case the root subselect then column == null
    DSLContext jooq = table.getJooq();
    String subAlias = tableAlias + (column != null ? "-" + column.getName() : "");

    SelectJoinStep<org.jooq.Record> from =
        jooq.select(jsonSubselectFields(table, subAlias, select))
            .from(tableWithInheritanceJoin(table).as(subAlias));

    List<Condition> conditions = new ArrayList<>();
    Select<org.jooq.Record> filterQuery =
        limitOffsetOrderBy(
            select, jsonFilterQuery(table, column, tableAlias, subAlias, filters, searchTerms));
    if (filters != null
        || searchTerms.length > 0
        || select.getLimit() > 0
        || select.getOffset() > 0) {
      List<Field> pkeyFields = table.getPrimaryKeyFields();
      if (pkeyFields.size() == 0) throw new MolgenisException("primary key not set");
      conditions.add(row(pkeyFields).in(filterQuery));
    }
    if (column != null) {
      conditions.add(refJoinCondition(column, tableAlias, subAlias));
    }
    if (!conditions.isEmpty()) {
      from = (SelectJoinStep<org.jooq.Record>) from.where(conditions);
    }

    String agg = column != null && column.isRef() ? ROW_TO_JSON_SQL : JSON_AGG_SQL;

    return field(jooq.select(field(agg)).from(orderBy(select, from).asTable(ITEM)))
        .as(select.getColumn());
  }

  private static SelectConditionStep<org.jooq.Record> jsonFilterQuery(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filters,
      String[] searchTerms) {

    List<Condition> conditions = new ArrayList<>();
    if (filters != null) {
      conditions.addAll(
          jsonFilterQueryConditions(table, column, tableAlias, subAlias, filters, searchTerms));
    }
    if (searchTerms.length > 0) {
      conditions.add(jsonSearchConditions(table, searchTerms));
    }

    // create the subquery
    if (!conditions.isEmpty()) {
      return table
          .getJooq()
          .select(table.getPrimaryKeyFields())
          .from(tableWithInheritanceJoin(table))
          .where(conditions);
    } else {
      return (SelectConditionStep<org.jooq.Record>)
          table.getJooq().select(table.getPrimaryKeyFields()).from(tableWithInheritanceJoin(table));
    }
  }

  private static List<Condition> jsonFilterQueryConditions(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filters,
      String[] searchTerms) {
    List<Condition> conditions = new ArrayList<>();
    DSLContext jooq = table.getJooq();
    if (filters != null) {
      for (Filter f : filters.getSubfilters()) {
        if (OR.equals(f.getOperator())) {
          conditions.add(
              or(jsonFilterQueryConditions(table, column, tableAlias, subAlias, f, searchTerms)));
        } else if (Operator.AND.equals(f.getOperator())) {
          conditions.add(
              and(jsonFilterQueryConditions(table, column, tableAlias, subAlias, f, searchTerms)));
        } else {
          Column c = isValidColumn(table, f.getColumn());
          if (c.isReference()) {
            // add subfilter where reference is in tuples from subquery
            // todo, if filter is on pkey we don't even need subquery!
            SelectConditionStep<org.jooq.Record> subQuery =
                jsonFilterQuery(
                    (SqlTableMetadata) c.getRefTable(),
                    column,
                    tableAlias,
                    subAlias,
                    f,
                    new String[0]);
            if (subQuery != null) {
              if (c.isRefArray()) {
                // if not composite it is simple array overlap
                if (c.getReferences().size() == 1) {
                  conditions.add(condition("{0} && ARRAY({1})", name(c.getName()), subQuery));
                } else {
                  // otherwise exists(unnest(ref_array) natural join (filterQuery))
                  List<Field> unnest =
                      c.getReferences().stream()
                          .map(
                              ref ->
                                  ref.isOverlappingRef()
                                      ? field(name(ref.getName())).as(name(ref.getRefTo()))
                                      : field("UNNEST({0})", name(ref.getName()))
                                          .as(name(ref.getRefTo())))
                          .collect(Collectors.toCollection(ArrayList::new));

                  conditions.add(
                      exists(selectFrom(jooq.select(unnest).asTable().naturalJoin(subQuery))));
                }
              } else if (c.isRefback()) {
                Column refBack = c.getRefBackColumn();
                List<Field> pkey =
                    c.getTable().getPrimaryKeyFields().stream().collect(Collectors.toList());
                List<Field> backRef =
                    c.getRefBackColumn().getReferences().stream()
                        .map(Reference::getJooqField)
                        .collect(Collectors.toList());
                List<Field> backRefKey =
                    c.getRefBackColumn().getTable().getPrimaryKeyFields().stream()
                        .collect(Collectors.toList());
                // can be ref, ref_array (mref is checked above)
                if (refBack.isRef()) {
                  // pkey in (backref from refBack table where backrefKey in subquery)
                  conditions.add(
                      row(pkey)
                          .in(
                              jooq.select(backRef)
                                  .from(c.getRefTable().getJooqTable())
                                  .where(row(backRefKey).in(subQuery))));
                } else {
                  // ref_array
                  // pkey in (unnest(backref) from mappedByTable where backrefKey in subquery)
                  conditions.add(
                      row(pkey)
                          .in(
                              jooq.select(
                                      c.getRefBackColumn().getReferences().stream()
                                          .map(
                                              bref ->
                                                  bref.isOverlappingRef()
                                                      ? field(name(bref.getName()))
                                                      : field("UNNEST({0})", name(bref.getName()))
                                                          .as(name(bref.getName())))
                                          .collect(Collectors.toList()))
                                  .from(c.getRefTable().getJooqTable())
                                  .where(row(backRefKey).in(subQuery))));
                }
              } else {
                // normal ref
                List<Field> refs =
                    c.getReferences().stream()
                        .map(Reference::getJooqField)
                        .collect(Collectors.toList());
                conditions.add(row(refs).in(subQuery));
              }
            }
          } else {
            // simple filter
            conditions.add(
                whereCondition(
                    c.getTableName(),
                    c.getName(),
                    c.getColumnType(),
                    f.getOperator(),
                    f.getValues()));
          }
        }
      }
    }
    return conditions;
  }

  private static Condition jsonSearchConditions(SqlTableMetadata table, String[] searchTerms) {
    // create search
    List<Condition> search = new ArrayList<>();
    for (String term : searchTerms) {
      search.add(
          field(name(table.getTableName(), searchColumnName(table.getTableName())))
              .likeIgnoreCase("%" + term + "%"));
      TableMetadata parent = table.getInheritedTable();
      while (parent != null) {
        search.add(
            field(name(parent.getTableName(), searchColumnName(parent.getTableName())))
                .likeIgnoreCase("%" + term + "%"));
        parent = parent.getInheritedTable();
      }
    }
    return or(search);
  }

  private static Collection<Field<?>> jsonSubselectFields(
      TableMetadata table, String tableAlias, SelectColumn selection) {
    List<Field<?>> fields = new ArrayList<>();

    // if no subselect, we will select primary keys
    if (selection.getSubselect().isEmpty()) {
      selection =
          s(
              selection.getColumn(),
              table.getPrimaryKeyColumns().stream()
                  .map(key -> s(key.getName()))
                  .toArray(SelectColumn[]::new));
    }

    for (SelectColumn select : selection.getSubselect()) {
      Column column =
          select.getColumn().endsWith("_agg")
              ? isValidColumn(table, select.getColumn().replace("_agg", ""))
              : isValidColumn(table, select.getColumn());

      // add the fields, using subselects for references
      if (column.isFile()) {
        fields.add(jsonFileField((SqlTableMetadata) table, tableAlias, select, column));
      } else if (column.isReference() && select.getColumn().endsWith("_agg")) {
        // aggregation subselect
        fields.add(
            jsonAggregateSelect(
                (SqlTableMetadata) column.getRefTable(),
                column,
                tableAlias,
                select,
                null,
                new String[0]));
      } else if (column.isReference()) {
        // normal subselect
        fields.add(
            jsonSubselect(
                (SqlTableMetadata) column.getRefTable(),
                column,
                tableAlias,
                select,
                null,
                new String[0]));
      } else {
        // primitive fields
        fields.add(field(name(tableAlias, column.getName())));
      }
    }
    return fields;
  }

  private static Field<Object> jsonFileField(
      SqlTableMetadata table, String tableAlias, SelectColumn select, Column column) {
    DSLContext jooq = table.getJooq();
    List<Field> subFields = new ArrayList<>();
    for (String ext : new String[] {"id", "contents", "size", "extension", "mimetype", "url"}) {
      if (select.has(ext)) {
        if (ext.equals("id")) {
          subFields.add(field(name(tableAlias, column.getName())).as(ext));
        } else if (ext.equals("url")) {
          subFields.add(
              field(
                      "'/"
                          + table.getSchemaName()
                          + "/api/file/"
                          + table.getTableName()
                          + "/"
                          + column.getName()
                          + "/' || {0}",
                      field(name(tableAlias, column.getName())))
                  .as(ext));
        } else {
          subFields.add(field(name(tableAlias, column.getName() + "_" + ext)).as(ext));
        }
      }
    }
    return field((jooq.select(field(ROW_TO_JSON_SQL)).from(jooq.select(subFields).asTable(ITEM))))
        .as(select.getColumn());
  }

  private static Field<?> jsonAggregateSelect(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      SelectColumn select,
      Filter filters,
      String[] searchTerms) {
    String subAlias = tableAlias + (column != null ? "-" + column.getName() : "");

    SelectSelectStep<org.jooq.Record> from =
        table
            .getJooq()
            .select(
                jsonAggregateFields(
                    table, column, tableAlias, subAlias, select, filters, searchTerms));

    return field(table.getJooq().select(field(ROW_TO_JSON_SQL)).from(table(from).as(ITEM)))
        .as(select.getColumn());
  }

  private static Collection<Field<?>> jsonAggregateFields(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      SelectColumn select,
      Filter filter,
      String[] searchTerms) {
    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn field : select.getSubselect()) {

      // count only uses the filter query to count
      if (COUNT_FIELD.equals(field.getColumn())) {
        fields.add(jsonCountField(table, column, tableAlias, subAlias, filter, searchTerms));
      } else if (GROUPBY_FIELD.equals(field.getColumn())) {
        fields.add(
            jsonAggregateGroupBy(table, column, field, tableAlias, subAlias, filter, searchTerms));
      } else {
        Column c = isValidColumn(table, field.getColumn());
        if (field.has(MAX_FIELD)
            || field.has(MIN_FIELD)
            || field.has(AVG_FIELD)
            || field.has(SUM_FIELD)) {
          fields.add(
              jsonAggField(table, column, tableAlias, subAlias, filter, searchTerms, field, c));
        }
      }
    }
    return fields;
  }

  private static Field jsonAggregateGroupBy(
      SqlTableMetadata table,
      Column column,
      SelectColumn groupBy,
      String tableAlias,
      String subAlias,
      Filter filter,
      String[] searchTerms) {
    DSLContext jooq = table.getJooq();

    // filter conditions
    Condition condition = null;
    if (filter != null || searchTerms.length > 1) {
      condition =
          row(table.getPrimaryKeyFields())
              .in(jsonFilterQuery(table, column, tableAlias, subAlias, filter, searchTerms));
    }

    List<SelectConnectByStep> subQuery = new ArrayList<>();
    List<Field> selectFields = new ArrayList<>();
    List<Field> groupByFields = new ArrayList<>();
    for (SelectColumn field : groupBy.getSubselect()) {
      if (COUNT_FIELD.equals(field.getColumn())) {
        selectFields.add(field("COUNT(*)"));
      } else {
        Column c = isValidColumn(table, field.getColumn());
        selectFields.add(c.getJooqField());
        List<Field> subselectFields = new ArrayList<>();
        subselectFields.addAll(table.getPrimaryKeyFields());
        // if array we unnest
        if (c.getColumnType().isArray()) {
          subselectFields.add(field("unnest({0})", c.getJooqField()).as(c.getJooqField()));
        } else {
          subselectFields.add(c.getJooqField());
        }
        if (condition != null) {
          subQuery.add(
              jooq.select(subselectFields)
                  .from(tableWithInheritanceJoin(table).as(tableAlias))
                  .where(condition));
        } else {
          subQuery.add(
              jooq.select(subselectFields).from(tableWithInheritanceJoin(table).as(tableAlias)));
        }
        groupByFields.add(c.getJooqField());
      }
    }

    SelectJoinStep<org.jooq.Record> groupByQuery =
        table.getJooq().select(selectFields).from(subQuery.get(0));
    for (int i = 1; i < subQuery.size(); i++) {
      groupByQuery = groupByQuery.naturalJoin(subQuery.get(i));
    }

    return field(
            jooq.select(field(JSON_AGG_SQL))
                .from(groupByQuery.groupBy(groupByFields).asTable(ITEM)))
        .as(GROUPBY_FIELD);
  }

  private static Field jsonAggField(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filter,
      String[] searchTerms,
      SelectColumn field,
      Column c) {
    // add to subselect as input for agg functions
    // add the agg functions

    Field aggField =
        field(
            "json_build_object({0},{1},{2},{3},{4},{5},{6},{7})",
            MAX_FIELD,
            max(field(name(tableAlias, c.getName()))),
            MIN_FIELD,
            min(field(name(tableAlias, c.getName()))),
            AVG_FIELD,
            avg(field(name(tableAlias, c.getName()), c.getJooqType())),
            SUM_FIELD,
            sum(field(name(tableAlias, c.getName()), c.getJooqType())));

    SelectJoinStep aggQuery =
        table.getJooq().select(aggField).from(tableWithInheritanceJoin(table).as(tableAlias));

    // filter on any filter settings
    if (column != null || filter != null || searchTerms.length > 1) {
      Condition condition =
          row(table.getPrimaryKeyFields())
              .in(jsonFilterQuery(table, column, tableAlias, subAlias, filter, searchTerms));
      aggQuery = (SelectJoinStep) aggQuery.where(condition);
    }
    return field(aggQuery).as(field.getColumn());
  }

  private static Field jsonCountField(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filter,
      String[] searchTerms) {
    Field countField;
    if (column != null) {
      List<Condition> conditions = new ArrayList<>();
      if (filter != null || searchTerms.length > 1) {
        List<Field> pkeyFields = table.getPrimaryKeyFields();
        if (pkeyFields.size() == 0) throw new MolgenisException("primary key not set");
        conditions.add(
            row(pkeyFields)
                .in(jsonFilterQuery(table, column, tableAlias, subAlias, filter, searchTerms)));
      }
      conditions.add(refJoinCondition(column, tableAlias, subAlias));
      countField =
          field(
                  table
                      .getJooq()
                      .select(count())
                      .from(tableWithInheritanceJoin(table).as(subAlias))
                      .where(conditions))
              .as(COUNT_FIELD);
    } else {
      countField =
          field(
                  table
                      .getJooq()
                      .select(count())
                      .from(
                          jsonFilterQuery(
                              table, column, tableAlias, subAlias, filter, searchTerms)))
              .as(COUNT_FIELD);
    }
    return countField;
  }

  private static Table<org.jooq.Record> tableWithInheritanceJoin(TableMetadata table) {

    Table<org.jooq.Record> result = table.getJooqTable();
    TableMetadata inheritedTable = table.getInheritedTable();
    // root and intermediate levels have mg_tableclass column
    Column mg_tableclass = table.getLocalColumn(MG_TABLECLASS);
    while (inheritedTable != null) {
      List<Field> using = inheritedTable.getPrimaryKeyFields();
      if (mg_tableclass != null) {
        using.add(mg_tableclass.getJooqField());
      }
      result = result.join(inheritedTable.getJooqTable()).using(using.toArray(new Field<?>[0]));
      inheritedTable = inheritedTable.getInheritedTable();
      if (inheritedTable != null) {
        mg_tableclass = inheritedTable.getLocalColumn(MG_TABLECLASS);
      }
    }
    return result;
  }

  private static SelectJoinStep<org.jooq.Record> refJoins(
      TableMetadata table,
      String tableAlias,
      SelectJoinStep<org.jooq.Record> join,
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

    if (column.isRef()) {
      if (column.getReferences().size() == 1) {
        Reference ref = column.getReferences().get(0);
        foreignKeyMatch.add(
            field(name(subAlias, ref.getRefTo())).eq(field(name(tableAlias, ref.getName()))));
      } else {
        foreignKeyMatch.add(
            and(
                // at least one column not null
                or(
                    column.getReferences().stream()
                        .map(ref -> field(name(tableAlias, ref.getName())).isNotNull())
                        .collect(Collectors.toList())),
                // and matches on values or nulls
                and(
                    column.getReferences().stream()
                        .map(
                            ref ->
                                field(name(subAlias, ref.getRefTo()))
                                    .eq(field(name(tableAlias, ref.getName()))))
                        .collect(Collectors.toList()))));
      }
    } else if (column.isRefArray()) {
      if (column.getReferences().size() == 1) {
        Reference ref = column.getReferences().get(0);
        // simple array comparison
        foreignKeyMatch.add(
            condition(
                "{0} = ANY({1})", name(subAlias, ref.getRefTo()), name(tableAlias, ref.getName())));
      } else {
        // expensive 'in' query to enable join on all fields
        List<Field> to =
            column.getReferences().stream()
                .map(ref -> field(name(subAlias, ref.getRefTo()).toString()))
                .collect(Collectors.toList());

        List<Field> unnest =
            column.getReferences().stream()
                .map(
                    r ->
                        r.isOverlappingRef()
                            ? field(name(tableAlias, r.getName()))
                            : field("UNNEST({0})", name(tableAlias, r.getName())))
                .collect(Collectors.toList());
        foreignKeyMatch.add(row(to).in(DSL.select(unnest)));
      }
    } else if (column.isRefback()) {
      Column refBack = column.getRefBackColumn();
      if (refBack.isRef()) {
        foreignKeyMatch.addAll(
            refBack.getReferences().stream()
                .map(
                    ref ->
                        field(name(subAlias, ref.getName()))
                            .eq(field(name(tableAlias, ref.getRefTo()))))
                .collect(Collectors.toList()));
      } else if (refBack.isRefArray()) {
        foreignKeyMatch.addAll(
            refBack.getReferences().stream()
                .map(
                    ref ->
                        ref.isOverlappingRef()
                            ? field(name(tableAlias, ref.getRefTo()))
                                .eq(field(name(subAlias, ref.getName())))
                            : condition(
                                ANY_SQL,
                                field(name(tableAlias, ref.getRefTo())),
                                field(name(subAlias, ref.getName()))))
                .collect(Collectors.toList()));
      }
    } else {
      throw new SqlQueryException(
          "Internal error: For column "
              + column.getTable().getTableName()
              + "."
              + column.getName());
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

    if (Operator.OR.equals(filters.getOperator())) {
      conditions.add(
          or(
              filters.getSubfilters().stream()
                  .map(f -> whereConditionsFilter(table, tableAlias, f))
                  .collect(Collectors.toList())));
    } else if (Operator.AND.equals(filters.getOperator())) {
      conditions.add(
          and(
              filters.getSubfilters().stream()
                  .map(f -> whereConditionsFilter(table, tableAlias, f))
                  .collect(Collectors.toList())));
    } else {
      Column column = isValidColumn(table, filters.getColumn());
      if (filters.getSubfilters().size() > 0) {
        for (Filter subfilter : filters.getSubfilters()) {
          if (column.isReference()) {

            conditions.add(
                whereConditionsFilter(
                    column.getRefTable(), tableAlias + "-" + column.getName(), subfilter));
          } else if (column.isFile()) {
            Filter sub = filters.getSubfilter("id");
            // todo expand properly
            if (sub != null && EQUALS.equals(sub.getOperator())) {
              conditions.add(field(name(column.getName())).in(sub.getValues()));
            } else {
              throw new MolgenisException("Invalid filter for file");
            }
          }
        }
      } else {
        conditions.add(
            whereCondition(
                tableAlias,
                column.getName(),
                column.getColumnType(),
                filters.getOperator(),
                filters.getValues()));
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
      case FILE:
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
        return whereConditionArrayEquals(name, operator, toBoolArray(values));
      case UUID_ARRAY:
        return whereConditionArrayEquals(name, operator, toUuidArray(values));
      case INT_ARRAY:
        return whereConditionArrayEquals(name, operator, toIntArray(values));
      case DECIMAL_ARRAY:
        return whereConditionArrayEquals(name, operator, toDecimalArray(values));
      case DATE_ARRAY:
        return whereConditionArrayEquals(name, operator, toDateArray(values));
      case DATETIME_ARRAY:
        return whereConditionArrayEquals(name, operator, toDateTimeArray(values));
      case JSONB_ARRAY:
        return whereConditionArrayEquals(name, operator, toJsonbArray(values));
      default:
        throw new SqlQueryException(
            SqlQuery.QUERY_FAILED
                + "Filter of '"
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
          SqlQuery.QUERY_FAILED + SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, columnName);
    }
  }

  private static Condition whereConditionArrayEquals(
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
            SqlQuery.QUERY_FAILED + SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
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
              SqlQuery.QUERY_FAILED + SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
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
          if (value.length() > 2) {
            conditions.add(condition("word_similarity({0},{1}) > 0.6", value, field(columnName)));
          } else {
            conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          }
          break;
        case TEXT_SEARCH:
          // NOTE WE ONLY SEARCH ON LONGER STRINGS
          if (value.length() > 2) {
            conditions.add(
                condition(
                    "to_tsquery({0}) @@ to_tsvector({1})",
                    value.trim().replaceAll("\\s+", ":* & ") + ":*", field(columnName)));
          } else {
            conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          }
          break;
        default:
          throw new SqlQueryException(
              SqlQuery.QUERY_FAILED + SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
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
                SqlQuery.QUERY_FAILED + SqlQuery.BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
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
                SqlQuery.QUERY_FAILED + SqlQuery.BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
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
              SqlQuery.QUERY_FAILED + SqlQuery.OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE,
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
          Field<Object> field = field(name(tableAlias, searchColumnName(table.getTableName())));
          // short terms with 'like', longer with trigram
          subConditions.add(field.likeIgnoreCase("%" + subTerm + "%"));
        }
      }
      table = table.getInheritedTable();
      if (!subConditions.isEmpty()) {
        searchConditions.add(and(subConditions));
      }
    }
    return searchConditions.isEmpty() ? null : or(searchConditions);
  }

  private static SelectJoinStep<org.jooq.Record> limitOffsetOrderBy(
      SelectColumn select, SelectConnectByStep<org.jooq.Record> query) {
    query = orderBy(select, (SelectJoinStep) query);
    if (select.getLimit() > 0) query = (SelectConditionStep) query.limit(select.getLimit());
    if (select.getOffset() > 0) query = (SelectConditionStep) query.offset(select.getOffset());
    return (SelectJoinStep<org.jooq.Record>) query;
  }

  private static SelectJoinStep<org.jooq.Record> orderBy(
      SelectColumn select, SelectJoinStep<org.jooq.Record> query) {
    for (Map.Entry<String, Order> col : select.getOrderBy().entrySet()) {
      if (ASC.equals(col.getValue())) {
        query = (SelectJoinStep<org.jooq.Record>) query.orderBy(field(name(col.getKey())).asc());
      } else {
        query = (SelectJoinStep<org.jooq.Record>) query.orderBy(field(name(col.getKey())).desc());
      }
    }
    return query;
  }

  private static Column isValidColumn(TableMetadata table, String columnName) {
    // is search?
    if (TEXT_SEARCH_COLUMN_NAME.equals(columnName)) {
      return new Column(table, searchColumnName(table.getTableName()));
    }
    // is scalar column
    Column column = table.getColumn(columnName);
    if (column == null) {
      // is reference?
      for (Column c : table.getColumns()) {
        for (Reference ref : c.getReferences()) {
          // can also request composite reference columns, can only be used on row level queries
          if (ref.getName().equals(columnName)) {
            return new Column(table, columnName, true).setType(ref.getPrimitiveType());
          }
        }
      }
      // is file?
      for (Column c : table.getColumns()) {
        if (c.isFile() && columnName.startsWith(c.getName())) {
          if (columnName.equals(c.getName())
              || columnName.endsWith("_mimetype")
              || columnName.endsWith("_extension")
              || columnName.endsWith("_size")
              || columnName.endsWith("_contents")) {
            return new Column(table, columnName);
          }
        }
      }
      throw new MolgenisException(
          "Query failed: Column '" + columnName + "' is unknown in table " + table.getTableName());
    }
    return column;
  }
}
