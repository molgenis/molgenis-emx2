package org.molgenis.emx2.web;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.MolgenisException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.GraphqlTypes.typeForMutationResult;

public class GraphqlApiForDatabase {

  private Database database;
  private final String NAME = "name";

  public GraphqlApiForDatabase(Database database) {
    this.database = database;
  }

  public ExecutionResult execute(String query) {
    return baseSchema.execute(query);
  }

  // crazy code, but it seems to be meant like this :-)
  private final GraphQL baseSchema =
      GraphQL.newGraphQL(
              GraphQLSchema.newSchema()
                  .query(
                      newObject()
                          .name("Query")
                          .field(
                              newFieldDefinition()
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
                                              .field(
                                                  newFieldDefinition()
                                                      .name(NAME)
                                                      .type(Scalars.GraphQLString)
                                                      .build())
                                              .build()))))
                  .mutation(
                      newObject()
                          .name("Mutation")
                          .field(
                              newFieldDefinition()
                                  .name("createSchema")
                                  .type(typeForMutationResult)
                                  .argument(
                                      GraphQLArgument.newArgument()
                                          .name(NAME)
                                          .type(Scalars.GraphQLString))
                                  .dataFetcher(
                                      dataFetchingEnvironment -> {
                                        String name = dataFetchingEnvironment.getArgument("name");
                                        try {
                                          database.createSchema(name);
                                          return Map.of("detail", "schema created");
                                        } catch (MolgenisException e) {
                                          return Map.of("detail", e.getDetail());
                                        }
                                      }))
                          .field(
                              newFieldDefinition()
                                  .name("deleteSchema")
                                  .type(typeForMutationResult)
                                  .argument(
                                      GraphQLArgument.newArgument()
                                          .name(NAME)
                                          .type(Scalars.GraphQLString))
                                  .dataFetcher(
                                      dataFetchingEnvironment -> {
                                        String name = dataFetchingEnvironment.getArgument("name");
                                        try {
                                          database.dropSchema(name);
                                          return Map.of("detail", "schema dropped");
                                        } catch (MolgenisException e) {
                                          return Map.of("detail", e.getDetail());
                                        }
                                      })))
                  .build())
          .build();
}
