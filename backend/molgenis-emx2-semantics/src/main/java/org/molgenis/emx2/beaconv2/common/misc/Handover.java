package org.molgenis.emx2.beaconv2.common.misc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/common/beaconCommonComponents.json#Handover
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Handover {
  private HandoverType handoverType;

  // An optional text including considerations on the handover link provided.
  private String note = "MOLGENIS EMX2 Beacon v2 handover";

  // URL endpoint to where the handover process could progress (in RFC\n3986 format)
  private String url = "https://molgenis.org/";

  private class HandoverType {
    // Handover type, as an Ontology_term object with CURIE syntax for the\n`id` value.
    String id = "EMX2:000001";

    // This would be the \"preferred Label\" in the case of an ontology term.
    String label = "MOLGENIS EMX2 Beacon v2 handover";
  }
}
