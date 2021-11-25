package org.molgenis.emx2.web.controllers;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.SparkWebContext;

public class ProfileManagerFactoryTest {

  private ProfileManagerFactory profileManagerFactory = new ProfileManagerFactory();

  @Test
  public void testCreateSparkWebContext() {
    SparkWebContext context = mock(SparkWebContext.class);
    when(context.getSessionStore()).thenReturn(mock(SessionStore.class));
    ProfileManager<OidcProfile> profileManager =
        profileManagerFactory.createOidcProfileManager(context);
    assertNotNull(profileManager);
  }
}
