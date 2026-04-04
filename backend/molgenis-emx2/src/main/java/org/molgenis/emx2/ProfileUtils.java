package org.molgenis.emx2;

import java.util.Set;

public class ProfileUtils {
  private ProfileUtils() {}

  public static boolean matchesActiveProfiles(String[] itemProfiles, String[] activeProfiles) {
    if (activeProfiles == null || activeProfiles.length == 0) return true;
    if (itemProfiles == null || itemProfiles.length == 0) return true;

    Set<String> active = Set.of(activeProfiles);
    boolean hasPositiveProfile = false;

    for (String profile : itemProfiles) {
      if (profile.startsWith("-")) {
        if (active.contains(profile.substring(1))) return false;
      } else {
        hasPositiveProfile = true;
        if (active.contains(profile)) return true;
      }
    }

    return !hasPositiveProfile;
  }
}
