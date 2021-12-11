package org.molgenis.emx2.beacon.common;

public enum BeaconEnvironment {
  PROD,
  TEST,
  DEV,
  STAGING;

  public String toString() {
    return name().toLowerCase();
  }
}
