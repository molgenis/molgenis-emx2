package org.molgenis.emx2.cafevariome.post.request.query;

public class DemographyQuery {

  private int minAge;
  private int maxAge;
  private String affected;
  private String gender;
  private String family_type;

  public int getMinAge() {
    return minAge;
  }

  public void setMinAge(int minAge) {
    this.minAge = minAge;
  }

  public int getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(int maxAge) {
    this.maxAge = maxAge;
  }

  public String getAffected() {
    return affected;
  }

  public void setAffected(String affected) {
    this.affected = affected;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getFamily_type() {
    return family_type;
  }

  public void setFamily_type(String family_type) {
    this.family_type = family_type;
  }
}
