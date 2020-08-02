package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.transform;
import static org.molgenis.emx2.graphql.GraphqlConstants.EQUALS;
import static org.molgenis.emx2.graphql.GraphqlConstants.FILTER;
import static org.molgenis.emx2.graphql.GraphqlTableMutationFields.getPrimaryKeyInput;
import static org.molgenis.emx2.sql.SqlQuery.*;

public class GraphqlTableQueryFields {

  private GraphqlTableQueryFields() {
    // hide constructor
  }

  static final GraphQLEnumType orderByEnum =
      GraphQLEnumType.newEnum()
          .name("MolgenisOrderByEnum")
          .value(Order.ASC.name(), Order.ASC)
          .value(Order.DESC.name(), Order.DESC)
          .build();

  public static GraphQLFieldDefinition.Builder tableQueryField(Table table) {
    GraphQLObjectType tableType = GraphqlTableQueryFields.createTableObjectType(table);

    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getName())
        .type(GraphQLList.list(tableType))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(createTableFilterInputObjectType(table.getMetadata()))
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
                .type(createTableOrderByInputObjectType(table))
                .build());
  }

  public static GraphQLFieldDefinition.Builder tableAggField(Table table) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getName() + "_agg")
        .type(createTableAggregationType(table))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(createTableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build());
  }

  private static DataFetcher fetcherForTableQueryField(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      Query q = table.query();
      String fieldName = dataFetchingEnvironment.getField().getName();
      if (fieldName.endsWith("_agg")) {
        q = table.agg();
      }
      q.select(convertMapSelection(dataFetchingEnvironment.getSelectionSet()));
      Map<String, Object> args = dataFetchingEnvironment.getArguments();
      if (dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT) != null) {
        q.where(
            convertMapToFilterArray(
                table, dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT)));
      }
      if (args.containsKey(GraphqlConstants.LIMIT)) {
        q.limit((int) args.get(GraphqlConstants.LIMIT));
      }
      if (args.containsKey(GraphqlConstants.OFFSET)) {
        q.offset((int) args.get(GraphqlConstants.OFFSET));
      }
      if (args.containsKey(GraphqlConstants.ORDERBY)) {
        q.orderBy((Map<String, Order>) args.get(GraphqlConstants.ORDERBY));
      }

      String search = dataFetchingEnvironment.getArgument(GraphqlConstants.SEARCH);
      if (search != null && !search.trim().equals("")) {
        q.search(search);
      }

      Object result = transform(q.retrieveJSON());
      // bit silly, we have to remove root field here. Some refactoring makes this look nicer
      if (result != null) return ((Map<String, Object>) result).get(fieldName);
      return null;
    };
  }

  private static GraphQLObjectType createTableObjectType(Table table) {
    GraphQLObjectType.Builder tableBuilder = GraphQLObjectType.newObject().name(table.getName());
    for (Column col : table.getMetadata().getColumns())
      switch (col.getColumnType()) {
        case BOOL:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(Scalars.GraphQLBoolean));
          break;
        case BOOL_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLList.list(Scalars.GraphQLBoolean)));
          break;
        case STRING:
        case TEXT:
        case UUID:
        case DATE:
        case DATETIME:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(Scalars.GraphQLString));
          break;
        case STRING_ARRAY:
        case TEXT_ARRAY:
        case DATE_ARRAY:
        case DATETIME_ARRAY:
        case UUID_ARRAY:
        case JSONB_ARRAY:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLList.list(Scalars.GraphQLString)));
          break;
        case DECIMAL:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(Scalars.GraphQLBigDecimal));
          break;
        case INT:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(Scalars.GraphQLInt));
          break;
        case REF:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName())));
          break;
        case REF_ARRAY:
        case REFBACK:
        case MREF:
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName())))
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
                          .type(
                              GraphQLTypeReference.typeRef(
                                  col.getRefTableName() + GraphqlConstants.ORDERBY))
                          .build()));
          tableBuilder.field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(col.getName() + "_agg")
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName() + "Aggregate")));
          break;
        default:
          throw new UnsupportedOperationException(
              "Not yet implemented type " + col.getColumnType());
      }
    return tableBuilder.build();
  }

  private static GraphQLObjectType createTableAggregationType(Table table) {
    GraphQLObjectType.Builder builder =
        GraphQLObjectType.newObject().name(table.getName() + "Aggregate");
    builder.field(
        GraphQLFieldDefinition.newFieldDefinition().name("count").type(Scalars.GraphQLInt));
    for (Column col : table.getMetadata().getColumns()) {
      // aggregate options
      ColumnType type = col.getColumnType();
      if (ColumnType.INT.equals(type) || ColumnType.DECIMAL.equals(type)) {
        builder.field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(col.getName())
                .type(
                    GraphQLObjectType.newObject()
                        .name(table.getName() + "AggregatorFor" + col.getName())
                        .field(
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(MAX_FIELD)
                                .type(graphQLTypeOf(col)))
                        .field(
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(MIN_FIELD)
                                .type(graphQLTypeOf(col)))
                        .field(
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(AVG_FIELD)
                                .type(Scalars.GraphQLFloat))
                        .field(
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(SUM_FIELD)
                                .type(graphQLTypeOf(col)))));
      } else {
        // group by options
        // TODO LATER
        // builder.field(newFieldDefinition().name(col.getName()).type(graphQLTypeOf(col)));
      }
    }
    return builder.build();
  }

  private static Map<String, GraphQLInputObjectType> tableFilterInputTypes = new LinkedHashMap<>();

  private static GraphQLInputObjectType createTableFilterInputObjectType(TableMetadata table) {
    if (!tableFilterInputTypes.containsKey(table.getTableName())) {
      GraphQLInputObjectType.Builder filterBuilder =
          GraphQLInputObjectType.newInputObject().name(table.getTableName() + FILTER);
      filterBuilder.field(
          GraphQLInputObjectField.newInputObjectField()
              .name(EQUALS)
              .type(GraphQLList.list(getPrimaryKeyInput(table)))
              .build());
      for (Column col : table.getColumns()) {
        ColumnType type = col.getColumnType();
        if (ColumnType.REF.equals(type)
            || ColumnType.REF_ARRAY.equals(type)
            || ColumnType.REFBACK.equals(type)) {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(col.getName())
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName() + FILTER))
                  .build());
        } else {
          filterBuilder.field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(col.getName())
                  .type(createColumnFilterInputObjectType(col))
                  .build());
        }
      }
      tableFilterInputTypes.put(table.getTableName(), filterBuilder.build());
    }
    return tableFilterInputTypes.get(table.getTableName());
  }

  private static GraphQLInputObjectType createTableOrderByInputObjectType(Table table) {
    GraphQLInputObjectType.Builder orderByBuilder =
        GraphQLInputObjectType.newInputObject().name(table.getName() + GraphqlConstants.ORDERBY);
    for (Column col : table.getMetadata().getColumns()) {
      ColumnType type = col.getColumnType();
      if (!ColumnType.REF_ARRAY.equals(type) && !ColumnType.REFBACK.equals(type)) {
        orderByBuilder.field(
            GraphQLInputObjectField.newInputObjectField().name(col.getName()).type(orderByEnum));
      }
    }
    return orderByBuilder.build();
  }

  // cache so we can reuse filter input types between tables
  static Map<ColumnType, GraphQLInputObjectType> columnFilterInputTypes = new LinkedHashMap<>();

  private static GraphQLInputObjectType createColumnFilterInputObjectType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (columnFilterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          GraphQLInputObjectType.newInputObject().name("Molgenis" + typeName + FILTER);
      for (Operator operator : type.getOperators()) {
        builder.field(
            GraphQLInputObjectField.newInputObjectField()
                .name(operator.getName())
                .type(GraphQLList.list(graphQLTypeOf(column))));
      }
      columnFilterInputTypes.put(type, builder.build());
    }
    return columnFilterInputTypes.get(type);
  }

  private static GraphQLScalarType graphQLTypeOf(Column col) {
    switch (col.getColumnType()) {
      case BOOL:
      case BOOL_ARRAY:
        return Scalars.GraphQLBoolean;
      case INT:
      case INT_ARRAY:
        return Scalars.GraphQLInt;
      case DECIMAL:
      case DECIMAL_ARRAY:
        return Scalars.GraphQLFloat;
      case DATE:
      case DATETIME:
      case STRING:
      case TEXT:
      case UUID:
      case DATE_ARRAY:
      case DATETIME_ARRAY:
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case UUID_ARRAY:
        return Scalars.GraphQLString;
      case REF_ARRAY:
      case REF:
      case REFBACK:
      case MREF:
      default:
        throw new UnsupportedOperationException("Type not supported yet: " + col.getColumnType());
    }
  }

  private static FilterBean[] convertMapToFilterArray(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      if (entry.getKey().equals(EQUALS)) {
        //  complex filter, should be an list of maps per graphql contract
        subFilters.add(
            or(
                ((List<Map<String, Object>>) entry.getValue())
                    .stream().map(v -> createKeyFilter(v)).collect(Collectors.toList())));
      } else {
        Column c = table.getMetadata().getColumn(entry.getKey());
        if (c == null)
          throw new GraphqlException(
              "Graphql API error",
              "Column " + entry.getKey() + " unknown in table " + table.getName());
        ColumnType type = c.getColumnType();
        if (ColumnType.REF.equals(type)
            || ColumnType.REF_ARRAY.equals(type)
            || ColumnType.REFBACK.equals(type)) {
          subFilters.add(
              f(
                  c.getName(),
                  convertMapToFilterArray(
                      table.getSchema().getTable(c.getRefTableName()), (Map) entry.getValue())));
        } else {
          if (entry.getValue() instanceof Map) {
            subFilters.add(
                convertMapToFilter(entry.getKey(), (Map<String, Object>) entry.getValue()));
          } else {
            throw new GraphqlException(
                "Graphql API error",
                "unknown filter expression " + entry.getValue() + " for column " + entry.getKey());
          }
        }
      }
    }
    return subFilters.toArray(new FilterBean[subFilters.size()]);
  }

  private static Filter createKeyFilter(Map<String, Object> map) {

    List<Filter> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.add(f(entry.getKey(), createKeyFilter((Map<String, Object>) entry.getValue())));
      } else {
        result.add(f(entry.getKey(), Operator.EQUALS, entry.getValue()));
      }
    }
    return and(result);
  }

  private static Filter convertMapToFilter(String name, Map<String, Object> subFilter) {
    int count = 0;
    for (Map.Entry<String, Object> entry2 : subFilter.entrySet()) {
      count++;
      if (count > 1)
        throw new MolgenisException("Can only have one operator, found multiple for " + name, "");
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
  private static SelectColumn[] convertMapSelection(DataFetchingFieldSelectionSet selection) {
    List<SelectColumn> result = new ArrayList<>();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/"))
        if (s.getSelectionSet().getFields().isEmpty()) {
          result.add(new SelectColumn(s.getName()));
        } else {
          SelectColumn sc = new SelectColumn(s.getName(), convertMapSelection(s.getSelectionSet()));
          // get limit and offset for the selection
          Map<String, Object> args = s.getArguments();
          if (args.containsKey(GraphqlConstants.LIMIT)) {
            sc.setLimit((int) args.get(GraphqlConstants.LIMIT));
          }
          if (args.containsKey(GraphqlConstants.OFFSET)) {
            sc.setOffset((int) args.get(GraphqlConstants.OFFSET));
          }
          if (args.containsKey(GraphqlConstants.ORDERBY)) {
            sc.setOrderBy((Map<String, Order>) args.get(GraphqlConstants.ORDERBY));
          }
          result.add(sc);
        }
    }
    return result.toArray(new SelectColumn[result.size()]);
  }
}
