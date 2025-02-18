package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.factory.ProfileManagerFactory;
import org.pac4j.javalin.JavalinContextFactory;
import org.pac4j.jee.context.session.JEESessionStoreFactory;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory {

  private String oidcClientId;
  private final String oidcClientSecret =
      (String)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_OIDC_CLIENT_SECRET, null, STRING);
  public static String OIDC_CLIENT_NAME =
      (String)
          EnvironmentProperty.getParameter(
              Constants.MOLGENIS_OIDC_CLIENT_NAME, "MolgenisAuth", STRING);
  private String oidcDiscoveryURI =
      (String)
          EnvironmentProperty.getParameter(
              Constants.MOLGENIS_OIDC_DISCOVERY_URI,
              "https://auth.molgenis.org/.well-known/openid-configuration/",
              STRING);
  private String callbackUrl =
      (String)
          EnvironmentProperty.getParameter(
              Constants.MOLGENIS_OIDC_CALLBACK_URL, "http://localhost:8080", STRING);

  private Boolean unsignedToken =
      (Boolean)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_OIDC_UNSIGNED_TOKEN, false, BOOL);

  public Config build() {
    final OidcConfiguration oidcConfiguration = new OidcConfiguration();
    oidcClientId =
        (String) EnvironmentProperty.getParameter(Constants.MOLGENIS_OIDC_CLIENT_ID, null, STRING);
    oidcConfiguration.setClientId(oidcClientId);
    oidcConfiguration.setSecret(oidcClientSecret);
    oidcConfiguration.setDiscoveryURI(oidcDiscoveryURI);
    oidcConfiguration.setAllowUnsignedIdTokens(unsignedToken);

    final OidcClient oidcClient = new OidcClient(oidcConfiguration);
    oidcClient.setName(OIDC_CLIENT_NAME);

    final Clients clients = new Clients(callbackUrl + ("/" + OIDC_CALLBACK_PATH), oidcClient);

    Config config = new Config(clients);
    config.setHttpActionAdapter(JavalinCustomHttpActionAdapter.INSTANCE);
    config.setWebContextFactory(JavalinContextFactory.INSTANCE);
    config.setSessionStoreFactory(JEESessionStoreFactory.INSTANCE);
    config.setProfileManagerFactory(ProfileManagerFactory.DEFAULT);
    return config;
  }
}
