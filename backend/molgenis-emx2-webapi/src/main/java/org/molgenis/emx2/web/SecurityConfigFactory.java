package org.molgenis.emx2.web;

import java.util.Optional;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory {

  private String clientId = "f09db035-c961-485a-b186-aa496a2fff65";
  private String clientSecret = "g37c-acHNUdsLaMgIhzeNsJphWXczJy7VbLlthQBKJ4";
  public static String clientName = "MolgenisAuth";
  private String discoveryURI = "https://auth.molgenis.org/.well-known/openid-configuration/";

  public Config build() {
    final OidcConfiguration oidcConfiguration = new OidcConfiguration();
    oidcConfiguration.setClientId(clientId);
    oidcConfiguration.setSecret(clientSecret);
    oidcConfiguration.setDiscoveryURI(discoveryURI);

    final OidcClient<OidcConfiguration> oidcClient = new OidcClient<>(oidcConfiguration);
    oidcClient.setName(clientName);
    oidcClient.setAuthorizationGenerator(
        (ctx, profile) -> {
          profile.addRole("ROLE_ADMIN");
          return Optional.of(profile);
        });

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

    final Clients clients =
        new Clients(
            "http://localhost:8080/callback", oidcClient, new AnonymousClient(), headerClient);

    return new Config(clients);
  }
}
