package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.List;
import java.util.Map;
import org.javers.common.collections.Maps;
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

  private static String validationQuery =
      """
query Resources($filter:ResourcesFilter){
  Resources(filter:$filter) {
    contacts {
      email
    }
  }
}
""";

  private static final Map<String, Object> validationFilter =
      Maps.of("filter", "{\"name\":{\"equals\":\"EU Child Cohort Network\"}}");

  public static void create() {
    post("/api/email/*", "application/json", EmailApi::send);
  }

  private static String send(Request request, Response response) {

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
    GraphQL gql = session.getGraphqlForSchema("catalogue-demo");

    final ExecutionResult executionResult =
        gql.execute(ExecutionInput.newExecutionInput(validationQuery).variables(validationFilter));
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
