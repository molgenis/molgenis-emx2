package org.molgenis.emx2.email;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {

  private final String senderEmail;
  private final String senderPassword;

  private final Properties props = new Properties();

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  public EmailService(EmailSettings settings) {

    this.props.put("mail.smtp.auth", settings.getAuth());
    this.props.put("mail.smtp.host", settings.getHost());
    this.props.put("mail.smtp.starttls.enable", settings.getStarttlsEnable());
    this.props.put("mail.smtp.port", settings.getPort());
    this.props.put("mail.smtp.ssl.protocols", settings.getSslProtocols());
    this.props.put("mail.smtp.socketFactory.port", settings.getSocketFactoryPort());
    this.props.put("mail.smtp.socketFactory.class", settings.getSocketFactoryClass());
    this.props.put("mail.smtp.socketFactory.fallback", settings.getSocketFactoryFallback());

    this.props.put("mail.debug", settings.getDebug());

    this.senderEmail = settings.getSenderEmail();
    this.senderPassword = settings.getSmtpAuthenticatorSenderPassword();
  }

  public Boolean send(EmailMessage emailMessage) {
    final List<InternetAddress> addressList =
        emailMessage.recipients().stream()
            .filter(EmailValidator::isValidEmail)
            .map(EmailValidator::toInternetAddress)
            .toList();
    try {
      Authenticator auth = null;
      if (senderPassword != null) {
        auth = new SMTPAuthenticator();
      }
      Session session = Session.getInstance(props, auth);
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(senderEmail));
      message.setRecipients(Message.RecipientType.TO, addressList.toArray(new InternetAddress[0]));
      message.setSubject(emailMessage.subject());
      message.setText(emailMessage.messageText());

      Optional<String> bccRecipient = emailMessage.bccRecipient();
      if (bccRecipient.isPresent()) {
        String bccEmail = bccRecipient.get();
        if (EmailValidator.isValidEmail(bccEmail)) {
          InternetAddress[] bccAddresses = {EmailValidator.toInternetAddress(bccEmail)};
          message.setRecipients(Message.RecipientType.BCC, bccAddresses);

        } else {
          String msg = String.format("Invalid bcc email address: %s", emailMessage.bccRecipient());
          logger.error(msg);
        }
      }

      Transport.send(message);
      logger.info("Email send successfully.");
      return true;

    } catch (Exception e) {
      logger.error("Error in sending email.");
      logger.error(e.getLocalizedMessage());
      return false;
    }
  }

  private class SMTPAuthenticator extends javax.mail.Authenticator {
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(senderEmail, senderPassword);
    }
  }
}
