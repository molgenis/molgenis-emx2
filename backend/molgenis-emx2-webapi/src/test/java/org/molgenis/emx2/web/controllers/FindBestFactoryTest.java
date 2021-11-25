package org.molgenis.emx2.web.controllers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.JEESessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.sparkjava.SparkHttpActionAdapter;
import org.pac4j.sparkjava.SparkWebContext;

public class FindBestFactoryTest {

  private FindBestFactory findBestFactory = new FindBestFactory();

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateSessionStore() {
    SessionStore<SparkWebContext> sessionStore = mock(SessionStore.class);
    Config config = mock(Config.class);
    try (MockedStatic<FindBest> findBest = mockStatic(FindBest.class)) {
      findBest
          .when(() -> FindBest.sessionStore(null, config, JEESessionStore.INSTANCE))
          .thenReturn(sessionStore);

      SessionStore<SparkWebContext> actualSessionStore =
          findBestFactory.createBestSessionStore(config);
      assertEquals(actualSessionStore, sessionStore);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateAdapter() {
    HttpActionAdapter<Object, SparkWebContext> httpActionAdapter = mock(HttpActionAdapter.class);
    Config config = mock(Config.class);
    try (MockedStatic<FindBest> findBest = mockStatic(FindBest.class)) {
      findBest
          .when(() -> FindBest.httpActionAdapter(null, config, SparkHttpActionAdapter.INSTANCE))
          .thenReturn(httpActionAdapter);

      HttpActionAdapter<Object, SparkWebContext> actualHttpActionAdapter =
          findBestFactory.createBestAdapter(config);
      assertEquals(actualHttpActionAdapter, httpActionAdapter);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateCallBackLogic() {
    CallbackLogic<Object, SparkWebContext> callbackLogic = mock(CallbackLogic.class);
    Config config = mock(Config.class);
    try (MockedStatic<FindBest> findBest = mockStatic(FindBest.class)) {
      findBest
          .when(() -> FindBest.callbackLogic(null, config, DefaultCallbackLogic.INSTANCE))
          .thenReturn(callbackLogic);

      CallbackLogic<Object, SparkWebContext> actualCallbackLogic =
          findBestFactory.createBestCallbackLogic(config);
      assertEquals(actualCallbackLogic, callbackLogic);
    }
  }
}
