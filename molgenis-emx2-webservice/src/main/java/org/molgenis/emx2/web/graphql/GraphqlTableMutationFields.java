package org.molgenis.emx2.web.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.sql.SqlTypeUtils;

import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static org.molgenis.emx2.ColumnType.REF;
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
      GraphQLInputType type;
      ColumnType columnType = col.getColumnType();
      if (REF.equals(columnType)) columnType = SqlTypeUtils.getRefColumnType(col);
      switch (columnType) {
        case DECIMAL:
          type = Scalars.GraphQLBigDecimal;
          break;
        case INT:
          type = Scalars.GraphQLInt;
          break;
        default:
          type = Scalars.GraphQLString;
          break;
      }
      if (col.isPrimaryKey()) {
        type = GraphQLNonNull.nonNull(type);
      }
      inputBuilder.field(newInputObjectField().name(col.getName()).type(type));
    }
    return inputBuilder.build();
  }
}
