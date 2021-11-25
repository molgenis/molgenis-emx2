package org.molgenis.emx2.web.controllers;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

public class SparkWebContextFactoryTest {

  private SparkWebContextFactory sparkWebContextFactory = new SparkWebContextFactory();

  @Test
  public void testCreateSparkWebContext() {

    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Response response = mock(Response.class);
    Request request = mock(Request.class);
    when(response.raw()).thenReturn(httpServletResponse);
    when(request.raw()).thenReturn(httpServletRequest);

    assertNotNull(sparkWebContextFactory.createSparkWebContext(request, response));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateSparkWebContextWithSessionStore() {

    SessionStore<SparkWebContext> sessionStore = mock(SessionStore.class);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    Response response = mock(Response.class);
    Request request = mock(Request.class);
    when(response.raw()).thenReturn(httpServletResponse);
    when(request.raw()).thenReturn(httpServletRequest);

    assertNotNull(
        sparkWebContextFactory.createSessionStoreSparkWebContext(request, response, sessionStore));
  }
}
