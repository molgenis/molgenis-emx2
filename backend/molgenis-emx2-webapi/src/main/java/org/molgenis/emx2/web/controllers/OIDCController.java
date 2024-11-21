package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static org.molgenis.emx2.web.SecurityConfigFactory.OIDC_CLIENT_NAME;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.Optional;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.web.JavalinCustomHttpActionAdapter;
import org.molgenis.emx2.web.SecurityConfigFactory;
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
import org.pac4j.javalin.JavalinHttpActionAdapter;
import org.pac4j.javalin.JavalinWebContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDCController {

  private static final Logger logger = LoggerFactory.getLogger(OIDCController.class);

  private Config securityConfig;
  private final SessionStore sessionStore;

  public OIDCController() {
    this.securityConfig = new SecurityConfigFactory().build();
    this.sessionStore = FindBest.sessionStore(null, securityConfig, JEESessionStore.INSTANCE);
  }

  public void reloadConfig() {
    this.securityConfig = new SecurityConfigFactory().build();
  }

  public void handleLoginRequest(Context ctx) {
    final JavalinWebContext context = new JavalinWebContext(ctx);
    sessionStore.set(context, Pac4jConstants.REQUESTED_URL, ctx.queryParams("redirect"));
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
    JavalinHttpActionAdapter.INSTANCE.adapt(action, context);
  }

  public void handleLoginCallback(Context ctx) {
    final JavalinWebContext context = new JavalinWebContext(ctx);
    Optional<Object> requestedUrlList = sessionStore.get(context, Pac4jConstants.REQUESTED_URL);
    HttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    final CallbackLogic callbackLogic =
        FindBest.callbackLogic(null, securityConfig, DefaultCallbackLogic.INSTANCE);

    callbackLogic.perform(
        context, sessionStore, securityConfig, adapter, null, false, OIDC_CLIENT_NAME);

    final ProfileManager manager = new ProfileManager(context, sessionStore);
    Optional<UserProfile> oidcProfile = manager.getProfile();

    if (oidcProfile.isEmpty()) {
      logger.error("OIDC sign in failed, no profile found");
      ctx.status(500);
      ctx.redirect("/");
      return;
    }

    String user = oidcProfile.get().getAttribute("email").toString();
    if (user == null || user.isEmpty()) {
      logger.error("OIDC sign in failed, email claim is empty");
      ctx.status(500);
      ctx.redirect("/");
      return;
    }

    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    if (!database.hasUser(user)) {
      logger.info("Add new OIDC user({}) to database", user);
      database.addUser(user);
    }
    database.setActiveUser(user);
    logger.info("OIDC sign in for user: {}", user);

    ctx.status(302);

    if (requestedUrlList.isPresent()) {
      @SuppressWarnings("unchecked")
      ArrayList<String> requestedUrl = (ArrayList<String>) requestedUrlList.get();
      if (requestedUrl.size() == 1) {
        ctx.redirect(requestedUrl.get(0));
      } else {
        ctx.redirect("/");
      }
    } else {
      ctx.redirect("/");
    }
  }
}
