package org.molgenis.emx2.datamodels.profiles;

import org.molgenis.emx2.Profile;

// pojo to load profile migration from yaml
public class ProfileMigration {
  private Profile profile;
  private Integer step;

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    String inSnakeCase =
        profile
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toUpperCase();
    this.profile = Profile.valueOf(inSnakeCase);
  }

  public Integer getStep() {
    return step;
  }

  public void setStep(String step) {
    this.step = Integer.valueOf(step);
  }
}
