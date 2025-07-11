package org.molgenis.emx2.graphql;

import static graphql.scalars.ExtendedScalars.GraphQLLong;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.IS_NULL;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.TableType.ONTOLOGIES;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.transform;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlCustomTypes.GraphQLJsonAsString;
import static org.molgenis.emx2.sql.SqlQuery.*;
import static org.molgenis.emx2.utils.TypeUtils.convertToPrimaryKeyRows;

import graphql.Scalars;
import graphql.schema.*;
import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class GraphqlTableFieldFactory {
  // static types
  private static final GraphQLEnumType orderByEnum =
      GraphQLEnumType.newEnum()
          .name("MolgenisOrderByEnum")
          .value(Order.ASC.name(), Order.ASC)
          .value(Order.DESC.name(), Order.DESC)
          .build();
  private static GraphQLObjectType fileDownload =
      GraphQLObjectType.newObject()
          .name("MolgenisFileDownload")
          .field(GraphQLFieldDefinition.newFieldDefinition().name("id").type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name("size").type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("filename")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("extension")
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name("url").type(Scalars.GraphQLString))
          .build();
  final List<String> agg_fields = List.of("max", "min", SUM_FIELD, "avg");
  private final Schema schema;

  // cache so we can reuse types between tables
  private Map<ColumnType, GraphQLInputObjectType> columnFilterInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedOutputType> tableTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedOutputType> tableAggTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedOutputType> tableGroupByTypes = new LinkedHashMap();
  private Map<String, GraphQLNamedInputType> tableFilterInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedInputType> tableOrderByInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedInputType> rowInputTypes = new LinkedHashMap<>();
  private Map<String, GraphQLNamedInputType> refTypes = new LinkedHashMap<>();

  public GraphqlTableFieldFactory(Schema schema) {
    this.schema = schema;
  }

  // helper to generate globally unique identifiers
  private String getTableTypeIdentifier(TableMetadata table) {
    if (table.getSchemaName().equals(schema.getName())) {
      // local types we keep as was before
      return table.getIdentifier();
    } else {
      // refschema types we prefix with schema
      return table.getSchema().getIdentifier().replace("-", "") + "_" + table.getIdentifier();
    }
  }

  // schema specific types
  public GraphQLFieldDefinition tableQueryField(TableMetadata table) {
    GraphQLNamedOutputType tableType = createTableObjectType(table);
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getIdentifier())
        .type(GraphQLList.list(tableType))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputType(table))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.LIMIT)
                .type(Scalars.GraphQLInt)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.OFFSET)
                .type(Scalars.GraphQLInt)
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.ORDERBY)
                .type(createTableOrderByInputType(table))
                .build())
        .build();
  }

  public GraphQLFieldDefinition tableGroupByField(TableMetadata table) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getIdentifier() + "_groupBy")
        .type(GraphQLList.list(createTableGroupByType(table)))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputType(table))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .build();
  }

  public GraphQLFieldDefinition tableAggField(TableMetadata table) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getIdentifier() + "_agg")
        .type(createTableAggregationType(table))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputType(table))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .build();
  }

  private GraphQLNamedOutputType createTableObjectType(TableMetadata table) {
    String tableObjectType = getTableTypeIdentifier(table);
    if (!tableTypes.containsKey(tableObjectType)) {
      // put reference in case of cyclic references
      tableTypes.put(tableObjectType, GraphQLTypeReference.typeRef(tableObjectType));
      // build the object
      GraphQLObjectType.Builder tableBuilder = GraphQLObjectType.newObject().name(tableObjectType);
      for (Column col : table.getColumnsIncludingSubclassesExcludingHeadings()) {
        createTableField(col, tableBuilder);
      }
      tableTypes.put(tableObjectType, tableBuilder.build());
    }
    return tableTypes.get(tableObjectType);
  }

  private void createTableField(Column col, GraphQLObjectType.Builder tableBuilder) {
    String id = col.getIdentifier();
    TableMetadata table = col.getTable();
    switch (col.getColumnType().getBaseType()) {
      case HEADING:
        // nothing to do
        break;
      case FILE:
        tableBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name(id).type(fileDownload));
        break;
      case BOOL:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLBoolean));
        break;
      case BOOL_ARRAY:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id)
                .type(GraphQLList.list(Scalars.GraphQLBoolean)));
        break;
      case STRING:
      case TEXT:
      case LONG:
      case UUID:
      case DATE:
      case DATETIME:
      case PERIOD:
      case EMAIL:
      case HYPERLINK:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLString));
        break;
      case JSON:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(id).type(GraphQLJsonAsString));
        break;
      case STRING_ARRAY:
      case EMAIL_ARRAY:
      case HYPERLINK_ARRAY:
      case TEXT_ARRAY:
      case LONG_ARRAY:
      case DATE_ARRAY:
      case DATETIME_ARRAY:
      case PERIOD_ARRAY:
      case UUID_ARRAY:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id)
                .type(GraphQLList.list(Scalars.GraphQLString)));
        break;
      case DECIMAL:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLFloat));
        break;
      case DECIMAL_ARRAY:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id)
                .type(GraphQLList.list(Scalars.GraphQLFloat)));
        break;
      case INT:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(id).type(Scalars.GraphQLInt));
        break;
      case INT_ARRAY:
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id)
                .type(GraphQLList.list(Scalars.GraphQLInt)));
        break;
      case REF:
        if (hasViewPermission(table)) {
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(createTableObjectType(col.getRefTable()))
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.FILTER_ARGUMENT)
                          .type(getTableFilterInputType(col.getRefTable()))
                          .build()));
        }
        break;
      case REF_ARRAY:
      case REFBACK:
        if (hasViewPermission(table)) {
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(id)
                  .type(GraphQLList.list(createTableObjectType(col.getRefTable())))
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.FILTER_ARGUMENT)
                          .type(getTableFilterInputType(col.getRefTable()))
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.LIMIT)
                          .type(Scalars.GraphQLInt)
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.OFFSET)
                          .type(Scalars.GraphQLInt)
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(GraphqlConstants.ORDERBY)
                          .type(createTableOrderByInputType(col.getRefTable()))
                          .build()));
        }
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id + "_agg")
                .type(createTableAggregationType(col.getRefTable()))
                .argument(
                    GraphQLArgument.newArgument()
                        .name(GraphqlConstants.FILTER_ARGUMENT)
                        .type(getTableFilterInputType(col.getRefTable()))
                        .build()));
        tableBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(id + "_groupBy")
                .type(GraphQLList.list(createTableGroupByType(col.getRefTable())))
                .argument(
                    GraphQLArgument.newArgument()
                        .name(GraphqlConstants.FILTER_ARGUMENT)
                        .type(getTableFilterInputType(col.getRefTable()))
                        .build()));
        break;
      default:
        throw new UnsupportedOperationException("Not yet implemented type " + col.getColumnType());
    }
  }

  private boolean hasViewPermission(TableMetadata table) {
    return table.getTableType().equals(ONTOLOGIES)
        || schema.getInheritedRolesForActiveUser().contains(VIEWER.toString());
  }

  private GraphQLNamedOutputType createTableGroupByType(TableMetadata table) {
    String tableGroupByType = table.getIdentifier() + "GroupBy";
    if (!tableGroupByTypes.containsKey(tableGroupByType)) {
      // add reference in case of self reference
      tableGroupByTypes.put(tableGroupByType, GraphQLTypeReference.typeRef(tableGroupByType));
      // group by options, for now only ref, refArray
      GraphQLObjectType.Builder groupByBuilder =
          GraphQLObjectType.newObject().name(tableGroupByType);
      groupByBuilder.field(
          GraphQLFieldDefinition.newFieldDefinition().name("count").type(Scalars.GraphQLInt));
      List<Column> aggCols =
          table.getColumnsIncludingSubclasses().stream()
              .filter(
                  c ->
                      ColumnType.INT.equals(c.getColumnType())
                          || ColumnType.DECIMAL.equals(c.getColumnType())
                          || ColumnType.LONG.equals(c.getColumnType()))
              .toList();
      if (aggCols.size() > 0) {
        GraphQLObjectType.Builder sumBuilder =
            GraphQLObjectType.newObject().name(tableGroupByType + "_" + SUM_FIELD);
        for (Column aggCol : aggCols) {
          sumBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(aggCol.getIdentifier())
                  .type(graphQLTypeOf(aggCol)));
        }
        groupByBuilder.field(
            GraphQLFieldDefinition.newFieldDefinition().name(SUM_FIELD).type(sumBuilder.build()));
      }

      for (Column column : table.getColumnsIncludingSubclasses()) {
        // for now only 'ref' types. We might want to have truncating actions for the other types.
        if (column.isReference() && (hasViewPermission(table) || column.isOntology())) {
          groupByBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(column.getIdentifier())
                  .type(createTableObjectType(column.getRefTable())));
        } else if (!column.isReference() && hasViewPermission(table)) {
          createTableField(column, groupByBuilder);
        }
      }
      tableGroupByTypes.put(tableGroupByType, groupByBuilder.build());
    }
    return tableGroupByTypes.get(tableGroupByType);
  }

  private GraphQLNamedOutputType createTableAggregationType(TableMetadata table) {
    String tableAggregationType = getTableTypeIdentifier(table) + "Aggregate";

    if (tableAggTypes.containsKey(tableAggregationType)) {
      return tableAggTypes.get(tableAggregationType);
    }

    // put reference in case of self reference
    tableAggTypes.put(tableAggregationType, GraphQLTypeReference.typeRef(tableAggregationType));
    // aggregate type
    GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(tableAggregationType);
    if (schema.hasActiveUserRole(EXISTS) || table.getTableType().equals(ONTOLOGIES)) {
      builder.field(
          GraphQLFieldDefinition.newFieldDefinition().name("exists").type(Scalars.GraphQLBoolean));
    }
    if (schema.hasActiveUserRole(RANGE) || table.getTableType().equals(ONTOLOGIES)) {
      builder.field(
          GraphQLFieldDefinition.newFieldDefinition().name("count").type(Scalars.GraphQLInt));
    }
    if (schema.hasActiveUserRole(VIEWER) || table.getTableType().equals(ONTOLOGIES)) {
      List<Column> aggCols =
          table.getColumnsIncludingSubclasses().stream()
              .filter(
                  c ->
                      ColumnType.INT.equals(c.getColumnType())
                          || ColumnType.DECIMAL.equals(c.getColumnType())
                          || ColumnType.LONG.equals(c.getColumnType()))
              .toList();

      if (!aggCols.isEmpty()) {
        GraphQLObjectType.Builder max =
            GraphQLObjectType.newObject().name(tableAggregationType + "_max");
        GraphQLObjectType.Builder min =
            GraphQLObjectType.newObject().name(tableAggregationType + "_min");
        GraphQLObjectType.Builder sum =
            GraphQLObjectType.newObject().name(tableAggregationType + "_sum");
        GraphQLObjectType.Builder avg =
            GraphQLObjectType.newObject().name(tableAggregationType + "_avg");
        for (Column col : aggCols) {
          max.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getIdentifier())
                  .type(graphQLTypeOf(col)));
          min.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getIdentifier())
                  .type(graphQLTypeOf(col)));
          avg.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getIdentifier())
                  .type(Scalars.GraphQLFloat));
          sum.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getIdentifier())
                  .type(graphQLTypeOf(col)));
        }
        builder
            .field(GraphQLFieldDefinition.newFieldDefinition().name(MAX_FIELD).type(max))
            .field(GraphQLFieldDefinition.newFieldDefinition().name(MIN_FIELD).type(min))
            .field(GraphQLFieldDefinition.newFieldDefinition().name(AVG_FIELD).type(avg))
            .field(GraphQLFieldDefinition.newFieldDefinition().name(SUM_FIELD).type(sum));
      }
    }
    tableAggTypes.put(tableAggregationType, builder.build());

    return tableAggTypes.get(tableAggregationType);
  }

  private GraphQLNamedInputType getTableFilterInputType(TableMetadata table) {
    String tableFilterInputType = getTableTypeIdentifier(table) + FILTER;
    if (!tableFilterInputTypes.containsKey(tableFilterInputType)) {
      // put reference in case of self reference\
      tableFilterInputTypes.put(
          tableFilterInputType, GraphQLTypeReference.typeRef(tableFilterInputType));
      GraphQLInputObjectType.Builder filterBuilder =
          GraphQLInputObjectType.newInputObject().name(tableFilterInputType);
      if (table.getPrimaryKeyColumns().size() > 0) {
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_EQUALS)
                .type(GraphQLList.list(getPrimaryKeyInput(table)))
                .build());
      }
      if (TableType.ONTOLOGIES.equals(table.getTableType())) {
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_MATCH_INCLUDING_CHILDREN)
                .type(GraphQLList.list(Scalars.GraphQLString))
                .build());
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_MATCH_INCLUDING_PARENTS)
                .type(GraphQLList.list(Scalars.GraphQLString))
                .build());
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_SEARCH_INCLUDING_PARENTS)
                .type(GraphQLList.list(Scalars.GraphQLString))
                .build());
        filterBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(FILTER_MATCH_PATH)
                .type(GraphQLList.list(Scalars.GraphQLString))
                .build());
      }
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_SEARCH)
              .type(Scalars.GraphQLString)
              .build());
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_OR)
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(tableFilterInputType)))
              .build());
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(FILTER_AND)
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(tableFilterInputType)))
              .build());
      for (Column col : table.getColumnsIncludingSubclasses()) {
        if (col.isReference()) {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(col.getIdentifier())
                  .type(getTableFilterInputType(col.getRefTable()))
                  .build());
          // prefix _is to ensure we can't clash with column named 'is'
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(FILTER_IS_NULL)
                  .type(Scalars.GraphQLBoolean)
                  .build());
          // in case of single primary key we don't need nested filter object
          GraphQLInputType refType =
              col.getReferences().size() == 1
                  ? getGraphQLInputType(col.getReferences().get(0).getPrimitiveType())
                  : getPrimaryKeyInput(table);
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(FILTER_MATCH_ALL)
                  .type(GraphQLList.list(refType))
                  .build());
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(FILTER_MATCH_ANY)
                  .type(GraphQLList.list(refType))
                  .build());
        } else if (col.getColumnType().getOperators().length > 0) {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(col.getIdentifier())
                  .type(getColumnFilterInputType(col))
                  .build());
        }
      }
      // replace reference with the actual thing
      tableFilterInputTypes.put(tableFilterInputType, filterBuilder.build());
    }
    return tableFilterInputTypes.get(tableFilterInputType);
  }

  private GraphQLNamedInputType createTableOrderByInputType(TableMetadata table) {
    String tableOrderByInputType = getTableTypeIdentifier(table) + GraphqlConstants.ORDERBY;
    if (!tableOrderByInputTypes.containsKey(tableOrderByInputType)) {
      // put reference in case of self reference
      tableOrderByInputTypes.put(
          tableOrderByInputType, GraphQLTypeReference.typeRef(tableOrderByInputType));
      // build the type
      GraphQLInputObjectType.Builder orderByBuilder =
          GraphQLInputObjectType.newInputObject().name(tableOrderByInputType);
      for (Column col : table.getColumnsIncludingSubclasses()) {
        orderByBuilder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(col.getIdentifier())
                .type(orderByEnum));
      }
      tableOrderByInputTypes.put(tableOrderByInputType, orderByBuilder.build());
    }
    return tableOrderByInputTypes.get(tableOrderByInputType);
  }

  private GraphQLInputObjectType getColumnFilterInputType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (this.columnFilterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          GraphQLInputObjectType.newInputObject().name("Molgenis" + typeName + FILTER);
      for (Operator operator : type.getOperators()) {
        if (IS_NULL.equals(operator)) {
          builder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(operator.getName())
                  .type(Scalars.GraphQLBoolean));
        } else {
          builder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(operator.getName())
                  .type(GraphQLList.list(graphQLTypeOf(column))));
        }
      }
      columnFilterInputTypes.put(type, builder.build());
    }
    return columnFilterInputTypes.get(type);
  }

  private GraphQLScalarType graphQLTypeOf(Column col) {
    switch (col.getColumnType().getBaseType()) {
      case BOOL, BOOL_ARRAY:
        return Scalars.GraphQLBoolean;
      case INT, INT_ARRAY:
        return Scalars.GraphQLInt;
      case LONG, LONG_ARRAY:
        return GraphQLLong;
      case DECIMAL, DECIMAL_ARRAY:
        return Scalars.GraphQLFloat;
      case JSON:
        return GraphQLJsonAsString;
      case DATE,
          DATETIME,
          PERIOD,
          STRING,
          TEXT,
          UUID,
          FILE,
          DATE_ARRAY,
          DATETIME_ARRAY,
          PERIOD_ARRAY,
          STRING_ARRAY,
          TEXT_ARRAY,
          EMAIL_ARRAY,
          HYPERLINK_ARRAY:
      case UUID_ARRAY:
        return Scalars.GraphQLString;
      case REF_ARRAY, REF, REFBACK:
      default:
        throw new UnsupportedOperationException("Type not supported yet: " + col.getColumnType());
    }
  }

  public static FilterBean[] convertMapToFilterArray(
      TableMetadata table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      if (entry.getKey().equals(FILTER_OR) || entry.getKey().equals(FILTER_AND)) {
        List<Map<String, Object>> nested = (List<Map<String, Object>>) entry.getValue();
        List<Filter> nestedFilters =
            nested.stream().map(m -> and(convertMapToFilterArray(table, m))).toList();
        if (entry.getKey().equals(FILTER_OR)) {
          subFilters.add(or(nestedFilters.toArray(new Filter[nestedFilters.size()])));
        } else {
          subFilters.add(and(nestedFilters.toArray(new Filter[nestedFilters.size()])));
        }
      } else if (entry.getKey().equals(FILTER_SEARCH)) {
        if (entry.getValue() instanceof String && !entry.getValue().toString().trim().equals("")) {
          subFilters.add(
              f(
                  Operator
                      .TEXT_SEARCH, // might change to trigram search if we learn how to tune for
                  // short terms
                  Arrays.stream(entry.getValue().toString().split(" ")).toArray(String[]::new)));
        }
      } else if (entry.getKey().equals(FILTER_EQUALS)) {
        //  complex filter, should be an list of maps per graphql contract
        if (entry.getValue() != null) {
          subFilters.add(
              or(
                  ((List<Map<String, Object>>) entry.getValue())
                      .stream().map(v -> createKeyFilter(table, v)).collect(Collectors.toList())));
        }
      } else if (entry.getKey().equals(FILTER_MATCH_INCLUDING_CHILDREN)
          || entry.getKey().equals(FILTER_MATCH_INCLUDING_PARENTS)
          || entry.getKey().equals(FILTER_MATCH_PATH)
          || entry.getKey().equals(FILTER_SEARCH_INCLUDING_PARENTS)
          || entry.getKey().equals(FILTER_MATCH_ALL)) {
        switch (entry.getKey()) {
          case FILTER_MATCH_INCLUDING_CHILDREN ->
              subFilters.add(
                  f(Operator.MATCH_ANY_INCLUDING_CHILDREN, ((List) entry.getValue()).toArray()));
          case FILTER_MATCH_INCLUDING_PARENTS ->
              subFilters.add(
                  f(Operator.MATCH_ANY_INCLUDING_PARENTS, ((List) entry.getValue()).toArray()));
          case FILTER_MATCH_PATH ->
              subFilters.add(f(Operator.MATCH_PATH, ((List) entry.getValue()).toArray()));
          case FILTER_SEARCH_INCLUDING_PARENTS ->
              subFilters.add(
                  f(Operator.SEARCH_INCLUDING_PARENTS, ((List) entry.getValue()).toArray()));
        }
        // skip match all, handled on parent column
      } else {
        // find column by escaped name
        Column c = table.getColumnByIdIncludingSubclasses(entry.getKey());
        if (c == null)
          throw new GraphqlException(
              "Graphql API error: Column "
                  + entry.getKey()
                  + " unknown in table "
                  + table.getTableName());
        Map value = (Map) entry.getValue();
        // although nested, this should apply on this level, not sublevel
        if (value.containsKey(FILTER_MATCH_INCLUDING_CHILDREN)) {
          subFilters.add(
              f(
                  c.getName(),
                  Operator.MATCH_ANY_INCLUDING_CHILDREN,
                  ((List) value.get(FILTER_MATCH_INCLUDING_CHILDREN)).toArray(new String[0])));
          value.remove(FILTER_MATCH_INCLUDING_CHILDREN);
        } else if (value.containsKey(FILTER_SEARCH_INCLUDING_PARENTS)) {
          subFilters.add(
              f(
                  c.getName(),
                  Operator.SEARCH_INCLUDING_PARENTS,
                  ((List) value.get(FILTER_SEARCH_INCLUDING_PARENTS)).toArray(new String[0])));
          value.remove(FILTER_SEARCH_INCLUDING_PARENTS);
        } else if (value.containsKey(FILTER_MATCH_PATH)) {
          subFilters.add(
              f(
                  c.getName(),
                  Operator.MATCH_PATH,
                  ((List) value.get(FILTER_MATCH_PATH)).toArray(new String[0])));
          value.remove(FILTER_MATCH_PATH);
        } else if (value.containsKey(FILTER_IS_NULL)) {
          subFilters.add(f(c.getName(), IS_NULL, value.get(FILTER_IS_NULL)));
          value.remove(FILTER_IS_NULL);
        } else if (value.containsKey(FILTER_MATCH_ALL)) {
          //  complex filter, should be an list of maps per graphql contract
          if (entry.getValue() != null && c.getReferences().size() > 1) {
            subFilters.add(
                f(
                    c.getName(),
                    Operator.MATCH_ALL,
                    convertToPrimaryKeyRows(
                            c.getRefTable(),
                            (List<Map<String, Object>>) value.get(FILTER_MATCH_ALL))
                        .toArray()));
          } else if (entry.getValue() != null) {
            subFilters.add(
                f(c.getName(), Operator.MATCH_ALL, (List<Object>) value.get(FILTER_MATCH_ALL)));
          }
          value.remove(FILTER_MATCH_ALL);
        }

        if (value.size() == 0) continue;
        if (c.isReference()) {
          subFilters.add(
              f(
                  c.getName(),
                  convertMapToFilterArray(
                      table
                          .getSchema()
                          .getDatabase()
                          .getSchema(c.getRefTable().getSchemaName())
                          .getTable(c.getRefTableName())
                          .getMetadata(),
                      value)));
        } else {
          subFilters.add(convertMapToFilter(c.getName(), (Map<String, Object>) entry.getValue()));
        }
      }
    }
    return subFilters.toArray(new FilterBean[subFilters.size()]);
  }

  private static Filter createKeyFilter(TableMetadata table, Map<String, Object> map) {
    Objects.requireNonNull(table);
    Objects.requireNonNull(map);
    List<Filter> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      Optional<Column> column = findColumnById(table, entry.getKey());
      if (column.isEmpty()) {
        throw new MolgenisException(
            "Filter " + entry.getKey() + " unknown in table " + table.getIdentifier());
      }
      if (entry.getValue() instanceof Map) {
        result.add(
            f(
                column.get().getName(),
                createKeyFilter(
                    column.get().getRefTable(), (Map<String, Object>) entry.getValue())));
      } else {
        result.add(f(column.get().getName(), Operator.EQUALS, entry.getValue()));
      }
    }
    return and(result);
  }

  private static Filter convertMapToFilter(String name, Map<String, Object> subFilter) {
    int count = 0;
    for (Map.Entry<String, Object> entry2 : subFilter.entrySet()) {
      count++;
      if (count > 1)
        throw new MolgenisException("Can only have one operator, found multiple for " + name);
      Operator op = Operator.fromAbbreviation(entry2.getKey());
      if (entry2.getValue() instanceof List) {
        return f(name, op, (List) entry2.getValue());
      } else {
        return f(name, op, entry2.getValue());
      }
    }
    return null;
  }

  /** creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...) */
  private SelectColumn[] convertMapSelection(
      TableMetadata aTable, DataFetchingFieldSelectionSet selection) {
    List<SelectColumn> result = new ArrayList<>();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/")) {
        if (s.getSelectionSet().getFields().isEmpty()) {
          Optional<Column> column = findColumnById(aTable, s.getName());
          if (column.isPresent()) {
            result.add(new SelectColumn(column.get().getName()));
          } else {
            result.add(new SelectColumn(s.getName()));
          }
        } else {
          Optional<Column> column = findColumnById(aTable, s.getName());
          if (column.isPresent()) {
            SelectColumn sc =
                new SelectColumn(
                    column.get().getName()
                        + (s.getName().endsWith("_agg")
                            ? "_agg"
                            : s.getName().endsWith("_groupBy") ? "_groupBy" : ""),
                    convertMapSelection(
                        column.get().isReference() ? column.get().getRefTable() : null,
                        s.getSelectionSet()));
            // get limit and offset for the selection
            Map<String, Object> args = s.getArguments();
            if (args.containsKey(GraphqlConstants.FILTER_ARGUMENT)) {
              sc.where(
                  convertMapToFilterArray(
                      column.get().getRefTable(),
                      (Map<String, Object>) args.get(GraphqlConstants.FILTER_ARGUMENT)));
            }
            if (args.containsKey(GraphqlConstants.LIMIT)) {
              sc.setLimit((int) args.get(GraphqlConstants.LIMIT));
            }
            if (args.containsKey(GraphqlConstants.OFFSET)) {
              sc.setOffset((int) args.get(GraphqlConstants.OFFSET));
            }
            if (args.containsKey(GraphqlConstants.ORDERBY)) {
              TableMetadata orderByTable =
                  column.get().isReference() ? column.get().getRefTable() : column.get().getTable();
              sc.setOrderBy(convertOrderByIdsToNames(orderByTable, args));
            }
            result.add(sc);
          } else if (agg_fields.contains(s.getName())) {
            result.add(
                new SelectColumn(s.getName(), convertMapSelection(aTable, s.getSelectionSet())));
          }
        }
      }
    }
    return result.toArray(new SelectColumn[result.size()]);
  }

  private static Optional<Column> findColumnById(TableMetadata aTable, String id) {
    if (aTable != null) {
      return aTable.getColumnsIncludingSubclasses().stream()
          .filter(
              c ->
                  c.getIdentifier().equals(id)
                      || (c.getIdentifier() + "_agg").equals(id)
                      || (c.getIdentifier() + "_groupBy").equals(id))
          .findFirst();
    } else {
      return Optional.empty();
    }
  }

  private DataFetcher fetcherForTableQueryField(TableMetadata aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable.getTable();
      Query q = table.query();
      String fieldName = dataFetchingEnvironment.getField().getName();
      if (fieldName.endsWith("_agg")) {
        q = table.agg();
      } else if (fieldName.endsWith("_groupBy")) {
        q = table.groupBy();
      }
      q.select(convertMapSelection(aTable, dataFetchingEnvironment.getSelectionSet()));
      Map<String, Object> args = dataFetchingEnvironment.getArguments();
      if (dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT) != null) {
        q.where(
            convertMapToFilterArray(
                table.getMetadata(),
                dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT)));
      }
      if (args.containsKey(GraphqlConstants.LIMIT)) {
        q.limit((int) args.get(GraphqlConstants.LIMIT));
      }
      if (args.containsKey(GraphqlConstants.OFFSET)) {
        q.offset((int) args.get(GraphqlConstants.OFFSET));
      }
      if (args.containsKey(GraphqlConstants.ORDERBY)) {
        q.orderBy(convertOrderByIdsToNames(aTable, args));
      }

      String search = dataFetchingEnvironment.getArgument(GraphqlConstants.SEARCH);
      if (search != null && !search.trim().equals("")) {
        q.search(search);
      }

      Object result = transform(q.retrieveJSON());
      // bit silly, we have to remove root field here. Some refactoring makes this look nicer
      if (result != null) {
        return ((Map<String, Object>) result).get(fieldName);
      } else {
        return null;
      }
    };
  }

  private Map<String, Order> convertOrderByIdsToNames(
      TableMetadata aTable, Map<String, Object> args) {
    Map<String, Order> orderBy = (Map<String, Order>) args.get(ORDERBY);
    Map<String, Order> unescapedMap = new HashMap<>();
    for (var entry : orderBy.entrySet()) {
      Optional<Column> column = findColumnById(aTable, entry.getKey());
      if (column.isPresent()) {
        unescapedMap.put(column.get().getName(), entry.getValue());
      } else {
        throw new MolgenisException("Unknown order by column id: " + entry.getKey());
      }
    }
    return unescapedMap;
  }

  private GraphQLFieldDefinition getMutationDefinition(Schema schema, MutationType type) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name(type.name().toLowerCase())
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, type));
    for (TableMetadata table : schema.getMetadata().getTables()) {
      if (!table.getColumnsIncludingSubclassesExcludingHeadings().isEmpty()) {
        fieldBuilder.argument(
            GraphQLArgument.newArgument()
                .name(table.getIdentifier())
                .type(GraphQLList.list(rowInputType(table))));
      }
    }
    return fieldBuilder.build();
  }

  public GraphQLFieldDefinition insertMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.INSERT);
  }

  public GraphQLFieldDefinition updateMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.UPDATE);
  }

  public GraphQLFieldDefinition upsertMutation(Schema schema) {
    return getMutationDefinition(schema, MutationType.SAVE);
  }

  public GraphQLFieldDefinition deleteMutation(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("delete")
            .type(typeForMutationResult)
            .dataFetcher(fetcher(schema, MutationType.DELETE));

    for (Table table : schema.getTablesSorted()) {
      // if no pkey is provided, you cannot delete rows
      if (!schema.getMetadata().getTableMetadata(table.getName()).getPrimaryKeys().isEmpty()) {
        fieldBuilder.argument(
            GraphQLArgument.newArgument()
                .name(table.getIdentifier())
                // reuse same input as insert
                .type(
                    GraphQLList.list(
                        GraphQLTypeReference.typeRef(
                            getTableTypeIdentifier(table.getMetadata()) + INPUT))));
      }
    }
    return fieldBuilder.build();
  }

  private DataFetcher fetcher(Schema schema, MutationType mutationType) {
    return dataFetchingEnvironment -> {
      StringBuilder result = new StringBuilder();
      boolean any = false;
      for (TableMetadata tableMetadata : schema.getMetadata().getTables()) {
        List<Map<String, Object>> rowsAslistOfMaps =
            dataFetchingEnvironment.getArgument(tableMetadata.getIdentifier());
        if (rowsAslistOfMaps != null) {
          String tableName = tableMetadata.getTableName();
          Table table = tableMetadata.getTable();
          int count = 0;
          switch (mutationType) {
            case UPDATE:
              count = table.update(TypeUtils.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("updated " + count + " records to " + tableName + "\n");
              break;
            case INSERT:
              count = table.insert(TypeUtils.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("inserted " + count + " records to " + tableName + "\n");
              break;
            case SAVE:
              count = table.save(TypeUtils.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("upserted " + count + " records to " + tableName + "\n");
              break;
            case DELETE:
              count = table.delete(TypeUtils.convertToRows(table.getMetadata(), rowsAslistOfMaps));
              result.append("delete " + count + " records from " + tableName + "\n");
              break;
          }
          any = true;
        }
      }
      if (!any) throw new MolgenisException("Error with save: no data provided");
      return new GraphqlApiMutationResult(SUCCESS, result.toString());
    };
  }

  private GraphQLNamedInputType rowInputType(TableMetadata table) {
    String rowInputType = getTableTypeIdentifier(table);
    if (rowInputTypes.get(rowInputType) == null) {
      // in case of self reference
      rowInputTypes.put(rowInputType, GraphQLTypeReference.typeRef(rowInputType + INPUT));
      GraphQLInputObjectType.Builder inputBuilder =
          GraphQLInputObjectType.newInputObject().name(rowInputType + INPUT);
      for (Column col : table.getColumnsIncludingSubclassesExcludingHeadings()) {
        GraphQLInputType type;
        if (col.isReference()) {
          if (col.isRef()) {
            type = rowInputType(col.getRefTable());
          } else {
            type = GraphQLList.list(rowInputType(col.getRefTable()));
          }
        } else {
          ColumnType columnType = col.getPrimitiveColumnType();
          type = getGraphQLInputType(columnType);
        }
        inputBuilder.field(
            GraphQLInputObjectField.newInputObjectField().name(col.getIdentifier()).type(type));
      }
      rowInputTypes.put(rowInputType, inputBuilder.build());
    }
    return rowInputTypes.get(rowInputType);
  }

  public GraphQLNamedInputType getPrimaryKeyInput(TableMetadata table) {
    String primaryKeyInput = getTableTypeIdentifier(table) + "PkeyInput";
    if (!refTypes.containsKey(primaryKeyInput)) {
      // in case circular reference
      refTypes.put(primaryKeyInput, GraphQLTypeReference.typeRef(primaryKeyInput));
      GraphQLInputObjectType.Builder refTypeBuilder =
          GraphQLInputObjectType.newInputObject().name(primaryKeyInput);
      for (Column ref : table.getPrimaryKeyColumns()) {
        GraphQLInputType type;
        ColumnType columnType = ref.getColumnType();
        if (ref.isReference()) {
          type = getPrimaryKeyInput(ref.getRefTable());
        } else {
          type = getGraphQLInputType(columnType);
        }
        refTypeBuilder.field(
            GraphQLInputObjectField.newInputObjectField().name(ref.getIdentifier()).type(type));
      }
      refTypes.put(primaryKeyInput, refTypeBuilder.build());
    }
    return refTypes.get(primaryKeyInput);
  }

  private GraphQLInputType getGraphQLInputType(ColumnType columnType) {
    return switch (columnType.getBaseType()) {
      case FILE -> GraphqlCustomTypes.GraphQLFileUpload;
      case BOOL -> Scalars.GraphQLBoolean;
      case INT -> Scalars.GraphQLInt;
      case LONG -> GraphQLLong;
      case DECIMAL -> Scalars.GraphQLFloat;
      case UUID, STRING, TEXT, DATE, DATETIME, PERIOD -> Scalars.GraphQLString;
      case BOOL_ARRAY -> GraphQLList.list(Scalars.GraphQLBoolean);
      case INT_ARRAY -> GraphQLList.list(Scalars.GraphQLInt);
      case LONG_ARRAY -> GraphQLList.list(GraphQLLong);
      case DECIMAL_ARRAY -> GraphQLList.list(Scalars.GraphQLFloat);
      case JSON -> GraphqlCustomTypes.GraphQLJsonAsString;
      case STRING_ARRAY,
              TEXT_ARRAY,
              DATE_ARRAY,
              DATETIME_ARRAY,
              PERIOD_ARRAY,
              UUID_ARRAY,
              EMAIL_ARRAY,
              HYPERLINK_ARRAY ->
          GraphQLList.list(Scalars.GraphQLString);
      default ->
          throw new MolgenisException(
              "Internal error: Type " + columnType + " not expected at this place");
    };
  }
}
