package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;
import static org.molgenis.emx2.utils.TypeUtils.*;

import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlQuery extends QueryBean {
  public static int AGGREGATE_COUNT_THRESHOLD = Integer.MIN_VALUE; // threshold disabled by default
  public static final String COUNT_FIELD = "count";
  public static final String EXISTS_FIELD = "exists";
  public static final String MAX_FIELD = "max";
  public static final String MIN_FIELD = "min";
  public static final String AVG_FIELD = "avg";
  public static final String SUM_FIELD = "_sum";
  public static final String UNNEST_0 = "UNNEST({0})";

  private static final String QUERY_FAILED = "Query failed: ";
  private static final String ANY_SQL = "{0} = ANY ({1})";
  private static final String JSON_AGG_SQL = "jsonb_agg(item)";
  private static final String ROW_TO_JSON_SQL = "to_jsonb(item)";
  private static final String ITEM = "item";
  private static final String BETWEEN_ERROR_MESSAGE =
      "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: %s";

  private static final Logger logger = LoggerFactory.getLogger(SqlQuery.class);

  private final SqlSchemaMetadata schema;
  private final List<String> tableAliasList = new LinkedList<>();

  public SqlQuery(SqlSchemaMetadata schema, String field) {
    super(field);
    this.schema = schema;
  }

  public SqlQuery(SqlSchemaMetadata schema, String field, SelectColumn[] selection) {
    super(field);
    this.schema = schema;
    this.select(selection);
  }

  /** Create alias that is short enough for postgresql to not complain */
  public String alias(String label) {
    if (!label.contains("-")) {
      // we only need aliases for subquery tables
      return label;
    }
    if (!tableAliasList.contains(label)) {
      tableAliasList.add(label);
    }
    return "a" + tableAliasList.indexOf(label);
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
    checkHasViewPermission(table);
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
            .from(tableWithInheritanceJoin(table).as(alias(tableAlias)));

    // joins, only filtered tables
    from = refJoins(table, tableAlias, from, filter, select, new ArrayList<>());

    // where
    Condition condition = rowConditions(table, tableAlias, filter, searchTerms);
    SelectConnectByStep<org.jooq.Record> where = condition != null ? from.where(condition) : from;
    SelectConnectByStep<org.jooq.Record> query =
        applyLimitOffsetOrderBy(table, select, where, tableAlias);

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
    } catch (Exception e) {
      throw new SqlMolgenisException(QUERY_FAILED, e);
    }
  }

  @Override
  public String retrieveJSON() {
    SelectColumn select = getSelect();
    List<Field<?>> fields = new ArrayList<>();
    DSLContext sql = schema.getJooq();

    // get the table from root select
    SqlTableMetadata table = schema.getTableMetadata(select.getColumn());
    if (table == null && (select.getColumn().endsWith("_agg"))) {
      table =
          schema.getTableMetadata(select.getColumn().substring(0, select.getColumn().length() - 4));
    } else if (table == null && (select.getColumn().endsWith("_groupBy"))) {
      table =
          schema.getTableMetadata(select.getColumn().substring(0, select.getColumn().length() - 8));
    }
    if (table == null) {
      throw new MolgenisException(
          "RetrieveJSON failed: Field "
              + select.getColumn()
              + " unknown for JSON queries in schema "
              + schema.getName());
    }
    String tableAlias = "gql_" + table.getTableName();
    if (select.getColumn().endsWith("_agg")) {
      fields.add(
          jsonAggregateSelect(table, null, tableAlias, select, getFilter(), getSearchTerms())
              .as(convertToPascalCase(select.getColumn())));
    } else if (select.getColumn().endsWith("_groupBy")) {
      fields.add(
          jsonGroupBySelect(table, null, tableAlias, select, getFilter(), getSearchTerms())
              .as(convertToPascalCase(select.getColumn())));
    } else {
      // select all on root level as default
      if (select.getSubselect().isEmpty()) {
        for (Column c : table.getColumns()) {
          if (!c.isHeading()) {
            select.select(c.getName());
          }
        }
      }
      fields.add(
          jsonSubselect(table, null, tableAlias, select, getFilter(), getSearchTerms())
              .as(name(convertToPascalCase(select.getColumn()))));
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

  private List<Field<?>> rowSelectFields(
      TableMetadata table, String tableAlias, String prefix, SelectColumn selection) {

    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn select : selection.getSubselect()) {
      Column column = getColumnByName(table, select.getColumn());
      String columnAlias = prefix.equals("") ? column.getName() : prefix + "-" + column.getName();
      if (column.isFile()) {
        // check what they want to get, contents, mimetype, size, filename and/or extension
        if (select.getSubselect().isEmpty() || select.has("id")) {
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
        if (select.has("filename")) {
          fields.add(field(name(column.getName() + "_filename")));
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
            field("array({0})", rowRefbackSubselect(column, tableAlias)).as(column.getName()));
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
          fields.add(
              field(name(alias(tableAlias), column.getName()), ref.getJooqType()).as(columnAlias));
        }
      } else if (!column.isHeading()) {
        fields.add(
            field(name(alias(tableAlias), column.getName()), column.getJooqType()).as(columnAlias));
      }
    }
    return fields;
  }

  private SelectConditionStep<org.jooq.Record> rowRefbackSubselect(
      Column column, String tableAlias) {
    Column refBack = column.getRefBackColumn();
    List<Condition> where = new ArrayList<>();

    // might be composite
    for (Reference ref : refBack.getReferences()) {
      if (refBack.isRef()) {
        where.add(
            field(name(refBack.getTable().getTableName(), ref.getName()))
                .eq(field(name(alias(tableAlias), ref.getRefTo()))));
      } else if (refBack.isRefArray()) {
        where.add(
            condition(
                ANY_SQL,
                field(name(alias(tableAlias), ref.getRefTo())),
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

  private Field<?> jsonSubselect(
      SqlTableMetadata table,
      Column parentColumn,
      String tableAlias,
      SelectColumn select,
      Filter filters,
      String[] searchTerms) {
    checkHasViewPermission(table);
    String subAlias = tableAlias + (parentColumn != null ? "-" + parentColumn.getName() : "");
    Collection<Field<?>> selection = jsonSubselectFields(table, subAlias, select);
    return jsonField(
        table, parentColumn, tableAlias, select, filters, searchTerms, subAlias, selection);
  }

  private Field<?> jsonField(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      SelectColumn select,
      Filter filters,
      String[] searchTerms,
      String subAlias,
      Collection<Field<?>> selection) {
    DSLContext jooq = table.getJooq();

    // query without all nested joins for the json
    // note: another optimization would be to only include fields needed instead of asterisk
    SelectConnectByStep<org.jooq.Record> from =
        fromQuery(table, List.of(asterisk()), column, tableAlias, subAlias, filters, searchTerms);
    from = applyLimitOffsetOrderBy(table, select, from, subAlias);

    // use filtered/sorted/limited/offsetted to produce json including only the joins needed
    SelectConnectByStep<org.jooq.Record> query =
        jooq.select(selection).from(from.asTable(alias(subAlias)));

    // agg
    String agg =
        select.getColumn().endsWith("_agg")
                || select.getColumn().endsWith("_groupBy")
                || column != null && column.isRef() && !column.isArray()
            ? ROW_TO_JSON_SQL
            : JSON_AGG_SQL;

    return field(jooq.select(field(agg)).from(query.asTable(ITEM)));
  }

  private Collection<Field<?>> jsonSubselectFields(
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
          select.getColumn().endsWith("_agg") || select.getColumn().endsWith("_groupBy")
              ? getColumnByName(
                  table, select.getColumn().replace("_agg", "").replace("_groupBy", ""))
              : getColumnByName(table, select.getColumn());

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
                    select.getFilter(),
                    new String[0])
                .as(convertToCamelCase(select.getColumn())));
      } else if (column.isReference() && select.getColumn().endsWith("_groupBy")) {
        // aggregation subselect
        fields.add(
            jsonGroupBySelect(
                    (SqlTableMetadata) column.getRefTable(),
                    column,
                    tableAlias,
                    select,
                    select.getFilter(),
                    new String[0])
                .as(convertToCamelCase(select.getColumn())));
      } else if (column.isReference()) {
        // normal subselect
        fields.add(
            jsonSubselect(
                    (SqlTableMetadata) column.getRefTable(),
                    column,
                    tableAlias,
                    select,
                    select.getFilter(),
                    new String[0])
                .as(name(convertToCamelCase(select.getColumn()))));
      } else if (column.isHeading()) {
        /**
         * Ignore headings, not part of rows. Fixme: must ignore to allow JSON subqueries, but
         * unsure if this can cause any problems elsewhere.
         */
      } else if (column.getJooqType().getSQLDataType() == SQLDataType.INTERVAL) {
        fields.add(getIntervalField(tableAlias, column));
      } else {
        // primitive fields
        fields.add(
            field(name(alias(tableAlias), column.getName())).as(name(column.getIdentifier())));
      }
    }
    return fields;
  }

  private Field<?> jsonAggregateSelect(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      SelectColumn select,
      Filter filters,
      String[] searchTerms) {
    String subAlias = tableAlias + (column != null ? "-" + column.getName() : "");
    List<Field<?>> fields = new ArrayList<>();
    for (SelectColumn field : select.getSubselect()) {
      if (COUNT_FIELD.equals(field.getColumn())) {
        fields.add(getCountField().as(COUNT_FIELD));
      } else if (EXISTS_FIELD.equals(field.getColumn())) {
        if (schema.hasActiveUserRole(EXISTS.toString())) {
          fields.add(field("COUNT(*) > 0").as(EXISTS_FIELD));
        }
      } else if (List.of(MAX_FIELD, MIN_FIELD, AVG_FIELD, SUM_FIELD).contains(field.getColumn())) {
        checkHasViewPermission(table);
        List<JSONEntry<?>> result = new ArrayList<>();
        for (SelectColumn sub : field.getSubselect()) {
          Column c = getColumnByName(table, sub.getColumn());
          switch (field.getColumn()) {
            case MAX_FIELD ->
                result.add(
                    key(c.getIdentifier()).value(max(field(name(alias(subAlias), c.getName())))));
            case MIN_FIELD ->
                result.add(
                    key(c.getIdentifier()).value(min(field(name(alias(subAlias), c.getName())))));
            case AVG_FIELD ->
                result.add(
                    key(c.getIdentifier())
                        .value(avg(field(name(alias(subAlias), c.getName()), c.getJooqType()))));
            case SUM_FIELD ->
                result.add(
                    key(c.getIdentifier())
                        .value(sum(field(name(alias(subAlias), c.getName()), c.getJooqType()))));
            default ->
                throw new MolgenisException(
                    "Unknown aggregate type provided: " + field.getColumn());
          }
        }
        fields.add(jsonObject(result.toArray(new JSONEntry[result.size()])).as(field.getColumn()));
      }
    }
    return jsonField(table, column, tableAlias, select, filters, searchTerms, subAlias, fields);
  }

  private Field<Object> jsonGroupBySelect(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      SelectColumn groupBy,
      Filter filter,
      String[] searchTerms) {
    DSLContext jooq = table.getJooq();
    String subAlias = tableAlias + (column != null ? "-" + column.getName() : "");

    if (groupBy.getSubselect(COUNT_FIELD) == null && groupBy.getSubselect(SUM_FIELD) == null) {
      throw new MolgenisException("COUNt or SUM is required when using group by");
    }

    // filter conditions
    Condition condition = null;
    if (filter != null || searchTerms.length > 1) {
      condition =
          row(table.getPrimaryKeyFields())
              .in(fromQuery(table, column, tableAlias, subAlias, filter, searchTerms));
    }

    Set<Field> aggregationFields = new HashSet<>(); // sum(x), count, etc
    Set<Field> groupByFields = new HashSet<>(); // name, ref{otherName}, etc
    Set<Field> nonArraySourceFields = new HashSet<>(); // xo x, name, except those from ref_array
    List<SelectConnectByStep> refArraySubqueries = new ArrayList<>(); // for the ref_array columns

    for (SelectColumn field : groupBy.getSubselect()) {
      if (COUNT_FIELD.equals(field.getColumn())) {
        if (schema.hasActiveUserRole(VIEWER.toString())) {
          aggregationFields.add(field("COUNT(*)"));
        } else {
          aggregationFields.add(
              field("GREATEST({0},COUNT(*))", AGGREGATE_COUNT_THRESHOLD).as(COUNT_FIELD));
        }
      } else if (SUM_FIELD.equals(field.getColumn())) {
        List sumFields = new ArrayList<>();
        // sum precision depends on count
        field
            .getSubselect()
            .forEach(
                sub -> {
                  Column col = getColumnByName(table, sub.getColumn());
                  sumFields.add(
                      key(col.getIdentifier())
                          .value(
                              field(
                                  "SUM({0})",
                                  field(name(alias(subAlias), col.getName())),
                                  AGGREGATE_COUNT_THRESHOLD)));
                  nonArraySourceFields.add(col.getJooqField());
                });
        aggregationFields.add(jsonObject(sumFields).as(field.getColumn()));
      } else {
        Column col = getColumnByName(table, field.getColumn());
        if (!col.isOntology()) {
          checkHasViewPermission(table);
        }
        String subQueryAlias = tableAlias + "_" + col.getIdentifier();
        // in case of 'ref' we need a subselect
        if (col.isReference()) {
          Column copy = new Column(col.getTable(), col);
          copy.setType(ColumnType.REF); // ref_array should be treated as ref
          groupByFields.add(
              jsonSubselect(
                      (SqlTableMetadata) copy.getRefTable(),
                      copy,
                      tableAlias,
                      field,
                      field.getFilter(),
                      new String[0])
                  .as(convertToCamelCase(field.getColumn())));
        } else {
          groupByFields.add(col.getJooqField().as(convertToCamelCase(field.getColumn())));
        }

        if (col.isRef() || !col.isArray()) {
          nonArraySourceFields.addAll(col.getCompositeFields());
        } else if (col.isRefback()) {
          // convert so it looks like a ref_array
          Set<Field> subselectFields = new HashSet<>();
          if (col.getRefBackColumn().isRefArray()) {
            subselectFields.addAll(
                col.getRefBackColumn().getReferences().stream()
                    .map(ref -> field("unnest({0})", name(ref.getName())).as(ref.getRefTo()))
                    .toList());
          } else {
            subselectFields.addAll(
                col.getRefBackColumn().getReferences().stream()
                    .map(ref -> field(name(ref.getName())).as(ref.getRefTo()))
                    .toList());
          }
          subselectFields.addAll(
              col.getReferences().stream()
                  .map(ref -> field(name(ref.getRefTo())).as(ref.getName()))
                  .toList());

          refArraySubqueries.add(
              jooq.select(asterisk())
                  .from(
                      jooq.select(subselectFields)
                          .from(
                              tableWithInheritanceJoin(col.getRefTable())
                                  .as(alias(subQueryAlias)))));
        } else {
          // must be array or ref_array
          // need subquery to unnest ref_array fields
          Set<Field> subselectFields = new HashSet<>();
          subselectFields.addAll(table.getPrimaryKeyFields());
          for (Field compositeField : col.getCompositeFields()) {
            subselectFields.add(field("unnest({0})", compositeField).as(compositeField.getName()));
          }
          refArraySubqueries.add(
              jooq.select(subselectFields)
                  .from(tableWithInheritanceJoin(table).as(alias(subQueryAlias))));
        }
      }
    }

    // create source query for the sourceColumns
    nonArraySourceFields.addAll(table.getPrimaryKeyFields());
    SelectJoinStep<org.jooq.Record> sourceQuery =
        jooq.select(asterisk())
            .from(
                jooq.select(nonArraySourceFields)
                    .from(tableWithInheritanceJoin(table))
                    .where(condition));
    for (SelectConnectByStep unnestQuery : refArraySubqueries) {
      // joining on primary key in natural join
      sourceQuery = sourceQuery.naturalLeftOuterJoin(unnestQuery);
    }

    // sort by groupBy fields to make deterministic
    final List<OrderField<?>> orderByFields = new ArrayList<>();
    groupByFields.forEach(field -> orderByFields.add(field.asc().nullsLast()));

    // aggregate into one field
    List<Field> selectFields = new ArrayList<>();
    selectFields.addAll(aggregationFields);
    selectFields.addAll(groupByFields);
    return field(
            jooq.select(field(JSON_AGG_SQL))
                .from(
                    jooq.select(selectFields)
                        .from(sourceQuery.asTable(name(tableAlias)))
                        .groupBy(groupByFields)
                        .orderBy(orderByFields)
                        .asTable(ITEM)))
        .as(convertToCamelCase(groupBy.getColumn()));
  }

  private Field<Object> jsonFileField(
      SqlTableMetadata table, String tableAlias, SelectColumn select, Column column) {
    DSLContext jooq = table.getJooq();
    List<Field<?>> subFields = new ArrayList<>();
    for (String ext :
        new String[] {"id", "contents", "size", "filename", "extension", "mimetype", "url"}) {
      if (select.has(ext)) {
        if (ext.equals("id")) {
          subFields.add(field(name(alias(tableAlias), column.getName())).as(ext));
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
                      field(name(alias(tableAlias), column.getName())))
                  .as(ext));
        } else {
          subFields.add(field(name(alias(tableAlias), column.getName() + "_" + ext)).as(ext));
        }
      }
    }
    return field((jooq.select(field(ROW_TO_JSON_SQL)).from(jooq.select(subFields).asTable(ITEM))))
        .as(column.getIdentifier());
  }

  // overload for backwards compatibility with other uses of this part
  private SelectConditionStep<org.jooq.Record> fromQuery(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filters,
      String[] searchTerms) {
    return fromQuery(
        table,
        table.getPrimaryKeyFields().stream().map(f -> (SelectFieldOrAsterisk) f).toList(),
        column,
        tableAlias,
        subAlias,
        filters,
        searchTerms);
  }

  private SelectConditionStep<org.jooq.Record> fromQuery(
      SqlTableMetadata table,
      List<SelectFieldOrAsterisk> selection,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filters,
      String[] searchTerms) {

    String filterAlias = subAlias + "_filter";

    List<Condition> conditions = new ArrayList<>();
    if (filters != null) {
      conditions.addAll(
          // column should be null when nesting (is only used for refJoinCondition)
          whereTableFilters(table, null, tableAlias, filterAlias, filters, searchTerms));
    }
    if (searchTerms.length > 0) {
      conditions.add(whereTableSearch(table, filterAlias, searchTerms));
    }
    if (column != null) {
      conditions.add(refJoinCondition(column, tableAlias, filterAlias));
    }

    // create the subquery
    if (!conditions.isEmpty()) {
      return table
          .getJooq()
          .select(selection)
          .from(tableWithInheritanceJoin(table).as(alias(filterAlias)))
          .where(conditions);
    } else {
      return (SelectConditionStep<org.jooq.Record>)
          table
              .getJooq()
              .select(selection)
              .from(tableWithInheritanceJoin(table).as(alias(filterAlias)));
    }
  }

  private List<Condition> whereTableFilters(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      Filter filters,
      String[] searchTerms) {
    List<Condition> conditions = new ArrayList<>();
    if (filters != null) {
      for (Filter filter : filters.getSubfilters()) {
        if (filter == null) {
          // continue
        } else if (filter.getOperator() != null) {
          conditions.add(
              whereTableFilter(table, column, tableAlias, subAlias, searchTerms, filter));
        } else {
          // nested query
          Column c = getColumnByName(table, filter.getColumn());
          SelectSelectStep subQuery =
              (SelectSelectStep<?>)
                  fromQuery(
                      (SqlTableMetadata) c.getRefTable(),
                      column,
                      tableAlias,
                      subAlias,
                      filter,
                      new String[0]);
          conditions.add(whereColumnInSubquery(c, subQuery));
        }
      }
    }
    return conditions;
  }

  private Condition whereTableFilter(
      SqlTableMetadata table,
      Column column,
      String tableAlias,
      String subAlias,
      String[] searchTerms,
      Filter filter) {
    if (filter.getOperator() != null)
      switch (filter.getOperator()) {
        case OR:
          return or(whereTableFilters(table, column, tableAlias, subAlias, filter, searchTerms));
        case AND:
          return and(whereTableFilters(table, column, tableAlias, subAlias, filter, searchTerms));
        case TRIGRAM_SEARCH, TEXT_SEARCH:
          return whereTableSearch(table, subAlias, TypeUtils.toStringArray(filter.getValues()));
        default:
          // then it must be a column filter
          return whereColumn(
              subAlias,
              getColumnByName(table, filter.getColumn()),
              filter.getOperator(),
              filter.getValues());
      }
    throw new MolgenisException("Unkown exception");
  }

  private Condition whereTableSearch(
      SqlTableMetadata table, String subAlias, String[] searchTerms) {
    // create search
    List<Condition> searchCondition = new ArrayList<>();
    for (String term : searchTerms) {
      List<Condition> search = new ArrayList<>();
      search.add(
          field(name(alias(subAlias), searchColumnName(table.getTableName())))
              .likeIgnoreCase("%" + term + "%"));
      // also search in ontology tables linked to current table
      table.getColumns().stream()
          .filter(Column::isOntology)
          .forEach(
              ontologyColumn -> {
                Table<Record> ontologyTable = ontologyColumn.getRefTable().getJooqTable();
                if (Boolean.TRUE.equals(ontologyColumn.isArray())) {
                  // include if array overlap between ontology table and our selected values in our
                  // ref_array
                  search.add(
                      condition(
                          "{0} && ARRAY({1})",
                          ontologyColumn.getJooqField(),
                          DSL.select(field("name"))
                              .from(ontologyTable)
                              .where(
                                  field(
                                          name(
                                              ontologyTable.getName(),
                                              searchColumnName(ontologyTable.getName())))
                                      .likeIgnoreCase("%" + term + "%"))));
                } else {
                  // include if our ref is in the ontology terms list that would be found given our
                  // search terms
                  search.add(
                      ontologyColumn
                          .getJooqField()
                          .in(
                              DSL.select(field("name"))
                                  .from(ontologyTable)
                                  .where(
                                      field(
                                              name(
                                                  ontologyTable.getName(),
                                                  searchColumnName(ontologyTable.getName())))
                                          .likeIgnoreCase("%" + term + "%"))));
                }
              });

      TableMetadata parent = table.getInheritedTable();
      while (parent != null) {
        search.add(
            field(name(alias(subAlias), searchColumnName(parent.getTableName())))
                .likeIgnoreCase("%" + term + "%"));
        parent = parent.getInheritedTable();
      }
      searchCondition.add(or(search));
    }
    return and(searchCondition);
  }

  private static Table<org.jooq.Record> tableWithInheritanceJoin(TableMetadata table) {

    Table<org.jooq.Record> result = table.getJooqTable();
    TableMetadata inheritedTable = table.getInheritedTable();
    // root and intermediate levels have mg_tableclass column
    Column mg_tableclass = table.getLocalColumn(MG_TABLECLASS);
    while (inheritedTable != null) {
      List<Field<?>> using = inheritedTable.getPrimaryKeyFields();
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

  private SelectJoinStep<org.jooq.Record> refJoins(
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
          Column column = getColumnByName(table, filter.getColumn());
          if (column.isReference() && !filter.getSubfilters().isEmpty()) {
            String subAlias = tableAlias + "-" + column.getName();
            if (!aliasList.contains(subAlias)) {
              // to ensure only join once
              aliasList.add(subAlias);
              // the join
              join.leftJoin(tableWithInheritanceJoin(column.getRefTable()).as(alias(subAlias)))
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
        Column column = getColumnByName(table, select.getColumn());
        if (column.isReference()) {
          String subAlias = tableAlias + "-" + column.getName();
          // only join if subselection extists
          if (!aliasList.contains(subAlias) && !select.getSubselect().isEmpty()) {
            aliasList.add(subAlias);
            join.leftJoin(tableWithInheritanceJoin(column.getRefTable()).as(alias(subAlias)))
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

  private Condition refJoinCondition(Column column, String tableAlias, String subAlias) {
    List<Condition> foreignKeyMatch = new ArrayList<>();

    if (column.isRef()) {
      if (column.getReferences().size() == 1) {
        Reference ref = column.getReferences().get(0);
        foreignKeyMatch.add(
            field(name(alias(subAlias), ref.getRefTo()))
                .eq(field(name(alias(tableAlias), ref.getName()))));
      } else {
        foreignKeyMatch.add(
            and(
                // at least one column not null
                or(
                    column.getReferences().stream()
                        .map(ref -> field(name(alias(tableAlias), ref.getName())).isNotNull())
                        .toList()),
                // and matches on values or nulls
                and(
                    column.getReferences().stream()
                        .map(
                            ref ->
                                field(name(alias(subAlias), ref.getRefTo()))
                                    .eq(field(name(alias(tableAlias), ref.getName()))))
                        .toList())));
      }
    } else if (column.isRefArray()) {
      if (column.getReferences().size() == 1) {
        Reference ref = column.getReferences().get(0);
        // simple array comparison
        foreignKeyMatch.add(
            condition(
                ANY_SQL,
                name(alias(subAlias), ref.getRefTo()),
                name(alias(tableAlias), ref.getName())));
      } else {
        // expensive 'in' query to enable join on all fields
        List<Field<Object>> to =
            column.getReferences().stream()
                .map(ref -> field(name(alias(subAlias), ref.getRefTo()).toString()))
                .toList();

        List<Field<Object>> unnest =
            column.getReferences().stream()
                .map(
                    r ->
                        r.isOverlappingRef()
                            ? field(name(alias(tableAlias), r.getName()))
                            : field(UNNEST_0, name(alias(tableAlias), r.getName())))
                .toList();
        foreignKeyMatch.add(row(to).in(DSL.select(unnest)));
      }
    } else if (column.isRefback()) {
      Column refBack = column.getRefBackColumn();
      if (refBack.isRef()) {
        foreignKeyMatch.addAll(
            refBack.getReferences().stream()
                .map(
                    ref ->
                        field(name(alias(subAlias), ref.getName()))
                            .eq(field(name(alias(tableAlias), ref.getRefTo()))))
                .toList());
      } else if (refBack.isRefArray()) {
        foreignKeyMatch.addAll(
            refBack.getReferences().stream()
                .map(
                    ref ->
                        ref.isOverlappingRef()
                            ? field(name(alias(tableAlias), ref.getRefTo()))
                                .eq(field(name(alias(subAlias), ref.getName())))
                            : condition(
                                ANY_SQL,
                                field(name(alias(tableAlias), ref.getRefTo())),
                                field(name(alias(subAlias), ref.getName()))))
                .toList());
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

  private Condition rowConditions(
      TableMetadata table, String tableAlias, Filter filter, String[] searchTerms) {
    Condition searchCondition = rowConditionsSearch(table, tableAlias, searchTerms);
    Condition filterCondition = rowConditionsFilter(table, tableAlias, filter);

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

  private Condition rowConditionsFilter(TableMetadata table, String tableAlias, Filter filters) {
    List<Condition> conditions = new ArrayList<>();
    if (Operator.OR.equals(filters.getOperator())) {
      conditions.add(
          or(
              filters.getSubfilters().stream()
                  .map(f -> rowConditionsFilter(table, tableAlias, f))
                  .toList()));
    } else if (Operator.AND.equals(filters.getOperator())) {
      conditions.add(
          and(
              filters.getSubfilters().stream()
                  .map(f -> rowConditionsFilter(table, tableAlias, f))
                  .toList()));
    } else {
      Column column =
          getColumnByName(table, filters.getColumn(), filters.getSubfilters().isEmpty());
      if (column.isReference()
          && column.getReferences().size() > 1
          && filters.getSubfilters().isEmpty()) {
        throw new MolgenisException(
            "Filter of '"
                + column.getName()
                + " not supported for compound key, use individual elements or nested filters.");
      }
      if (!filters.getSubfilters().isEmpty()) {
        for (Filter subfilter : filters.getSubfilters()) {
          if (column.isReference()) {
            conditions.add(
                rowConditionsFilter(
                    column.getRefTable(), tableAlias + "-" + column.getName(), subfilter));
          } else if (column.isFile()) {
            Filter sub = filters.getSubfilter("id");
            if (sub != null && EQUALS.equals(sub.getOperator())) {
              conditions.add(field(name(column.getName())).in(sub.getValues()));
            } else {
              throw new MolgenisException("Invalid filter for file");
            }
          }
        }
      } else {
        conditions.add(whereColumn(tableAlias, column, filters.getOperator(), filters.getValues()));
      }
    }
    return conditions.isEmpty() ? null : and(conditions);
  }

  /** Wrapper around all column conditions */
  private Condition whereColumn(
      String tableAlias, Column column, org.molgenis.emx2.Operator operator, Object[] values) {
    Name columnName = name(alias(tableAlias), column.getName());
    ColumnType columnType = column.getColumnType();
    if (!List.of(columnType.getOperators()).contains(operator)) {
      throw new MolgenisException(
          "Operator="
              + operator
              + " not supported for columnType="
              + columnType
              + ". Condition: "
              + tableAlias
              + "."
              + column.getName()
              + " "
              + operator
              + " "
              + values);
    }
    // cast value to match the column type
    if ((values[0] instanceof IsNullOrNotNull) || columnType.isReference()) {
      // type casting is handled below
    } else if (columnType.isFile()) {
      values = toStringArray(values);
    } else if (ColumnType.JSON.equals(columnType)) {
      values = toJsonbArray(values); // doesn't have a array type
    } else if (values.length > 0) {
      values =
          (Object[])
              (columnType.isArray()
                  ? getTypedValue(values, columnType)
                  : getTypedValue(values, getArrayType(columnType)));
    }
    switch (operator) {
      case CONTAINS_ANY, EQUALS: // equals to be deprecated for ref columns,
        return whereContainsAnyOrEquals(tableAlias, columnName, column, values);
      case CONTAINS_NONE, NOT_EQUALS: // non_equals to be deprecated for ref columns,
        return not(whereContainsAnyOrEquals(tableAlias, columnName, column, values));
      case CONTAINS_ALL:
        return whereColumnContainsAll(tableAlias, columnName, column, values);
      case IS:
        return whereColumnIsNullOrNotNull(tableAlias, columnName, column, values);
      case LIKE:
        return whereColumnLike(columnName, columnType.isArray(), values);
      case NOT_LIKE:
        return not(whereColumnLike(columnName, columnType.isArray(), values));
      case TRIGRAM_SEARCH:
        return whereColumnTrigramSearch(columnName, columnType.isArray(), values);
      case TEXT_SEARCH:
        return whereColumnTextSearch(columnName, columnType.isArray(), (String[]) values);
      case NOT_BETWEEN:
        return not(whereColumnBetween(columnName, values));
      case BETWEEN:
        return whereColumnBetween(columnName, values);
      case MATCH_INCLUDING_PARENTS:
        return whereColumnInSubquery(
            column,
            DSL.select(
                field(
                    "\"MOLGENIS\".get_terms_including_parents({0},{1},{2})",
                    column.getRefTable().getSchemaName(),
                    column.getRefTable().getTableName(),
                    TypeUtils.toStringArray(values))));
      case MATCH_INCLUDING_CHILDREN:
        return whereColumnInSubquery(
            column,
            DSL.select(
                field(
                    "\"MOLGENIS\".get_terms_including_children({0},{1},{2})",
                    column.getRefTable().getSchemaName(),
                    column.getRefTable().getTableName(),
                    TypeUtils.toStringArray(values))));
      default:
        throw new MolgenisException("Unknown operator: " + operator);
    }
  }

  private static @NotNull Condition whereColumnTextSearch(
      Name columnName, boolean isArray, String[] values) {
    if (isArray) {
      return or(
          Arrays.stream(values)
              .map(
                  value ->
                      condition(
                          "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE to_tsquery({0}) @@ to_tsvector(v)",
                          value.trim().replaceAll("\\s+", ":* & ") + ":*", field(columnName)))
              .toList());
    } else {
      // NOTE WE ONLY SEARCH ON LONGER STRINGS
      return or(
          Arrays.stream(values)
              .map(
                  value ->
                      value.length() > 2
                          ? condition(
                              "to_tsquery({0}) @@ to_tsvector({1})",
                              value.trim().replaceAll("\\s+", ":* & ") + ":*", field(columnName))
                          : field(columnName).likeIgnoreCase("%" + value + "%"))
              .toList());
    }
  }

  private static @NotNull Condition whereColumnTrigramSearch(
      Name columnName, boolean isArray, Object[] values) {
    if (isArray) {
      return or(
          Arrays.stream(values)
              .map(
                  value ->
                      condition(
                          "0 < ( SELECT COUNT(*) FROM unnest({1}) AS v WHERE word_similarity({0},v) > 0.6",
                          value, field(columnName)))
              .toList());
    } else {
      return or(
          Arrays.stream(values)
              .map(
                  value ->
                      ((String) value).length() > 2
                          ? condition("word_similarity({0},{1}) > 0.6", value, field(columnName))
                          : field(columnName).likeIgnoreCase("%" + value + "%"))
              .toList());
    }
  }

  private static @Nullable Condition whereColumnIsNullOrNotNull(
      String tableAlias, Name columnName, Column columnMetadata, Object[] values) {
    ColumnType type = columnMetadata.getColumnType().getBaseType();
    if (type.isRefback()) {
      // check if any reference (not)exist
    } else if (type.isArray()) {
      String sqlTemplate =
          IsNullOrNotNull.NULL.equals(values[0])
              ? "({0} IS NULL OR {0} = '{}')"
              : "({0} IS NOT NULL AND {0} <> '{}')";
      if (type.isReference() && columnMetadata.getReferences().size() > 1) {
        return and(
            columnMetadata.getReferences().stream()
                .map(ref -> condition(sqlTemplate, field(name(tableAlias, ref.getName()))))
                .toList());
      } else {
        return condition(sqlTemplate, field(columnName));
      }
    } else {
      if (IsNullOrNotNull.NULL.equals(values[0])) {
        return field(columnName).isNull();
      } else {
        return field(columnName).isNotNull();
      }
    }
    return null;
  }

  private static @NotNull Condition whereColumnLike(
      Name columnName, boolean isArray, Object[] values) {
    if (isArray) {
      return or(
          Arrays.stream(values)
              .map(value -> condition("{0} ILIKE ANY({1})", "%" + value + "%", field(columnName)))
              .toList());
    } else {
      return or(
          Arrays.stream(values)
              .map(value -> field(columnName).likeIgnoreCase("%" + value + "%"))
              .toList());
    }
  }

  private static @NotNull Condition whereContainsAnyOrEquals(
      String tableAlias, Name columnName, Column columnDefinition, Object[] values) {
    ColumnType type = columnDefinition.getColumnType().getBaseType();
    if (type.isRef()) {
      if (columnDefinition.getReferences().size() == 1) {
        return columnDefinition.getJooqField().in(values);
      } else {
        Table<Record> compositeKeyValuesTempTable =
            getCompositeKeyValuesAsTempTable(tableAlias, columnDefinition, values);
        return row(columnDefinition.getReferences().stream()
                .map(ref -> field(name(tableAlias, ref.getName())))
                .toList())
            .in(selectFrom(compositeKeyValuesTempTable));
      }
    } else if (type.isRefArray()) {
      if (columnDefinition.getReferences().size() == 1) {
        return condition(
            "{0} && {1}",
            field(columnName), getTypedValue(values, columnDefinition.getPrimitiveColumnType()));
      } else {
        // when size == 1 then use same as array below
        return exists(
            selectFrom(getUnnestedRefArrayAsTable(tableAlias, columnDefinition))
                .where(
                    row(getUnnestedRefArrayFields(columnDefinition))
                        .in(
                            selectFrom(
                                getCompositeKeyValuesAsTempTable(
                                    tableAlias, columnDefinition, values)))));
      }
    } else if (type.isRefback()) {
      if (columnDefinition.getReferences().size() == 1) {
        return row(columnDefinition.getTable().getPrimaryKeyFields())
            .in(
                getRefBackSelect(columnDefinition)
                    .where(
                        condition(
                            ANY_SQL,
                            field(name(columnDefinition.getRefTable().getPrimaryKeys().get(0))),
                            getTypedValue(values, columnDefinition.getPrimitiveColumnType()))));
      } else {
        return row(columnDefinition.getTable().getPrimaryKeyFields())
            .in(
                getRefBackSelect(columnDefinition)
                    .where(
                        row(columnDefinition.getRefBackColumn().getTable().getPrimaryKeyFields())
                            .in(
                                selectFrom(
                                    getCompositeKeyValuesAsTempTable(
                                        tableAlias, columnDefinition, values)))));
      }
    } else if (type.isArray()) {
      return condition("{0} && {1}", field(columnName), values);
    } else {
      return condition(ANY_SQL, field(columnName), values);
    }
  }

  private static @NotNull Condition whereColumnContainsAll(
      String tableAlias, Name columnName, Column columnMetadata, Object[] values) {
    ColumnType type = columnMetadata.getColumnType().getBaseType();
    if (type.isRefArray()) {
      if (columnMetadata.getReferences().size() == 1) {
        return condition("{0} <@ {1}", values, field(columnName));
      } else {
        List<Field> compositeKeyFields =
            columnMetadata.getReferences().stream()
                .map(
                    ref -> field(name(ref.getTargetColumn()), ref.getJooqType().getArrayBaseType()))
                .toList();
        return notExists(
            selectOne()
                .from(getCompositeKeyValuesAsTempTable(tableAlias, columnMetadata, values))
                .where(
                    row(compositeKeyFields)
                        .notIn(
                            DSL.select(getUnnestedRefArrayFields(columnMetadata))
                                .from(getUnnestedRefArrayAsTable(tableAlias, columnMetadata)))));
      }
    } else if (type.isRefback()) {
      // complex, both the refback as well as id of target can have multiple columns
      // moreover the ref could be a single value or an array
      // first simplest implementation is to convert to temp table in all cases
      // might not be efficient
      // so select from refTable where values in compositeKeyTable and then check there is none that
      Column refBack = columnMetadata.getRefBackColumn();
      Condition refBackFilter =
          refBack.isRef()
              ?
              // refback is simple ref so just equals on all columns
              and(
                  refBack.getReferences().stream()
                      .map(
                          ref ->
                              field(name(ref.getName()))
                                  .eq(field(name(tableAlias, ref.getTargetColumn()))))
                      .toList())
              // refBack is an array so unnest
              : row(refBack.getReferences().stream()
                      .map(ref -> field(name(tableAlias, ref.getTargetColumn())))
                      .toList())
                  .in(
                      DSL.select(getUnnestedRefArrayFields(refBack))
                          .from(getUnnestedRefArrayAsTable(tableAlias, refBack)));

      SelectConditionStep<Record> refBackSelect =
          DSL.select(
                  refBack.getTable().getPrimaryKeyColumns().stream()
                      .map(
                          ref ->
                              ref.isArray()
                                  ? field(UNNEST_0, name(ref.getName())).as(name(ref.getName()))
                                  : field(name(ref.getName())))
                      .toList())
              .from(refBack.getJooqTable())
              .where(refBackFilter);

      if (columnMetadata.getReferences().size() == 1) {
        // will be value array
        return condition(
            "{0} <@ {1}",
            getTypedValue(values, columnMetadata.getPrimitiveColumnType()), array(refBackSelect));
      } else {
        // will be rows in compsite key values temp table
        List<Field> compositeKeyFields =
            columnMetadata.getReferences().stream()
                .map(
                    ref -> field(name(ref.getTargetColumn()), ref.getJooqType().getArrayBaseType()))
                .toList();
        return notExists(
            selectOne()
                .from(getCompositeKeyValuesAsTempTable(tableAlias, columnMetadata, values))
                .where(row(compositeKeyFields).notIn(refBackSelect)));
      }
    } else if (type.isArray()) {
      return condition("{0} <@ {1}", values, field(columnName));
    } else {
      throw new MolgenisException("Can apply CONTAINS_ALL only to array columns");
    }
  }

  private static Condition whereColumnBetween(Name columnName, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
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
    }
    return or(conditions);
  }

  private Condition rowConditionsSearch(
      TableMetadata table, String tableAlias, String[] searchTerms) {
    List<Condition> searchConditions = new ArrayList<>();
    while (table != null) {
      List<Condition> subConditions = new ArrayList<>();
      // will get inherit tables too
      for (String term : searchTerms) {
        for (String subTerm : term.split(" ")) {
          subTerm = subTerm.trim();
          Field<Object> field =
              field(name(alias(tableAlias), searchColumnName(table.getTableName())));
          // short terms with 'like', longer with trigram
          subConditions.add(field.likeIgnoreCase("%" + subTerm + "%"));
        }
      }
      table = table.getInheritedTable();
      if (!subConditions.isEmpty()) {
        searchConditions.add(and(subConditions));
      }
    }
    return searchConditions.isEmpty() ? null : and(searchConditions);
  }

  private Condition whereColumnInSubquery(Column c, SelectSelectStep subQuery) {
    if (c.isRefArray()) {
      // if not composite it is simple array overlap
      if (c.getReferences().size() == 1) {
        return condition("{0} && ARRAY({1})", name(c.getName()), subQuery);
      } else {
        // otherwise exists(unnest(ref_array) natural join (filterQuery))
        List<Field<?>> unnest =
            c.getReferences().stream()
                .map(
                    ref ->
                        ref.isOverlappingRef()
                            ? field(name(ref.getName())).as(name(ref.getRefTo()))
                            : field(UNNEST_0, name(ref.getName())).as(name(ref.getRefTo())))
                .collect(Collectors.toCollection(ArrayList::new));

        return exists(selectFrom(DSL.select(unnest).asTable().naturalJoin(subQuery)));
      }
    } else if (c.isRefback()) {
      Column refBack = c.getRefBackColumn();
      List<Field<?>> pkey = c.getTable().getPrimaryKeyFields().stream().toList();
      List<Field> backRef =
          c.getRefBackColumn().getReferences().stream().map(Reference::getJooqField).toList();
      List<Field<?>> backRefKey =
          c.getRefBackColumn().getTable().getPrimaryKeyFields().stream().toList();
      // can be ref, ref_array (mref is checked above)
      if (refBack.isRef()) {
        // pkey in (backref from refBack table where backrefKey in subquery)
        return row(pkey)
            .in(
                DSL.select(backRef)
                    .from(c.getRefTable().getJooqTable())
                    .where(row(backRefKey).in(subQuery)));
      } else {
        // ref_array
        // pkey in (unnest(backref) from mappedByTable where backrefKey in subquery)
        return row(pkey)
            .in(
                DSL.select(
                        c.getRefBackColumn().getReferences().stream()
                            .map(
                                bref ->
                                    bref.isOverlappingRef()
                                        ? field(name(bref.getName()))
                                        : field(UNNEST_0, name(bref.getName()))
                                            .as(name(bref.getName())))
                            .toList())
                    .from(c.getRefTable().getJooqTable())
                    .where(row(backRefKey).in(subQuery)));
      }
    } else {
      // normal ref
      List<Field> refs = c.getReferences().stream().map(Reference::getJooqField).toList();
      return row(refs).in(subQuery);
    }
  }

  private static SelectJoinStep<org.jooq.Record> applyLimitOffsetOrderBy(
      TableMetadata table,
      SelectColumn select,
      SelectConnectByStep<org.jooq.Record> query,
      String tableAlias) {
    query = SqlQueryBuilderHelpers.orderBy(table, select, query, tableAlias);
    if (select.getLimit() > 0) {
      query = (SelectConditionStep) query.limit(select.getLimit());
    }
    if (select.getOffset() > 0) {
      query = (SelectConditionStep) query.offset(select.getOffset());
    }
    return (SelectJoinStep<org.jooq.Record>) query;
  }

  private static @NotNull SelectJoinStep<Record> getRefBackSelect(Column column) {
    SelectJoinStep<Record> refBackSelect =
        DSL.select(
                column.getRefBackColumn().getReferences().stream()
                    .map(
                        bref ->
                            bref.isArray()
                                ? field(UNNEST_0, name(bref.getName())).as(name(bref.getName()))
                                : field(name(bref.getName())))
                    .toList())
            .from(column.getRefTable().getJooqTable());
    return refBackSelect;
  }

  private static Table<Record> getUnnestedRefArrayAsTable(String subAlias, Column contains_column) {
    Table<Record> unnestedRefArrayAsTable =
        contains_column.getReferences().stream()
            .map(
                ref ->
                    unnest(field(name(ref.getName()), ref.getJooqType()))
                        .withOrdinality()
                        .as(
                            subAlias + "_" + ref.getName() + "_unnested",
                            "unnested_" + ref.getName(),
                            "ordinality"))
            .reduce((table1, table2) -> table1.naturalJoin(table2))
            .orElseThrow(() -> new IllegalStateException("No references available"));
    return unnestedRefArrayAsTable;
  }

  private static @NotNull List<Field> getUnnestedRefArrayFields(Column contains_column) {
    List<Field> unnestedRefArrayFields =
        contains_column.getReferences().stream()
            .map(
                ref ->
                    field(name("unnested_" + ref.getName()), ref.getJooqType().getArrayBaseType()))
            .toList();
    return unnestedRefArrayFields;
  }

  private static @NotNull Table<Record> getCompositeKeyValuesAsTempTable(
      String subAlias, Column refColumn, Object[] values) {
    return DSL.values(
            Arrays.stream(values)
                .map(
                    value ->
                        row(
                            refColumn.getReferences().stream()
                                .map(
                                    ref ->
                                        val(
                                            ((Row) value)
                                                .get(
                                                    ref.getTargetColumn(),
                                                    refColumn
                                                        .getRefTable()
                                                        .getColumn(ref.getTargetColumn())
                                                        .getPrimitiveColumnType()),
                                            ref.getJooqType().getArrayBaseDataType()))
                                .toArray(Field[]::new)))
                .toArray(RowN[]::new))
        .as(
            subAlias + "_" + refColumn.getName() + "_values",
            refColumn.getReferences().stream()
                .map(ref -> ref.getTargetColumn())
                .toArray(String[]::new));
  }

  private void checkHasViewPermission(SqlTableMetadata table) {
    if (!table.getTableType().equals(TableType.ONTOLOGIES)
        && !schema.getInheritedRolesForActiveUser().contains(VIEWER.toString())) {
      throw new MolgenisException("Cannot retrieve rows: requires VIEWER permission");
    }
  }

  private Field<String> getIntervalField(String tableAlias, Column column) {
    Field<?> intervalField = field(name(alias(tableAlias), column.getName()));
    Field<String> functionCallField =
        function("\"MOLGENIS\".interval_to_iso8601", String.class, intervalField);
    return functionCallField.as(name(column.getIdentifier()));
  }

  private Field<Integer> getCountField() {
    if (schema.hasActiveUserRole(COUNT.toString())) {
      return count();
    } else if (schema.hasActiveUserRole(AGGREGATOR.toString())) {
      return field("GREATEST(COUNT(*),{0})", Integer.class, 10L);
    } else if (schema.hasActiveUserRole(RANGE.toString())) {
      return field("CEIL(COUNT(*)::numeric / {0}) * {0}", Integer.class, 10L);
    }
    throw new MolgenisException("Need permission >= RANGE to perform count queries");
  }

  private static Column getColumnByName(TableMetadata table, String columnName) {
    return getColumnByName(table, columnName, false);
  }

  private static Column getColumnByName(
      TableMetadata table, String columnName, boolean isRowQuery) {
    // is search?
    if (TEXT_SEARCH_COLUMN_NAME.equals(columnName)) {
      return new Column(table, searchColumnName(table.getTableName()));
    }
    // is scalar column
    Column column = table.getColumn(columnName);
    if (column == null || (isRowQuery && column.isReference())) {
      // is reference?
      for (Column c : table.getColumns()) {
        if (c.isReference()) {
          for (Reference ref : c.getReferences()) {
            // can also request composite reference columns, can only be used on row level queries
            if (ref.getName().equals(columnName)) {
              return new Column(table, columnName, true).setType(ref.getPrimitiveType());
            }
          }
        }
      }
      // is file?
      for (Column c : table.getColumns()) {
        if (c.isFile()
            && columnName.startsWith(c.getName())
            && (columnName.equals(c.getName())
                || columnName.endsWith("_mimetype")
                || columnName.endsWith("_filename")
                || columnName.endsWith("_extension")
                || columnName.endsWith("_size")
                || columnName.endsWith("_contents"))) {
          return new Column(table, columnName);
        }
      }
      throw new MolgenisException(
          "Query failed: Column '" + columnName + "' is unknown in table " + table.getTableName());
    }
    return column;
  }
}
