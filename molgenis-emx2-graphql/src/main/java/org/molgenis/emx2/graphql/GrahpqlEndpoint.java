package org.molgenis.emx2.graphql;

import graphql.schema.*;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLSchema.newSchema;

public class GrahpqlEndpoint {

  GraphQLSchema getSchema(SchemaMetadata model) throws MolgenisException {
    GraphQLSchema.Builder schema = newSchema();

    GraphQLObjectType.Builder query = newObject().name("QueryOld");
    for (String tableName : model.getTableNames()) {
      TableMetadata table = model.getTableMetadata(tableName);
      GraphQLObjectType.Builder type = newObject().name(table.getTableName());
      for (Column col : table.getColumns()) {
        type.field(newFieldDefinition().name(col.getColumnName()).type(GraphQLString));
      }
      query.field(
          GraphQLFieldDefinition.newFieldDefinition()
              .name(table.getTableName())
              .type(GraphQLList.list(type.build()))
              .argument(newArgument().name("where").type(GraphQLString).build())
              .argument(newArgument().name("limit").type(GraphQLInt).build())
              .argument(newArgument().name("offset").type(GraphQLInt).build())
              .build());
    }

    schema.query(query.build());
    return schema.build();
  }
}
