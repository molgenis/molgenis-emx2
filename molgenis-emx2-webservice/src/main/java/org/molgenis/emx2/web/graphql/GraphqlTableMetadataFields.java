package org.molgenis.emx2.web.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Member;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.web.JsonApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.web.JsonApi.jsonToSchema;
import static org.molgenis.emx2.web.JsonApi.schemaToJson;

public class GraphqlTableMetadataFields {
  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_COLUMN_NAME = "refColumn";
  public static final String MAPPED_BY = "mappedBy";

  private GraphqlTableMetadataFields() {
    // hide constructor
  }

  public static GraphQLFieldDefinition.Builder metaField(Schema schema) {
    return newFieldDefinition()
        .name("_meta")
        .type(outputMetadataType)
        .dataFetcher(GraphqlTableMetadataFields.queryFetcher(schema));
  }

  public static GraphQLFieldDefinition saveMetaField(Schema schema) {
    return newFieldDefinition()
        .name("saveMeta")
        .type(typeForMutationResult)
        .dataFetcher(saveMetaFetcher(schema))
        .argument(newArgument().name(TABLES).type(GraphQLList.list(inputTableMetadataType)))
        .argument(newArgument().name(MEMBERS).type(GraphQLList.list(inputMembersMetadataType)))
        .build();
  }

  public static GraphQLFieldDefinition deleteMetaField(Schema schema) {
    return newFieldDefinition()
        .name("deleteMeta")
        .type(typeForMutationResult)
        .dataFetcher(deleteMetaFetcher(schema))
        .argument(newArgument().name(TABLES).type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(newArgument().name(MEMBERS).type(GraphQLList.list(Scalars.GraphQLString)))
        .build();
  }

  private static GraphQLInputObjectType inputMembersMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMembersInput")
          .field(newInputObjectField().name("user").type(Scalars.GraphQLString))
          .field(newInputObjectField().name("role").type(Scalars.GraphQLString))
          .build();

  private static GraphQLInputObjectType inputColumnMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisColumnInput")
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name("columnType").type(Scalars.GraphQLString))
          .field(newInputObjectField().name("pkey").type(Scalars.GraphQLBoolean))
          .field(newInputObjectField().name("nullable").type(Scalars.GraphQLBoolean))
          .field(newInputObjectField().name(REF_TABLE_NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(REF_COLUMN_NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(MAPPED_BY).type(Scalars.GraphQLString))
          .build();

  private static GraphQLInputObjectType inputTableMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisTableInput")
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name("pkey").type(Scalars.GraphQLString))
          .field(
              newInputObjectField()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(
              newInputObjectField().name("columns").type(GraphQLList.list(inputColumnMetadataType)))
          .build();

  // medatadata
  private static final GraphQLType outputRolesMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisRolesType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLType outputMembersMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMembersType")
          .field(newFieldDefinition().name("user").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("role").type(Scalars.GraphQLString))
          .build();

  private static final GraphQLObjectType outputColumnMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisColumnType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("columnType").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("pkey").type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name("nullable").type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name(REF_TABLE_NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(REF_COLUMN_NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(MAPPED_BY).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("validation").type(Scalars.GraphQLString))
          .build();

  private static final GraphQLObjectType outputTableMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("pkey").type(Scalars.GraphQLString))
          .field(
              newFieldDefinition()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(
              newFieldDefinition().name("columns").type(GraphQLList.list(outputColumnMetadataType)))
          .build();

  private static final GraphQLObjectType outputMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMetaType")
          .field(newFieldDefinition().name(TABLES).type(GraphQLList.list(outputTableMetadataType)))
          .field(
              newFieldDefinition().name(MEMBERS).type(GraphQLList.list(outputMembersMetadataType)))
          .field(newFieldDefinition().name("roles").type(GraphQLList.list(outputRolesMetadataType)))
          .build();

  private static DataFetcher<?> queryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // add tables
      String json = schemaToJson(schema.getMetadata());
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      // add members
      List<Map<String, String>> members = new ArrayList<>();
      for (Member m : schema.getMembers()) {
        members.add(Map.of("user", m.getUser(), "role", m.getRole()));
      }
      result.put(MEMBERS, members);

      // add roles
      List<Map<String, String>> roles = new ArrayList<>();
      for (String role : schema.getRoles()) {
        roles.add(Map.of(NAME, role));
      }
      result.put("roles", roles);

      return result;
    };
  }

  private static DataFetcher<?> saveMetaFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            try {
              Object tables = dataFetchingEnvironment.getArgument(TABLES);
              if (tables != null) {
                Map tableMap = Map.of("tables", tables);
                String json = JsonApi.getWriter().writeValueAsString(tableMap);
                SchemaMetadata otherSchema = jsonToSchema(json);
                schema.merge(otherSchema);
              }
              List<Map<String, String>> members = dataFetchingEnvironment.getArgument(MEMBERS);
              if (members != null) {
                for (Map<String, String> m : members) {
                  schema.addMember(m.get("user"), m.get("role"));
                }
              }
            } catch (IOException e) {
              throw new GraphqlException("Save metadata failed", e);
            }
          });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  private static DataFetcher<?> deleteMetaFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            List<String> tables = dataFetchingEnvironment.getArgument(TABLES);
            if (tables != null) {
              for (String tableName : tables) {
                schema.dropTable(tableName);
              }
            }
            List<String> members = dataFetchingEnvironment.getArgument(MEMBERS);
            if (members != null) {
              for (String name : members) {
                schema.removeMember(name);
              }
            }
          });
      Map result = new LinkedHashMap<>();
      result.put(DETAIL, "success");
      return result;
    };
  }
}
