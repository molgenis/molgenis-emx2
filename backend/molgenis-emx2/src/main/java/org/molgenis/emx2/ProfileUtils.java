package org.molgenis.emx2;

import java.util.Set;

public class ProfileUtils {
  private ProfileUtils() {}

  public static boolean matchesActiveProfiles(String[] itemSubsets, String[] activeSubsets) {
    if (activeSubsets == null || activeSubsets.length == 0) return true;
    if (itemSubsets == null || itemSubsets.length == 0) return true;

    Set<String> active = Set.of(activeSubsets);
    boolean hasPositiveSubset = false;

    for (String subset : itemSubsets) {
      if (subset.startsWith("-")) {
        if (active.contains(subset.substring(1))) return false;
      } else {
        hasPositiveSubset = true;
        if (active.contains(subset)) return true;
      }
    }

    return !hasPositiveSubset;
  }
}
