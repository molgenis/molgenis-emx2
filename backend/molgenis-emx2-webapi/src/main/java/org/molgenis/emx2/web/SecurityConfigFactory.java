package org.molgenis.emx2.web;

import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory {

    private String clientId = "f09db035-c961-485a-b186-aa496a2fff65";
    private String clientSecret = "g37c-acHNUdsLaMgIhzeNsJphWXczJy7VbLlthQBKJ4";
    private String discoveryURI = "https://auth.molgenis.org";

    public Config build() {
        final OidcConfiguration oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId(clientId);
        oidcConfiguration.setSecret(clientSecret);
        oidcConfiguration.setDiscoveryURI(discoveryURI);

        final OidcClient<OidcConfiguration> oidcClient = new OidcClient<>(oidcConfiguration);

        final Clients clients = new Clients("http://localhost:8080/callback", oidcClient, new AnonymousClient());

        return new Config(clients);
    }

}
