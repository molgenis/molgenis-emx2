package org.molgenis.emx2.web.controllers;

import org.pac4j.core.context.session.SessionStore;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

public class SparkWebContextFactory {

  private SparkWebContext context;
  private SparkWebContext sessionStoreContext;

  public SparkWebContext createSparkWebContext(Request request, Response response) {
    if (context == null) {
      context = new SparkWebContext(request, response);
    }
    return context;
  }

  public SparkWebContext createSessionStoreSparkWebContext(
      Request request, Response response, SessionStore<SparkWebContext> bestSessionStore) {
    if (sessionStoreContext == null) {
      sessionStoreContext = new SparkWebContext(request, response, bestSessionStore);
    }
    return sessionStoreContext;
  }
}
