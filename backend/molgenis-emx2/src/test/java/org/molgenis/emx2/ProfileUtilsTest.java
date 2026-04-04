package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ProfileUtilsTest {

  @Test
  void noActiveProfiles_allVisible() {
    assertTrue(ProfileUtils.matchesActiveProfiles(new String[] {"wgs"}, null));
    assertTrue(ProfileUtils.matchesActiveProfiles(new String[] {"wgs"}, new String[0]));
  }

  @Test
  void noItemProfiles_alwaysVisible() {
    assertTrue(ProfileUtils.matchesActiveProfiles(null, new String[] {"wgs"}));
    assertTrue(ProfileUtils.matchesActiveProfiles(new String[0], new String[] {"wgs"}));
  }

  @Test
  void positiveMatch_visible() {
    assertTrue(ProfileUtils.matchesActiveProfiles(new String[] {"wgs"}, new String[] {"wgs"}));
    assertTrue(
        ProfileUtils.matchesActiveProfiles(
            new String[] {"wgs", "rdm"}, new String[] {"wgs", "other"}));
  }

  @Test
  void positiveNoMatch_hidden() {
    assertFalse(
        ProfileUtils.matchesActiveProfiles(new String[] {"wgs"}, new String[] {"rdm", "other"}));
  }

  @Test
  void negativeExclusion_hidden() {
    assertFalse(
        ProfileUtils.matchesActiveProfiles(new String[] {"-core"}, new String[] {"core", "wgs"}));
  }

  @Test
  void negativeNoExclusion_visible() {
    assertTrue(
        ProfileUtils.matchesActiveProfiles(new String[] {"-core"}, new String[] {"wgs", "rdm"}));
  }

  @Test
  void mixedProfiles() {
    assertTrue(
        ProfileUtils.matchesActiveProfiles(
            new String[] {"-core", "wgs"}, new String[] {"wgs", "rdm"}));
    assertFalse(
        ProfileUtils.matchesActiveProfiles(
            new String[] {"-core", "wgs"}, new String[] {"core", "wgs"}));
    assertFalse(
        ProfileUtils.matchesActiveProfiles(new String[] {"-core", "wgs"}, new String[] {"rdm"}));
  }
}
