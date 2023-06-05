package org.molgenis.emx2.email;

import static org.junit.jupiter.api.Assertions.*;

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
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_SMTP_SERVER", matches = ".*")
  @EnabledIfEnvironmentVariable(named = "EMX2_TEST_SMTP_RECEIVER", matches = ".*")
  // note set env vars to test sending a mail via smtp server
  public void sendEmail() {
    EmailSettings.EmailSettingsBuilder builder = new EmailSettings.EmailSettingsBuilder();
    builder.senderEmail(System.getenv("EMX2_TEST_EMAIL_SENDER"));
    builder.auth("true");
    builder.smtpAuthenticatorSenderPassword(System.getenv("EMX2_TEST_EMAIL_PASSWORD"));
    builder.host(System.getenv("EMX2_TEST_SMTP_SERVER"));
    builder.port("587");
    builder.debug("true");
    EmailSettings settings = builder.build();
    EmailService emailService = new EmailService(settings);
    boolean isSuccess =
        emailService.send(
            System.getenv("EMX2_TEST_SMTP_RECEIVER"),
            "This is just a test",
            "This is a test mail from EMX2");
    assertTrue(isSuccess);
  }
}
