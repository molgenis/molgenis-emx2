package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.web.SecurityConfigFactory.OIDC_CLIENT_NAME;

import java.util.Optional;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.web.MolgenisSessionManager;
import org.molgenis.emx2.web.SecurityConfigFactory;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.JEESessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.FindBest;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class OIDCController {

  private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);
  private static final Config securityConfig = new SecurityConfigFactory().build();

  private OIDCController() {}

  public static Object handleLoginRequest(Request request, Response response) {
    final SparkWebContext context = new SparkWebContext(request, response);
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
      Optional optional = client.getRedirectionAction(context);
      if (optional.isEmpty()) {
        throw new MolgenisException("Expected OIDC redirection action not found");
      }
      action = (HttpAction) optional.get();

    } catch (final HttpAction e) {
      action = e;
    }
    return SparkHttpActionAdapter.INSTANCE.adapt(action, context);
  }

  public static Object handleLoginCallback(
      Request request, Response response, MolgenisSessionManager sessionManager) {
    final SessionStore<SparkWebContext> bestSessionStore =
        FindBest.sessionStore(null, securityConfig, JEESessionStore.INSTANCE);
    final HttpActionAdapter<Object, SparkWebContext> bestAdapter =
        FindBest.httpActionAdapter(null, securityConfig, SparkHttpActionAdapter.INSTANCE);
    final CallbackLogic<Object, SparkWebContext> bestLogic =
        FindBest.callbackLogic(null, securityConfig, DefaultCallbackLogic.INSTANCE);

    final SparkWebContext context = new SparkWebContext(request, response, bestSessionStore);
    bestLogic.perform(
        context, securityConfig, bestAdapter, null, false, true, true, "MolgenisAuth");

    final ProfileManager<OidcProfile> manager = new ProfileManager<>(context);
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
