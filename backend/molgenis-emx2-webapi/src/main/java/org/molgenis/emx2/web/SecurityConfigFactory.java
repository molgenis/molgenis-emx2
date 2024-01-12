package org.molgenis.emx2.web;

import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.OIDC_CALLBACK_PATH;

import org.molgenis.emx2.Constants;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory {

  private String oidcClientId =
      (String) EnvironmentProperty.getParameter(Constants.MOLGENIS_OIDC_CLIENT_ID, null, STRING);
  private String oidcClientSecret =
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

  public Config build() {
    final OidcConfiguration oidcConfiguration = new OidcConfiguration();
    oidcConfiguration.setClientId(oidcClientId);
    oidcConfiguration.setSecret(oidcClientSecret);
    oidcConfiguration.setDiscoveryURI(oidcDiscoveryURI);

    final OidcClient oidcClient = new OidcClient(oidcConfiguration);
    oidcClient.setName(OIDC_CLIENT_NAME);

    final Clients clients = new Clients(callbackUrl + ("/" + OIDC_CALLBACK_PATH), oidcClient);

    return new Config(clients);
  }
}
