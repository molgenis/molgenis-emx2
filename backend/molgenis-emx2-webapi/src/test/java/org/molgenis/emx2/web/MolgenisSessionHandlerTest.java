package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Constants.ANONYMOUS_USER;
import static org.pac4j.core.util.Pac4jConstants.USERNAME;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MolgenisSessionHandlerTest {

  @Test
  void givenBlankUsernameInSession_whenGetCurrentUser_thenAnonymous() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(USERNAME)).thenReturn("  ");

    assertEquals(ANONYMOUS_USER, new MolgenisSessionHandler(request).getCurrentUser());
  }
}
