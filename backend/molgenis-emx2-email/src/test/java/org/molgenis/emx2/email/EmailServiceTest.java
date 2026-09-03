package org.molgenis.emx2.email;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import javax.mail.internet.AddressException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class EmailServiceTest {

  @Test
  public void createMailerService() {
    EmailSettings.EmailSettingsBuilder builder = new EmailSettings.EmailSettingsBuilder();
    EmailSettings settings = builder.build();
    EmailService emailService = new EmailService(settings);
    assertNotNull(emailService);
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_EMAIL_SENDER", matches = ".*")
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_EMAIL_PASSWORD", matches = ".*")
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_EMAIL_BCC_ADDRESS", matches = ".*")
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_SMTP_SERVER", matches = ".*")
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_SMTP_RECEIVER", matches = ".*")
  // note set env vars to test sending a mail via smtp server
  public void sendEmail() throws AddressException {
    EmailSettings.EmailSettingsBuilder builder = new EmailSettings.EmailSettingsBuilder();
    builder.senderEmail(System.getenv("EMX2_TEST_EMAIL_SENDER"));
    builder.auth("true");
    builder.smtpAuthenticatorSenderPassword(System.getenv("EMX2_TEST_EMAIL_PASSWORD"));
    builder.host(System.getenv("EMX2_TEST_SMTP_SERVER"));
    builder.port("587");
    builder.debug("true");
    EmailSettings settings = builder.build();
    EmailService emailService = new EmailService(settings);
    String bccAddress = System.getenv("EMX2_TEST_EMAIL_BCC_ADDRESS");
    EmailMessage message =
        new EmailMessage(
            Collections.singletonList(System.getenv("EMX2_TEST_SMTP_RECEIVER")),
            "This is just a test",
            "This is a test mail from EMX2",
            Optional.of(bccAddress));
    boolean isSuccess = emailService.send(message);
    assertTrue(isSuccess);
  }

  @Test
  public void sendGivesUpWhenTheServerNeverReplies() throws IOException {
    try (ServerSocket silentServer = new ServerSocket(0)) {
      EmailSettings settings =
          new EmailSettings.EmailSettingsBuilder()
              .host("127.0.0.1")
              .port(String.valueOf(silentServer.getLocalPort()))
              .connectionTimeout("200")
              .readTimeout("200")
              .build();
      EmailService emailService = new EmailService(settings);
      EmailMessage message =
          new EmailMessage(
              Collections.singletonList("test@test.com"), "subject", "text", Optional.empty());

      assertFalse(
          assertTimeoutPreemptively(Duration.ofSeconds(5), () -> emailService.send(message)));
    }
  }

  @Test
  public void defaultsBoundTheWaitOnAnUnresponsiveServer() {
    EmailSettings settings = new EmailSettings.EmailSettingsBuilder().build();
    assertTrue(Integer.parseInt(settings.getConnectionTimeout()) > 0);
    assertTrue(Integer.parseInt(settings.getReadTimeout()) > 0);
  }
}
