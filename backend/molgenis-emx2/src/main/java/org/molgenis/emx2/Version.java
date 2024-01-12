package org.molgenis.emx2;

public class Version {

  public static final String DEVELOPMENT = "DEVELOPMENT";

  private Version() {
    // hide constructor
  }

  public static String getSpecificationVersion() {
    String result = Version.class.getPackage().getSpecificationVersion();
    if (result == null) return DEVELOPMENT;
    return result;
  }

  public static String getImplementationVersion() {
    String result = Version.class.getPackage().getImplementationVersion();
    if (result == null) return DEVELOPMENT;
    return result;
  }

  public static String getVersion() {
    if (DEVELOPMENT.equals(getSpecificationVersion())) {
      return DEVELOPMENT;
    }
    return getSpecificationVersion() + " (git:" + getImplementationVersion() + ")";
  }
}
