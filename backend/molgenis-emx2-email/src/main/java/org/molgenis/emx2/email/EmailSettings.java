package org.molgenis.emx2.email;

import org.apache.logging.log4j.util.Strings;

public class EmailSettings {

  private String host;

  private final String port;
  private final String starttlsEnable;
  private final String sslProtocols;
  private final String socketFactoryPort;
  private final String socketFactoryClass;
  private final String socketFactoryFallback;
  private final String debug;

  private final String auth;
  private final String senderEmail;
  private final String smtpAuthenticatorSenderPassword;

  private EmailSettings(EmailSettingsBuilder builder) {
    this.host = builder.host;
    this.port = builder.port;

    this.starttlsEnable = builder.starttlsEnable;
    this.sslProtocols = builder.sslProtocols;

    this.socketFactoryPort = builder.socketFactoryPort;
    this.socketFactoryClass = builder.socketFactoryClass;
    this.socketFactoryFallback = builder.socketFactoryFallback;
    this.debug = builder.debug;

    this.auth = builder.auth;
    this.senderEmail = builder.senderEmail;
    this.smtpAuthenticatorSenderPassword = builder.smtpAuthenticatorSenderPassword;
  }

  public String getHost() {
    return this.host;
  }

  public String getSenderEmail() {
    return this.senderEmail;
  }

  public String getSmtpAuthenticatorSenderPassword() {
    return this.smtpAuthenticatorSenderPassword;
  }

  public String getPort() {
    return this.port;
  }

  public String getStarttlsEnable() {
    return this.starttlsEnable;
  }

  public String getSslProtocols() {
    return this.sslProtocols;
  }

  public String getSocketFactoryPort() {
    return this.socketFactoryPort;
  }

  public String getSocketFactoryClass() {
    return this.socketFactoryClass;
  }

  public String getSocketFactoryFallback() {
    return this.socketFactoryFallback;
  }

  public String getDebug() {
    return this.debug;
  }

  public String getAuth() {
    return this.auth;
  }

  public static class EmailSettingsBuilder {
    private String host = "smtpout1.molgenis.net";
    private String port = "25"; // / 587 / 2525
    private String starttlsEnable = Boolean.FALSE.toString();
    private String sslProtocols = "TLSv1.2";
    private String socketFactoryPort = Strings.EMPTY;
    private String socketFactoryClass = Strings.EMPTY;
    private String socketFactoryFallback = Strings.EMPTY;
    private String debug = Boolean.FALSE.toString();

    private String auth = Boolean.FALSE.toString();
    private String senderEmail = "no-reply@molgenis.net";
    private String smtpAuthenticatorSenderPassword;

    public EmailSettings build() {
      return new EmailSettings(this);
    }

    public EmailSettingsBuilder host(String host) {
      this.host = host;
      return this;
    }

    public EmailSettingsBuilder port(String port) {
      this.port = port;
      return this;
    }

    public EmailSettingsBuilder starttlsEnable(String starttlsEnable) {
      this.starttlsEnable = starttlsEnable;
      return this;
    }

    public EmailSettingsBuilder sslProtocols(String sslProtocols) {
      this.sslProtocols = sslProtocols;
      return this;
    }

    public EmailSettingsBuilder socketFactoryPort(String socketFactoryPort) {
      this.socketFactoryPort = socketFactoryPort;
      return this;
    }

    public EmailSettingsBuilder socketFactoryClass(String socketFactoryClass) {
      this.socketFactoryClass = socketFactoryClass;
      return this;
    }

    public EmailSettingsBuilder socketFactoryFallback(String socketFactoryFallback) {
      this.socketFactoryFallback = socketFactoryFallback;
      return this;
    }

    public EmailSettingsBuilder debug(String debug) {
      this.debug = debug;
      return this;
    }

    public EmailSettingsBuilder auth(String auth) {
      this.auth = auth;
      return this;
    }

    public EmailSettingsBuilder senderEmail(String senderEmail) {
      this.senderEmail = senderEmail;
      return this;
    }

    public EmailSettingsBuilder smtpAuthenticatorSenderPassword(
        String smtpAuthenticatorSenderPassword) {
      this.smtpAuthenticatorSenderPassword = smtpAuthenticatorSenderPassword;
      return this;
    }
  }
}
