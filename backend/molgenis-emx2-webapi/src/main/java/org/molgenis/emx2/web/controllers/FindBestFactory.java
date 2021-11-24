package org.molgenis.emx2.web.controllers;

import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.JEESessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;

public class FindBestFactory {

  private SessionStore<SparkWebContext> sessionStore;
  private HttpActionAdapter<Object, SparkWebContext> adapter;
  private CallbackLogic<Object, SparkWebContext> callbackLogic;

  public SessionStore<SparkWebContext> createBestSessionStore(Config config) {
    if (sessionStore == null) {
      sessionStore = FindBest.sessionStore(null, config, JEESessionStore.INSTANCE);
    }
    return sessionStore;
  }

  public HttpActionAdapter<Object, SparkWebContext> createBestAdapter(Config config) {
    if (adapter == null) {
      adapter = FindBest.httpActionAdapter(null, config, SparkHttpActionAdapter.INSTANCE);
    }
    return adapter;
  }

  public CallbackLogic<Object, SparkWebContext> createBestCallbackLogic(Config config) {
    if (callbackLogic == null) {
      callbackLogic = FindBest.callbackLogic(null, config, DefaultCallbackLogic.INSTANCE);
    }
    return callbackLogic;
  }
}
