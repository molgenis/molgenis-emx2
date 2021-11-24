package org.molgenis.emx2.web.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.web.MolgenisSession;
import org.molgenis.emx2.web.MolgenisSessionManager;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
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
  private ProfileManager<OidcProfile> profileManager = mock(ProfileManager.class);

  private OIDCController oidcController;

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

    when(findBestFactory.createBestAdapter(config)).thenReturn(adapter);
    when(findBestFactory.createBestSessionStore(config)).thenReturn(sessionStore);
    when(findBestFactory.createBestCallbackLogic(config)).thenReturn(callbackLogic);

    when(profileManagerFactory.createOidcProfileManager(context)).thenReturn(profileManager);

    oidcController =
        new OIDCController(
            sessionManager, config, contextFactory, findBestFactory, profileManagerFactory);
  }

  @Test
  public void testHandleLoginCallback() {
    when(response.status()).thenReturn(302);

    String user = "user@example.org";
    Optional<OidcProfile> oidcProfile = Optional.of(mock(OidcProfile.class));
    Database database = mock(Database.class);
    when(profileManager.get(true)).thenReturn(oidcProfile);
    when(oidcProfile.get().getEmail()).thenReturn(user);
    MolgenisSession molgenisSession = mock(MolgenisSession.class);
    when(sessionManager.getSession(any())).thenReturn(molgenisSession);
    when(molgenisSession.getDatabase()).thenReturn(database);
    when(database.hasUser(user)).thenReturn(true);

    Object responseObject = oidcController.handleLoginCallback(request, response);
    assertEquals(302, ((Response) responseObject).status());
  }

  @Test
  public void testHandleLoginCallbackNoProfile() {
    when(response.status()).thenReturn(404);
    when(profileManager.get(true)).thenReturn(Optional.ofNullable(null));

    Object responseObject = oidcController.handleLoginCallback(request, response);
    assertEquals(404, ((Response) responseObject).status());
  }
}
