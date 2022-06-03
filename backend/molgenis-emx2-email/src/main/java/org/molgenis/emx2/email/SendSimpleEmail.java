package org.molgenis.emx2.email;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendSimpleEmail {

  private final String senderEmailId;
  private final String senderPassword;

  private final Properties props = new Properties();

  private static final Logger logger = LoggerFactory.getLogger(SendSimpleEmail.class);

  public SendSimpleEmail() {

    String emailSMTPserver = System.getenv("MOLGENIS_EMAIL_SMTP_SERVER");
    senderEmailId = System.getenv("MOLGENIS_EMAIL_SENDER_ID");
    senderPassword = System.getenv("MOLGENIS_EMAIL_SENDER_PASSWORD");

    if (emailSMTPserver == null) {
      throw new MolgenisException("Missing 'emailSMTPserver' property");
    }

    if (senderEmailId == null) {
      throw new MolgenisException("Missing 'senderEmailId' property");
    }

    if (senderPassword == null) {
      throw new MolgenisException("Missing 'senderPassword' property");
    }

    this.props.put("mail.smtp.auth", "true");
    this.props.put("mail.smtp.host", emailSMTPserver);
    this.props.put("mail.smtp.starttls.enable", "true");
    this.props.put("mail.smtp.port", "465");
    this.props.put("mail.smtp.ssl.protocols", "TLSv1.2");
    this.props.put("mail.smtp.socketFactory.port", "465");
    this.props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    this.props.put("mail.smtp.socketFactory.fallback", "false");

    //    props.put("mail.debug", "true");

  }

  public Boolean send(String receiverEmail, String subject, String messageText) {
    try {
      Authenticator auth = new SMTPAuthenticator();
      Session session = Session.getInstance(props, auth);
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(senderEmailId));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail));
      message.setSubject(subject);
      message.setText(messageText);

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
      return new PasswordAuthentication(senderEmailId, senderPassword);
    }
  }

//  public static void main(String[] args) {
//    SendSimpleEmail email = new SendSimpleEmail();
//    email.send("foobar@gmail.com", "Test Email", "Hi,\n\n This is a test email.");
//  }
}
