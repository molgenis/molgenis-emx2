package org.molgenis.emx2.web;

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
import static org.molgenis.emx2.web.GraphqlApi.*;
import static org.molgenis.emx2.web.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.GraphqlApiMutationResult.typeForMutationResult;

public class GraphqlTableMutationFields {
  public static GraphQLFieldDefinition tableMutationField(Schema schema) {

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
      inputBuilder.field(newInputObjectField().name(col.getName()).type(type));
    }
    return inputBuilder.build();
  }
}
