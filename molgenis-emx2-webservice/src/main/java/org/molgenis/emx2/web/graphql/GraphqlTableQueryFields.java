package org.molgenis.emx2.web.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.FilterBean;
import org.molgenis.emx2.SelectColumn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.Order.DESC;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.sql.SqlQueryGraphHelper.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.graphql.GraphqlApi.*;

public class GraphqlTableQueryFields {

  private GraphqlTableQueryFields() {
    // hide constructor
  }

  static final GraphQLEnumType orderByEnum =
      newEnum().name("MolgenisOrderByEnum").value(ASC.name(), ASC).value(DESC.name(), DESC).build();

  public static GraphQLFieldDefinition.Builder tableQueryField(Table table) {
    GraphQLObjectType tableType = GraphqlTableQueryFields.createTableObjectType(table);
    GraphQLObjectType connection =
        GraphqlTableQueryFields.createTableableConnectionObjectType(table, tableType);

    return newFieldDefinition()
        .name(table.getName())
        .type(connection)
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(FILTER)
                .type(createTableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(GraphQLArgument.newArgument().name(SEARCH).type(Scalars.GraphQLString).build());
  }

  private static DataFetcher fetcherForTableQueryField(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      Query q = table.query();
      q.select(convertMapSelection(dataFetchingEnvironment.getSelectionSet()));
      if (dataFetchingEnvironment.getArgument(FILTER) != null) {
        q.filter(convertMapToFilterArray(table, dataFetchingEnvironment.getArgument(FILTER)));
      }
      String search = dataFetchingEnvironment.getArgument(SEARCH);
      if (search != null) {
        q.search(search);
      }

      return transform(q.retrieveJSON());
    };
  }

