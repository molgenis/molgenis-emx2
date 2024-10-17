package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.OkAction;
import org.pac4j.javalin.JavalinWebContext;

class JavalinCustomHttpActionAdapterTest {

  @Test
  void checksContextForJavalin() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(HttpAction.class);
    WebContext ctx = mock(WebContext.class);
    assertThrows(RuntimeException.class, () -> adapter.adapt(action, ctx));
  }

  @Test
  void throwsUnauthorizedIfActionHas401Code() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(HttpAction.class);
    when(action.getCode()).thenReturn(401);
    WebContext ctx = mock(JavalinWebContext.class);
    assertThrows(UnauthorizedResponse.class, () -> adapter.adapt(action, ctx));
  }

  @Test
  void throwsForbiddenIfActionHas403Code() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(HttpAction.class);
    when(action.getCode()).thenReturn(403);
    WebContext ctx = mock(JavalinWebContext.class);
    assertThrows(ForbiddenResponse.class, () -> adapter.adapt(action, ctx));
  }

  @Test
  void throwsBadRequestIfActionHas400Code() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(HttpAction.class);
    when(action.getCode()).thenReturn(400);
    WebContext ctx = mock(JavalinWebContext.class);
    assertThrows(BadRequestResponse.class, () -> adapter.adapt(action, ctx));
  }

  @Test
  void shouldReturnAfterSettingContext() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(OkAction.class);
    when(action.getCode()).thenReturn(200);
    JavalinWebContext ctx = mock(JavalinWebContext.class);
    Context context = mock(Context.class);

    when(ctx.getJavalinCtx()).thenReturn(context);
    assertEquals(null, adapter.adapt(action, ctx));
  }

  @Test
  void shouldReturnInCaseOfLocationAction() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(FoundAction.class);
    when(action.getCode()).thenReturn(300);
    JavalinWebContext ctx = mock(JavalinWebContext.class);
    Context context = mock(Context.class);

    when(ctx.getJavalinCtx()).thenReturn(context);
    assertEquals(null, adapter.adapt(action, ctx));
  }

  @Test
  void shouldReturnInCaseOfUnmatchedAction() {
    JavalinCustomHttpActionAdapter adapter = JavalinCustomHttpActionAdapter.INSTANCE;
    HttpAction action = mock(HttpAction.class);
    when(action.getCode()).thenReturn(418);
    JavalinWebContext ctx = mock(JavalinWebContext.class);
    Context context = mock(Context.class);

    when(ctx.getJavalinCtx()).thenReturn(context);
    assertEquals(null, adapter.adapt(action, ctx));
  }
}
