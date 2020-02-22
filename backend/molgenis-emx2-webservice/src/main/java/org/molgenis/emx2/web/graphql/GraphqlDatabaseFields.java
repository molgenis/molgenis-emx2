package org.molgenis.emx2.web.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import org.molgenis.emx2.Database;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.Constants.NAME;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.typeForMutationResult;

public class GraphqlDatabaseFields {

  private GraphqlDatabaseFields() {
    // no instances
  }

  public static GraphQLFieldDefinition.Builder deleteSchemaField(Database database) {
    return newFieldDefinition()
        .name("deleteSchema")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(NAME).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument("name");
              database.dropSchema(name);
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s dropped", name);
            });
  }

  public static GraphQLFieldDefinition.Builder createSchemaField(Database database) {
    return newFieldDefinition()
        .name("createSchema")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(NAME).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument("name");
              database.createSchema(name);
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s created", name);
            });
  }

  public static GraphQLFieldDefinition.Builder querySchemasField(Database database) {
    return newFieldDefinition()
        .name("Schemas")
        .dataFetcher(
            dataFetchingEnvironment -> {
              List<Map<String, String>> result = new ArrayList<>();
              for (String name : database.getSchemaNames()) {
                result.add(Map.of("name", name));
              }
              return result;
            })
        .type(
            GraphQLList.list(
                newObject()
                    .name("Schema")
                    .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString).build())
                    .build()));
  }
}
