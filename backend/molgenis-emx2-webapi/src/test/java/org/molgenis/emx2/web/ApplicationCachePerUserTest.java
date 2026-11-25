package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.pac4j.core.util.Pac4jConstants.USERNAME;

import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;

class ApplicationCachePerUserTest {

  @Test
  void givenBlankUsernameInSession_whenGetDatabaseForUser_thenAnonymous() {
    Context ctx = Mockito.mock(Context.class);
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpSession session = Mockito.mock(HttpSession.class);
    when(ctx.req()).thenReturn(request);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(USERNAME)).thenReturn("  ");

    Database database = ApplicationCachePerUser.getInstance().getDatabaseForUser(ctx);

    assertEquals(SqlDatabase.ANONYMOUS, database.getActiveUsername());
  }
}
