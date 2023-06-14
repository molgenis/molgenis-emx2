package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.parser.Parser;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.email.EmailService;
import org.molgenis.emx2.email.EmailSettings;
import org.molgenis.emx2.email.EmailValidator;
import org.molgenis.emx2.web.actions.SendMessageAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class MessageApi {

  private static final Logger logger = LoggerFactory.getLogger(MessageApi.class);
  private static final Parser parser = new Parser();

  public static String send(Request request, Response response) {
    logger.info("Received message request");
    Schema schema = MolgenisWebservice.getSchema(request);
    if (schema == null) {
      throw new MolgenisException("Cannot handle send action, schema is null");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    SendMessageAction sendMessageAction;
    Map<String, Object> validationFilter;
    try {
      sendMessageAction = objectMapper.readValue(request.body(), SendMessageAction.class);
      validationFilter = objectMapper.readValue(sendMessageAction.recipientsFilter(), Map.class);
    } catch (JsonProcessingException e) {
      response.status(422);
      return "Error parsing request: " + e.getMessage();
    }

    String recipientsQuery = schema.getSettingValue(Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY);

    try {
      parser.parseDocument(recipientsQuery);
    } catch (Exception e) {
      throw new MolgenisException(
          "Cannot handle send action, schema setting "
              + Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY
              + " is not a valid graphql query");
    }

    MolgenisSession session = sessionManager.getSession(request);
    GraphQL gql = session.getGraphqlForSchema(schema.getName());

    final ExecutionResult executionResult =
        gql.execute(ExecutionInput.newExecutionInput(recipientsQuery).variables(validationFilter));
    if (!executionResult.getErrors().isEmpty()) {
      response.status(500);
      return "Error parsing request: " + executionResult.getErrors().get(0).getMessage();
    }
    Map<String, Object> resultMap = executionResult.toSpecification();

    final List<String> recipients = new java.util.ArrayList<>(Collections.emptyList());
    try {
      recipients.addAll(EmailValidator.validationResponseToRecievers(resultMap));
    } catch (Exception e) {
      response.status(500);
      return "Error parsing request: " + e.getMessage();
    }

    if (recipients.isEmpty()) {
      response.status(400);
      return "No recipients found for given filter";
    }

    // send email to all recipients on allow list
    EmailSettings settings = loadEmailSettings(schema);
    EmailService emailService = new EmailService(settings);

    final Boolean sendResult =
        emailService.send(recipients, sendMessageAction.subject(), sendMessageAction.body());
    return sendResult.toString();
  }

  private static EmailSettings loadEmailSettings(Schema schema) {
    EmailSettings.EmailSettingsBuilder builder = new EmailSettings.EmailSettingsBuilder();

    setEmailSetting(schema, "EMAIL_HOST", builder::host);
    setEmailSetting(schema, "EMAIL_PORT", builder::port);
    setEmailSetting(schema, "EMAIL_START_TTLS_ENABLE", builder::starttlsEnable);
    setEmailSetting(schema, "EMAIL_SSL_PROTOCOLS", builder::sslProtocols);
    setEmailSetting(schema, "EMAIL_SOCKET_FACTORY_PORT", builder::socketFactoryPort);
    setEmailSetting(schema, "EMAIL_SOCKET_FACTORY_CLASS", builder::socketFactoryClass);
    setEmailSetting(schema, "EMAIL_SOCKET_FACTORY_FALLBACK", builder::socketFactoryFallback);
    setEmailSetting(schema, "EMAIL_DEBUG", builder::debug);
    setEmailSetting(schema, "EMAIL_AUTH", builder::auth);
    setEmailSetting(schema, "EMAIL_SENDER_EMAIL", builder::senderEmail);
    setEmailSetting(
        schema,
        "EMAIL_SMTP_AUTHENTICATOR_SENDER_PASSWORD",
        builder::smtpAuthenticatorSenderPassword);

    return builder.build();
  }

  private static void setEmailSetting(
      Schema schema, String setting, Function<String, EmailSettings.EmailSettingsBuilder> method) {
    if (schema.hasSetting(setting)) {
      method.apply(schema.getSettingValue(setting));
    }
  }
}
