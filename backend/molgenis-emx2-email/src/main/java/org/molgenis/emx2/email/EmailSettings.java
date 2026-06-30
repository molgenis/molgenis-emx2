package org.molgenis.emx2.email;

import org.apache.logging.log4j.util.Strings;

public class EmailSettings {

  private final String host;

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
    host = builder.host;
    port = builder.port;

    starttlsEnable = builder.starttlsEnable;
    sslProtocols = builder.sslProtocols;

    socketFactoryPort = builder.socketFactoryPort;
    socketFactoryClass = builder.socketFactoryClass;
    socketFactoryFallback = builder.socketFactoryFallback;
    debug = builder.debug;

    auth = builder.auth;
    senderEmail = builder.senderEmail;
    smtpAuthenticatorSenderPassword = builder.smtpAuthenticatorSenderPassword;
  }

  public String getHost() {
    return host;
  }

  public String getSenderEmail() {
    return senderEmail;
  }

  public String getSmtpAuthenticatorSenderPassword() {
    return smtpAuthenticatorSenderPassword;
  }

  public String getPort() {
    return port;
  }

  public String getStarttlsEnable() {
    return starttlsEnable;
  }

  public String getSslProtocols() {
    return sslProtocols;
  }

  public String getSocketFactoryPort() {
    return socketFactoryPort;
  }

  public String getSocketFactoryClass() {
    return socketFactoryClass;
  }

  public String getSocketFactoryFallback() {
    return socketFactoryFallback;
  }

  public String getDebug() {
    return debug;
  }

  public String getAuth() {
    return auth;
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
