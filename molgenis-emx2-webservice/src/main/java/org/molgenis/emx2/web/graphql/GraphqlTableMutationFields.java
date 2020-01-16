package org.molgenis.emx2.web.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;

import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static org.molgenis.emx2.utils.TypeUtils.getPrimitiveColumnType;
import static org.molgenis.emx2.web.graphql.GraphqlApi.*;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.typeForMutationResult;

class GraphqlTableMutationFields {
  private GraphqlTableMutationFields() {
    // hide
  }

  static GraphQLFieldDefinition saveField(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        newFieldDefinition()
            .name("save")
            .type(typeForMutationResult)
            .dataFetcher(fetcherForSave(schema));

    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      GraphQLInputObjectType inputType = createTableInputType(table);
      fieldBuilder.argument(
          GraphQLArgument.newArgument().name(tableName).type(GraphQLList.list(inputType)));
    }
    return fieldBuilder.build();
  }

  static GraphQLFieldDefinition deleteField(Schema schema) {
    GraphQLFieldDefinition.Builder fieldBuilder =
        newFieldDefinition()
            .name("delete")
            .type(typeForMutationResult)
            .dataFetcher(fetcherForDelete(schema));

    for (String tableName : schema.getTableNames()) {
      fieldBuilder.argument(
          GraphQLArgument.newArgument()
              .name(tableName)
              .type(GraphQLList.list(GraphQLTypeReference.typeRef(tableName + "Input"))));
    }
    return fieldBuilder.build();
  }

  private static DataFetcher fetcherForSave(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuilder result = new StringBuilder();
      for (String tableName : schema.getTableNames()) {
        List<Map<String, Object>> rowsAslistOfMaps = dataFetchingEnvironment.getArgument(tableName);
        if (rowsAslistOfMaps != null) {
          Table table = schema.getTable(tableName);
          int count = table.update(convertToRows(rowsAslistOfMaps));
          result.append("saved " + count + " records to " + tableName + "\n");
        }
      }
      return new GraphqlApiMutationResult(SUCCESS, result.toString());
    };
  }

  private static DataFetcher<?> fetcherForDelete(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuilder result = new StringBuilder();
      for (String tableName : schema.getTableNames()) {
        List<Map<String, Object>> rowsAslistOfMaps = dataFetchingEnvironment.getArgument(tableName);
        if (rowsAslistOfMaps != null) {
          Table table = schema.getTable(tableName);
          int count = table.delete(convertToRows(rowsAslistOfMaps));
          result.append("deleted " + count + " records int " + tableName + "\n");
        }
      }
      return new GraphqlApiMutationResult(SUCCESS, result.toString());
    };
  }

  private static GraphQLInputObjectType createTableInputType(Table table) {
    GraphQLInputObjectType.Builder inputBuilder = newInputObject().name(table.getName() + "Input");
    for (Column col : table.getMetadata().getColumns()) {
      ColumnType columnType = getPrimitiveColumnType(col);
      GraphQLInputType type = getGraphQLInputType(columnType);
      if (col.isPrimaryKey() || !col.isNullable()) {
        type = GraphQLNonNull.nonNull(type);
      }
      inputBuilder.field(newInputObjectField().name(col.getName()).type(type));
    }
    return inputBuilder.build();
  }

  private static GraphQLInputType getGraphQLInputType(ColumnType columnType) {
    switch (columnType) {
      case BOOL:
        return Scalars.GraphQLBoolean;
      case INT:
        return Scalars.GraphQLInt;
      case DECIMAL:
        return Scalars.GraphQLFloat;
      case UUID:
      case STRING:
      case TEXT:
      case DATE:
      case DATETIME:
        return Scalars.GraphQLString;
      case BOOL_ARRAY:
        return GraphQLList.list(Scalars.GraphQLBoolean);
      case INT_ARRAY:
        return GraphQLList.list(Scalars.GraphQLInt);
      case DECIMAL_ARRAY:
        return GraphQLList.list(Scalars.GraphQLFloat);
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case DATE_ARRAY:
      case DATETIME_ARRAY:
      case UUID_ARRAY:
        return GraphQLList.list(Scalars.GraphQLString);
      default:
        throw new MolgenisException(
            "Internal error", "Type " + columnType + " not expected at this place");
    }
  }
}
