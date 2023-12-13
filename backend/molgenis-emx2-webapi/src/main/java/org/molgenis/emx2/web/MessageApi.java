package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static spark.Spark.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.parser.Parser;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.email.EmailMessage;
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

  @SuppressWarnings("VulnerableCodeUsages")
  private static final Parser parser = new Parser();

  private MessageApi() {
    // hide constructor
  }

  public static void create() {
    post("/:schema/api/message/*", ACCEPT_JSON, MessageApi::send);
  }

  public static String send(Request request, Response response) {
    logger.info("Received message request");
    Schema schema = MolgenisWebservice.getSchema(request);
    if (schema == null) {
      String msg = "Cannot handle send action, schema is null";
      logger.error(msg);
      throw new MolgenisException(msg);
    }

    ObjectMapper objectMapper = new ObjectMapper();
    SendMessageAction sendMessageAction;
    Map<String, Object> validationFilter;
    try {
      sendMessageAction = objectMapper.readValue(request.body(), SendMessageAction.class);
      validationFilter = objectMapper.readValue(sendMessageAction.recipientsFilter(), Map.class);
    } catch (JsonProcessingException e) {
      response.status(422);
      String msg = "Error parsing request: " + e.getMessage();
      logger.error(msg);
      return msg;
    }

    String recipientsQuery = schema.getSettingValue(Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY);

    try {
      parser.parseDocument(recipientsQuery);
    } catch (Exception e) {
      String msg =
          "Cannot handle send action, schema setting "
              + Constants.CONTACT_RECIPIENTS_QUERY_SETTING_KEY
              + " is not a valid graphql query";
      logger.error(msg);
      throw new MolgenisException(msg);
    }

    MolgenisSession session = sessionManager.getSession(request);
    GraphQL gql = session.getGraphqlForSchema(schema.getName());

    final ExecutionResult executionResult =
        gql.execute(ExecutionInput.newExecutionInput(recipientsQuery).variables(validationFilter));
    if (!executionResult.getErrors().isEmpty()) {
      response.status(500);
      String msg =
          "Error validating message receivers: " + executionResult.getErrors().get(0).getMessage();
      logger.error(msg);
      return msg;
    }
    Map<String, Object> resultMap = executionResult.toSpecification();

    final List<String> recipients = new java.util.ArrayList<>(Collections.emptyList());
    try {
      recipients.addAll(EmailValidator.validationResponseToReceivers(resultMap));
    } catch (Exception e) {
      response.status(500);
      String msg = "Error validating message receivers: " + e.getMessage();
      logger.error(msg);
      return msg;
    }

    if (recipients.isEmpty()) {
      response.status(500);
      String msg = "No recipients found for given filter";
      logger.error(msg);
      return msg;
    }

    logger.info(
        "Sending message to recipients: {} with subject: {} and message: {}",
        recipients,
        sendMessageAction.subject(),
        sendMessageAction.body());

    // send email to all recipients on allow list
    EmailSettings settings = loadEmailSettings(schema);
    EmailService emailService = new EmailService(settings);

    Optional<String> bccRecipient =
        schema.hasSetting(Constants.CONTACT_BCC_ADDRESS)
            ? Optional.ofNullable(schema.getSettingValue(Constants.CONTACT_BCC_ADDRESS))
            : Optional.empty();
    EmailMessage message =
        new EmailMessage(
            recipients, sendMessageAction.subject(), sendMessageAction.body(), bccRecipient);
    final boolean sendResult = emailService.send(message);
    if (!sendResult) {
      response.status(500);
      logger.error(
          "failed to send message to recipients: {} with subject: {} and message: {}",
          recipients,
          sendMessageAction.subject(),
          sendMessageAction.body());
    }
    return String.valueOf(sendResult);
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
