package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.io.submission.SubmissionService.TARGET_TABLES;

import graphql.Scalars;
import graphql.schema.*;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.io.submission.SubmissionService;

public class GraphqlSubmissionFieldFactory {

  private static final GraphQLObjectType submissionsListOutput =
      new GraphQLObjectType.Builder()
          .name("MolgenisSubmissionOutputType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SubmissionService.STATUS)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SubmissionService.TARGET_SCHEMA)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TARGET_TABLES)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SubmissionService.CREATED)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SubmissionService.CHANGED)
                  .type(Scalars.GraphQLString))
          .build();

  public static final String TARGET_IDENTIFIERS_JSON = "targetIdentifiers";
  public static final String CREATE = "create";
  public static final String MERGE = "merge";

  private final GraphQLInputObjectType inputSubmissionCreate =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisSubmissionCreateInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TARGET_TABLES)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TARGET_IDENTIFIERS_JSON)
                  .type(Scalars.GraphQLString))
          .build();

  public GraphQLFieldDefinition.Builder submissionsMutation(SubmissionService submissionService) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_submissions")
        .argument(GraphQLArgument.newArgument().name(CREATE).type(inputSubmissionCreate))
        .argument(GraphQLArgument.newArgument().name(MERGE).type(Scalars.GraphQLString))
        .type(typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              if (dataFetchingEnvironment.getArgument(CREATE) != null) {
                Map<String, Object> params = dataFetchingEnvironment.getArgument(CREATE);
                return new GraphqlApiMutationResult(
                    submissionService.createSubmission(
                        (List<String>) params.get(TARGET_TABLES),
                        (String) params.get(TARGET_IDENTIFIERS_JSON)));
              } else if (dataFetchingEnvironment.getArgument(MERGE) != null) {
                return new GraphqlApiMutationResult(
                    submissionService.mergeSubmission(dataFetchingEnvironment.getArgument(MERGE)));
              }
              return null;
            });
  }

  public GraphQLFieldDefinition.Builder submissionsQuery(SubmissionService submissionService) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_submissions")
        .type(GraphQLList.list(submissionsListOutput))
        .dataFetcher(environment -> submissionService.list());
  }
}
