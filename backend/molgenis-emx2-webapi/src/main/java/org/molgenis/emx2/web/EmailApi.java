package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.parser.Parser;
import java.util.List;
import java.util.Map;
import org.javers.common.collections.Maps;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.email.EmailService;
import org.molgenis.emx2.email.EmailSettings;
import org.molgenis.emx2.email.EmailValidator;
import org.molgenis.emx2.web.actions.SendMailAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class EmailApi {

  private static final Logger logger = LoggerFactory.getLogger(EmailApi.class);

  private static final Map validationFilter =
      Maps.of("filter", Maps.of("name", Maps.of("equals", "EU Child Cohort Network")));

  public static void create() {
    post("/api/email/*", "application/json", EmailApi::send);
  }

  public static String send(Request request, Response response) {
    logger.info("Received email request");
    Schema schema = MolgenisWebservice.getSchema(request);
    if (schema == null) {
      throw new MolgenisException("Cannot handle send action, schema is null");
    }

    String recipientsQuery =
        schema.getMetadata().getSetting(Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY);
    if (recipientsQuery == null) {
      throw new MolgenisException(
          "Cannot handle send action, schema does not have setting "
              + Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY);
    }

    graphql.parser.Parser graphQLParser = new Parser();
    try {
      graphQLParser.parseDocument(recipientsQuery);
    } catch (Exception e) {
      throw new MolgenisException(
          "Cannot handle send action, schema setting "
              + Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY
              + " is not a valid graphql query");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    SendMailAction sendMailAction = null;
    try {
      // parse the request
      sendMailAction = objectMapper.readValue(request.body(), SendMailAction.class);
    } catch (JsonProcessingException e) {
      response.status(500); // internal server error
      return "Error parsing request: " + e.getMessage();
    }

    // query for allow list
    MolgenisSession session = sessionManager.getSession(request);
    GraphQL gql = session.getGraphqlForSchema(schema.getName());

    //    Map<String, Object> node = new ObjectMapper().readValue(request.body(), Map.class);
    //    return (Map<String, Object>) node.get(VARIABLES);

    final ExecutionResult executionResult =
        gql.execute(ExecutionInput.newExecutionInput(recipientsQuery).variables(validationFilter));
    // todo check for errors and return on error
    Map<String, Object> resultMap = executionResult.toSpecification();

    // todo try catch the whole parsing and return on error
    final List recipients = EmailValidator.validationResponseToRecievers(resultMap);

    // send email to all recipients on allow list
    EmailSettings.EmailSettingsBuilder builder = new EmailSettings.EmailSettingsBuilder();
    EmailSettings settings = builder.build();
    EmailService emailService = new EmailService(settings);

    final Boolean sendResult =
        emailService.send(recipients, sendMailAction.subject(), sendMailAction.body());
    return sendResult.toString();
  }
}
