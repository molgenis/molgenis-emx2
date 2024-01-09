package org.molgenis.emx2.beaconv2.configuration;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;
import org.molgenis.emx2.beaconv2.common.EntryType;
import org.molgenis.emx2.beaconv2.common.SecurityLevel;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;

public class BeaconConfigurationSchema {
  private String $schema;
  private MaturityAttributes maturityAttributes;
  private SecurityAttributes securityAttributes;

  // this one is dynamic, depending on implemented models
  private Map<String, EntryType> entryTypes;

  public static class MaturityAttributes {
    BeaconEnvironment productionStatus;
  }

  public static class SecurityAttributes {
    Granularity defaultGranularity;
    SecurityLevel[] securityLevels;
  }
}
