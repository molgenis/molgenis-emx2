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
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.FindBest;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class OIDCController {

  private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);

  private final MolgenisSessionManager sessionManager;
  private final Config securityConfig;
  private final SessionStore sessionStore;

  public OIDCController(MolgenisSessionManager sessionManager, Config securityConfig) {
    this.sessionManager = requireNonNull(sessionManager);
    this.securityConfig = requireNonNull(securityConfig);
    this.sessionStore = FindBest.sessionStore(null, securityConfig, JEESessionStore.INSTANCE);
  }

  public Object handleLoginRequest(Request request, Response response) {
    final SparkWebContext context = new SparkWebContext(request, response);
    sessionStore.set(context, Pac4jConstants.REQUESTED_URL, request.queryParams("redirect"));
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
      Optional<RedirectionAction> redirectionAction =
          client.getRedirectionAction(context, JEESessionStore.INSTANCE);
      if (redirectionAction.isEmpty()) {
        throw new MolgenisException("Expected OIDC redirection action not found");
      }
      action = redirectionAction.get();

    } catch (final HttpAction e) {
      action = e;
    }
    return SparkHttpActionAdapter.INSTANCE.adapt(action, context);
  }

  public Object handleLoginCallback(Request request, Response response) {
    final SparkWebContext context = new SparkWebContext(request, response);

    final HttpActionAdapter adapter =
        FindBest.httpActionAdapter(null, securityConfig, SparkHttpActionAdapter.INSTANCE);
    final CallbackLogic callbackLogic =
        FindBest.callbackLogic(null, securityConfig, DefaultCallbackLogic.INSTANCE);

    callbackLogic.perform(
        context, sessionStore, securityConfig, adapter, null, false, OIDC_CLIENT_NAME);

    final ProfileManager manager = new ProfileManager(context, sessionStore);
    Optional<UserProfile> oidcProfile = manager.getProfile();

    if (oidcProfile.isEmpty()) {
      logger.error("OIDC sign in failed, no profile found");
      response.status(500);
      response.redirect("/");
      return response;
    }

    String user = oidcProfile.get().getAttribute("email").toString();
    if (user == null || user.isEmpty()) {
      logger.error("OIDC sign in failed, email claim is empty");
      response.status(500);
      response.redirect("/");
      return response;
    }

    Database database = sessionManager.getSession(request).getDatabase();
    if (!database.hasUser(user)) {
      logger.info("Add new OIDC user({}) to database", user);
      database.addUser(user);
    }
    database.setActiveUser(user);
    logger.info("OIDC sign in for user: {}", user);

    response.status(302);
    return response;
  }
}
