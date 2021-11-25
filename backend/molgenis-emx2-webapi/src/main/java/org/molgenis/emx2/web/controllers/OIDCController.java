package org.molgenis.emx2.web.controllers;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.web.SecurityConfigFactory.OIDC_CLIENT_NAME;

import java.util.Optional;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.web.MolgenisSessionManager;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class OIDCController {

  private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);

  private MolgenisSessionManager sessionManager;
  private Config securityConfig;

  private SparkWebContextFactory contextFactory;
  private FindBestFactory findBestFactory;
  private ProfileManagerFactory profileManagerFactory;

  public OIDCController(
      MolgenisSessionManager sessionManager,
      Config securityConfig,
      SparkWebContextFactory contextFactory,
      FindBestFactory findBestFactory,
      ProfileManagerFactory profileManagerFactory) {
    this.sessionManager = requireNonNull(sessionManager);
    this.securityConfig = requireNonNull(securityConfig);
    this.contextFactory = requireNonNull(contextFactory);
    this.findBestFactory = requireNonNull(findBestFactory);
    this.profileManagerFactory = requireNonNull(profileManagerFactory);
  }

  public Object handleLoginRequest(Request request, Response response) {
    final SparkWebContext context = contextFactory.createSparkWebContext(request, response);
    final var client =
        securityConfig
            .getClients()
            .findClient(OIDC_CLIENT_NAME)
            .orElseThrow(
                () ->
                    new MolgenisException(
                        "Expected OIDC client not found in security configuration"));
    HttpAction action;
    try {
      @SuppressWarnings("unchecked")
      Optional<HttpAction> httpAction = client.getRedirectionAction(context);
      if (httpAction.isEmpty()) {
        throw new MolgenisException("Expected OIDC redirection action not found");
      }
      action = httpAction.get();

    } catch (final HttpAction e) {
      action = e;
    }
    return SparkHttpActionAdapter.INSTANCE.adapt(action, context);
  }

  public Object handleLoginCallback(Request request, Response response) {
    final SessionStore<SparkWebContext> sessionStore =
        findBestFactory.createBestSessionStore(securityConfig);
    final SparkWebContext context =
        contextFactory.createSessionStoreSparkWebContext(request, response, sessionStore);

    final HttpActionAdapter<Object, SparkWebContext> adapter =
        findBestFactory.createBestAdapter(securityConfig);
    final CallbackLogic<Object, SparkWebContext> callbackLogic =
        findBestFactory.createBestCallbackLogic(securityConfig);

    callbackLogic.perform(
        context, securityConfig, adapter, null, false, true, true, OIDC_CLIENT_NAME);

    final ProfileManager<OidcProfile> manager =
        profileManagerFactory.createOidcProfileManager(context);
    Optional<OidcProfile> oidcProfile = manager.get(true);

    if (oidcProfile.isPresent()) {
      String user = oidcProfile.get().getEmail();
      Database database = sessionManager.getSession(request).getDatabase();
      if (!database.hasUser(user)) {
        logger.info("Add new OIDC user({}) to database", user);
        database.addUser(user);
      }
      database.setActiveUser(user);
      logger.info("OIDC sign in for user: {}", user);
      response.status(302);
    } else {
      logger.error("OIDC sign in failed, no profile found");
      response.status(404);
    }

    response.redirect("/");
    return response;
  }
}
