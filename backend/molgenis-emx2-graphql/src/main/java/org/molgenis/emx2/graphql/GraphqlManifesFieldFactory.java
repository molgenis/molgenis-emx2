package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import java.util.HashMap;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Version;

public class GraphqlManifesFieldFactory {

  public static final String IMPLEMENTATION_VERSION = "ImplementationVersion";
  public static final String SPECIFICATION_VERSION = "SpecificationVersion";
  public static final String DATABASE_VERSION = "DatabaseVersion";

  public GraphQLFieldDefinition queryVersionField(Database db) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_manifest")
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new HashMap<>();
              result.put(IMPLEMENTATION_VERSION, Version.getImplementationVersion());
              result.put(SPECIFICATION_VERSION, Version.getSpecificationVersion());
              result.put(DATABASE_VERSION, db.getDatabaseVersion());
              return result;
            })
        .type(
            GraphQLObjectType.newObject()
                .name("MolgenisManifest")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(IMPLEMENTATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SPECIFICATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(DATABASE_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .build())
        .build();
  }
}
