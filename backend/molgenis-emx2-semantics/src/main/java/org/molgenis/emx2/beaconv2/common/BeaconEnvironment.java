package org.molgenis.emx2.beaconv2.common;

public enum BeaconEnvironment {
  PROD,
  TEST,
  DEV,
  STAGING;

  public String toString() {
    return name().toLowerCase();
  }
}
