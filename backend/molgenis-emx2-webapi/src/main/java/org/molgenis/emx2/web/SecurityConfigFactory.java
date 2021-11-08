package org.molgenis.emx2.web;

import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory {

  private String oidcClientId = "f09db035-c961-485a-b186-aa496a2fff65";
  private String oidcClientSecret = "g37c-acHNUdsLaMgIhzeNsJphWXczJy7VbLlthQBKJ4";
  public static String OIDC_CLIENT_NAME = "MolgenisAuth";
  private String oidcDiscoveryURI = "https://auth.molgenis.org/.well-known/openid-configuration/";

  public Config build() {
    final OidcConfiguration oidcConfiguration = new OidcConfiguration();
    oidcConfiguration.setClientId(oidcClientId);
    oidcConfiguration.setSecret(oidcClientSecret);
    oidcConfiguration.setDiscoveryURI(oidcDiscoveryURI);

    final OidcClient<OidcConfiguration> oidcClient = new OidcClient<>(oidcConfiguration);
    oidcClient.setName(OIDC_CLIENT_NAME);

    final HeaderClient headerClient =
        new HeaderClient(
            "Authorization",
            (credentials, ctx) -> {
              final String token = ((TokenCredentials) credentials).getToken();
              if (CommonHelper.isNotBlank(token)) {
                final CommonProfile profile = new CommonProfile();
                profile.setId(token);
                credentials.setUserProfile(profile);
              }
            });

    final Clients clients = new Clients("http://localhost:8080/callback", oidcClient, headerClient);

    return new Config(clients);
  }
}
