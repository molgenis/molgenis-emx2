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
    String inSnakeCase = convertToSnakeCase(profile);
    this.profile = Profile.valueOf(inSnakeCase);
  }

  public Integer getStep() {
    return step;
  }

  public void setStep(String step) {
    this.step = Integer.valueOf(step);
  }

  private String convertToSnakeCase(String camelCaseString) {
    if (camelCaseString == null || camelCaseString.isEmpty()) {
      return camelCaseString;
    }

    StringBuilder snakeCaseString = new StringBuilder();
    for (char c : camelCaseString.toCharArray()) {
      if (Character.isUpperCase(c) && !snakeCaseString.isEmpty()) {
        // Add an underscore before the uppercase character
        snakeCaseString.append('_');
        // Convert the uppercase character to lowercase
        snakeCaseString.append(Character.toLowerCase(c));
      } else {
        // Add the lowercase character directly
        snakeCaseString.append(c);
      }
    }

    return snakeCaseString.toString().toUpperCase();
  }
}
