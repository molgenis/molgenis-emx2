package org.molgenis.emx2.graphql;

import graphql.schema.*;
import org.molgenis.MolgenisException;
import org.molgenis.metadata.ColumnMetadata;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;

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
      GraphQLObjectType.Builder type = newObject().name(table.getName());
      for (ColumnMetadata col : table.getColumns()) {
        type.field(newFieldDefinition().name(col.getName()).type(GraphQLString));
      }
      query.field(
          GraphQLFieldDefinition.newFieldDefinition()
              .name(table.getName())
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