  private static GraphQLObjectType createTableableConnectionObjectType(
      Table table, GraphQLObjectType tableType) {
    GraphQLObjectType.Builder connectionBuilder = newObject().name(table.getName() + "Connection");
    connectionBuilder.field(
        newFieldDefinition().name(DATA_AGG_FIELD).type(createTableAggregationType(table)));
    connectionBuilder.field(
        newFieldDefinition()
            .name(DATA_FIELD)
            .type(GraphQLList.list(tableType))
            .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt).build())
            .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt).build())
            .argument(
                GraphQLArgument.newArgument()
                    .name(ORDERBY)
                    .type(createTableOrderByInputObjectType(table))
                    .build()));
    return connectionBuilder.build();
  }

  private static GraphQLObjectType createTableObjectType(Table table) {
    GraphQLObjectType.Builder tableBuilder = newObject().name(table.getName());
    for (Column col : table.getMetadata().getColumns())
      switch (col.getColumnType()) {
        case DECIMAL:
          tableBuilder.field(
              newFieldDefinition().name(col.getName()).type(Scalars.GraphQLBigDecimal));
          break;
        case INT:
          tableBuilder.field(newFieldDefinition().name(col.getName()).type(Scalars.GraphQLInt));
          break;
        case REF:
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName())));
          break;
        case REF_ARRAY:
        case REFBACK:
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName())
                  .type(GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName())))
                  .argument(
                      GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt).build())
                  .argument(
                      GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt).build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name(ORDERBY)
                          .type(GraphQLTypeReference.typeRef(col.getRefTableName() + ORDERBY))
                          .build()));
          tableBuilder.field(
              newFieldDefinition()
                  .name(col.getName() + "_agg")
                  .type(GraphQLTypeReference.typeRef(col.getRefTableName() + "Aggregate")));
          break;
        default:
          tableBuilder.field(newFieldDefinition().name(col.getName()).type(Scalars.GraphQLString));
      }
    return tableBuilder.build();
  }

  private static GraphQLObjectType createTableAggregationType(Table table) {
    GraphQLObjectType.Builder builder = newObject().name(table.getName() + "Aggregate");
    builder.field(newFieldDefinition().name("count").type(Scalars.GraphQLInt));
    for (Column col : table.getMetadata().getColumns()) {
      // aggregate options
      ColumnType type = col.getColumnType();
      if (INT.equals(type) || DECIMAL.equals(type)) {
        builder.field(
            newFieldDefinition()
                .name(col.getName())
                .type(
                    newObject()
                        .name(table.getName() + "AggregatorFor" + col.getName())
                        .field(newFieldDefinition().name(MAX_FIELD).type(graphQLTypeOf(col)))
                        .field(newFieldDefinition().name(MIN_FIELD).type(graphQLTypeOf(col)))
                        .field(newFieldDefinition().name(AVG_FIELD).type(Scalars.GraphQLFloat))
                        .field(newFieldDefinition().name(SUM_FIELD).type(graphQLTypeOf(col)))));
      } else {
        // group by options
        // TODO LATER
        // builder.field(newFieldDefinition().name(col.getName()).type(graphQLTypeOf(col)));
      }
    }
    return builder.build();
  }

  private static GraphQLInputObjectType createTableFilterInputObjectType(TableMetadata table) {
    GraphQLInputObjectType.Builder filterBuilder =
        newInputObject().name(table.getTableName() + FILTER);
    for (Column col : table.getColumns()) {
      ColumnType type = col.getColumnType();
      if (REF.equals(type) || REF_ARRAY.equals(type) || REFBACK.equals(type)) {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getName())
                .type(GraphQLTypeReference.typeRef(col.getRefTableName() + FILTER))
                .build());
      } else {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getName())
                .type(createColumnFilterInputObjectType(col))
                .build());
      }
    }
    return filterBuilder.build();
  }

  private static GraphQLInputObjectType createTableOrderByInputObjectType(Table table) {
    GraphQLInputObjectType.Builder orderByBuilder =
        newInputObject().name(table.getName() + ORDERBY);
    for (Column col : table.getMetadata().getColumns()) {
      ColumnType type = col.getColumnType();
      if (!REF_ARRAY.equals(type) && !REFBACK.equals(type)) {
        orderByBuilder.field(newInputObjectField().name(col.getName()).type(orderByEnum));
      }
    }
    return orderByBuilder.build();
  }

  // cache so we can reuse filter input types between tables
  static Map<ColumnType, GraphQLInputObjectType> filterInputTypes = new LinkedHashMap<>();

  private static GraphQLInputObjectType createColumnFilterInputObjectType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (filterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          newInputObject().name("Molgenis" + typeName + FILTER);
      for (Operator operator : type.getOperators()) {
        builder.field(
            newInputObjectField()
                .name(operator.getName())
                .type(GraphQLList.list(graphQLTypeOf(column))));
      }
      filterInputTypes.put(type, builder.build());
    }
    return filterInputTypes.get(type);
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
      case DATE_ARRAY:
      case DATE:
      case DATETIME:
      case DATETIME_ARRAY:
      case STRING:
      case TEXT:
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case UUID:
      case UUID_ARRAY:
      case REF:
      case REF_ARRAY:
      case REFBACK:
      case MREF:
    }
    return Scalars.GraphQLString;
  }

  private static FilterBean[] convertMapToFilterArray(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      Column c = table.getMetadata().getColumn(entry.getKey());
      if (c == null)
        throw new GraphqlException(
            "Graphql API error",
            "Column " + entry.getKey() + " unknown in table " + table.getName());
      ColumnType type = c.getColumnType();
      if (REF.equals(type) || REF_ARRAY.equals(type) || REFBACK.equals(type)) {
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
    return subFilters.toArray(new FilterBean[subFilters.size()]);
  }

  private static Filter convertMapToFilter(String name, Map<String, Object> subFilter) {
    Filter f = f(name);
    for (Map.Entry<String, Object> entry2 : subFilter.entrySet()) {
      Operator op = Operator.fromAbbreviation(entry2.getKey());
      if (entry2.getValue() instanceof List) {
        f.add(op, (List) entry2.getValue());
      } else {
        f.add(op, entry2.getValue());
      }
    }
    return f;
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
          if (args.containsKey(LIMIT)) {
            sc.setLimit((int) args.get(LIMIT));
          }
          if (args.containsKey(OFFSET)) {
            sc.setOffset((int) args.get(OFFSET));
          }
          if (args.containsKey(ORDERBY)) {
            sc.setOrderBy((Map<String, Order>) args.get(ORDERBY));
          }
          result.add(sc);
        }
    }
    return result.toArray(new SelectColumn[result.size()]);
  }
}
