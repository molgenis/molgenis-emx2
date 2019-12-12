package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Member;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.GraphqlApi.typeForMutationResult;
import static org.molgenis.emx2.web.JsonApi.jsonToSchema;
import static org.molgenis.emx2.web.JsonApi.schemaToJson;

public class GraphqlTableMetadataFields {

  public static GraphQLFieldDefinition.Builder tableMetatadataQueryField(Schema schema) {
    return newFieldDefinition()
        .name("_meta")
        .type(outputMetadataType)
        .dataFetcher(GraphqlTableMetadataFields.queryFetcher(schema));
  }

  public static GraphQLFieldDefinition.Builder tableMetadataMutationField(Schema schema) {
    return newFieldDefinition()
        .name("alterMetadata")
        .type(typeForMutationResult)
        .dataFetcher(GraphqlTableMetadataFields.mutationFetcher(schema))
        .argument(GraphQLArgument.newArgument().name(Constants.INPUT).type(inputMetadataType));
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
          .field(newInputObjectField().name("refTableName").type(Scalars.GraphQLString))
          .field(newInputObjectField().name("refColumnName").type(Scalars.GraphQLString))
          .build();

  private static GraphQLInputObjectType inputTableMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisTableInput")
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              newInputObjectField()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(
              newInputObjectField().name("columns").type(GraphQLList.list(inputColumnMetadataType)))
          .build();

  private static GraphQLInputObjectType inputMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMetaInput")
          .field(newInputObjectField().name(TABLES).type(GraphQLList.list(inputTableMetadataType)))
          .field(
              newInputObjectField().name(MEMBERS).type(GraphQLList.list(inputMembersMetadataType)))
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
          .field(newFieldDefinition().name("refTableName").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("refColumnName").type(Scalars.GraphQLString))
          .build();
  private static final GraphQLObjectType outputTableMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)))
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

  private static DataFetcher<?> mutationFetcher(Schema model) {
    return dataFetchingEnvironment -> {
      try {
        Map<String, Object> metaInput = dataFetchingEnvironment.getArgument(Constants.INPUT);
        model.tx(
            db -> {
              try {
                if (metaInput.containsKey(TABLES)) {
                  String json = JsonApi.getWriter().writeValueAsString(metaInput);
                  SchemaMetadata otherSchema = jsonToSchema(json);
                  model.merge(otherSchema);
                }
                if (metaInput.containsKey(MEMBERS)) {
                  List<Map<String, String>> members = (List) metaInput.get(MEMBERS);
                  for (Map<String, String> m : members) {
                    model.addMember(m.get("user"), m.get("role"));
                  }
                }
              } catch (IOException e) {
                throw new GraphqlApiException(e);
              }
            });
        Map result = new LinkedHashMap<>();
        result.put(DETAIL, "success");
        return result;
      } catch (MolgenisException e) {
        return GraphqlApi.transform(e);
      }
    };
  }
}
