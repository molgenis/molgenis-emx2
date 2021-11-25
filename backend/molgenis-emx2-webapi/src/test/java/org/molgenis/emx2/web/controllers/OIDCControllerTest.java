package org.molgenis.emx2.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.molgenis.emx2.web.SecurityConfigFactory.OIDC_CLIENT_NAME;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.web.MolgenisSession;
import org.molgenis.emx2.web.MolgenisSessionManager;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

public class OIDCControllerTest {

  private Request request = mock(Request.class);
  private Response response = mock(Response.class);

  private SparkWebContext context = mock(SparkWebContext.class);
  private Config config = mock(Config.class);

  private MolgenisSessionManager sessionManager = mock(MolgenisSessionManager.class);

  @SuppressWarnings("unchecked")
  private ProfileManager<OidcProfile> profileManager = mock(ProfileManager.class);

  private OIDCController oidcController;

  @SuppressWarnings("unchecked")
  @Before
  public void beforeMethod() {
    SparkWebContextFactory contextFactory = mock(SparkWebContextFactory.class);
    FindBestFactory findBestFactory = mock(FindBestFactory.class);
    ProfileManagerFactory profileManagerFactory = mock(ProfileManagerFactory.class);

    SessionStore<SparkWebContext> sessionStore = mock(SessionStore.class);
    HttpActionAdapter<Object, SparkWebContext> adapter = mock(HttpActionAdapter.class);
    CallbackLogic<Object, SparkWebContext> callbackLogic = mock(CallbackLogic.class);

    when(contextFactory.createSessionStoreSparkWebContext(request, response, sessionStore))
        .thenReturn(context);
    when(contextFactory.createSparkWebContext(request, response)).thenReturn(context);

    when(findBestFactory.createBestAdapter(config)).thenReturn(adapter);
    when(findBestFactory.createBestSessionStore(config)).thenReturn(sessionStore);
    when(findBestFactory.createBestCallbackLogic(config)).thenReturn(callbackLogic);

    when(profileManagerFactory.createOidcProfileManager(context)).thenReturn(profileManager);

    oidcController =
        new OIDCController(
            sessionManager, config, contextFactory, findBestFactory, profileManagerFactory);
  }

  @Test
  public void testHandleLoginCallbackWithExistingUser() {
    String user = "user@example.org";
    Database database = mock(Database.class);
    beforeCallBackTests(database, user);

    when(database.hasUser(user)).thenReturn(true);

    oidcController.handleLoginCallback(request, response);
    verify(sessionManager).getSession(request);
    verify(sessionManager.getSession(request)).getDatabase();
    verify(database).hasUser(user);
    verify(database).setActiveUser(user);
  }

  @Test
  public void testHandleLoginCallbackWithNonExistingUser() {
    String user = "user@example.org";
    Database database = mock(Database.class);
    beforeCallBackTests(database, user);

    when(database.hasUser(user)).thenReturn(false);

    oidcController.handleLoginCallback(request, response);
    verify(sessionManager).getSession(request);
    verify(sessionManager.getSession(request)).getDatabase();
    verify(database).hasUser(user);
    verify(database).addUser(user);
    verify(database).setActiveUser(user);
  }

  @Test
  public void testHandleLoginCallbackNoProfile() {
    when(profileManager.get(true)).thenReturn(Optional.empty());
    oidcController.handleLoginCallback(request, response);
    verify(profileManager).get(true);
  }

  @Test
  public void testHandleLoginRequest() {
    Clients clients = mock(Clients.class);
    Client client = mock(Client.class);
    Optional<Client> optionalClient = Optional.of(client);
    when(config.getClients()).thenReturn(clients);
    when(config.getClients().findClient(OIDC_CLIENT_NAME)).thenReturn(optionalClient);

    FoundAction action = mock(FoundAction.class);
    Optional<FoundAction> optionalAction = Optional.of(action);
    when(action.getCode()).thenReturn(302);
    when(action.getLocation()).thenReturn("http://localhost");
    when(client.getRedirectionAction(context)).thenReturn(optionalAction);
    when(context.getSparkResponse()).thenReturn(response);

    oidcController.handleLoginRequest(request, response);
    verify(config, atLeast(1)).getClients();
    verify(config.getClients()).findClient(OIDC_CLIENT_NAME);
    verify(client).getRedirectionAction(context);
    verify(response).redirect("http://localhost", 302);
  }

  private void beforeCallBackTests(Database database, String user) {
    Optional<OidcProfile> oidcProfile = Optional.of(mock(OidcProfile.class));
    when(profileManager.get(true)).thenReturn(oidcProfile);
    when(oidcProfile.get().getEmail()).thenReturn(user);
    MolgenisSession molgenisSession = mock(MolgenisSession.class);
    when(sessionManager.getSession(any())).thenReturn(molgenisSession);
    when(molgenisSession.getDatabase()).thenReturn(database);
  }


}
