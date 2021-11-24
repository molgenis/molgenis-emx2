package org.molgenis.emx2.web.controllers;

import org.pac4j.core.profile.ProfileManager;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.sparkjava.SparkWebContext;

public class ProfileManagerFactory {

  private ProfileManager<OidcProfile> profileManager;

  public ProfileManager<OidcProfile> createOidcProfileManager(SparkWebContext context) {
    if (profileManager == null) {
      profileManager = new ProfileManager<>(context);
    }
    return profileManager;
  }
}
