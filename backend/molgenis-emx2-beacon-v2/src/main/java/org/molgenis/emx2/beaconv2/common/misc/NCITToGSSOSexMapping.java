package org.molgenis.emx2.beaconv2.common.misc;

import java.util.Map;

public final class NCITToGSSOSexMapping {
  static final Map<String, String> MAPPING =
      Map.of(
          "//purl.obolibrary.org/obo/NCIT_C16576", "GSSO_000123",
          "ncit:C16576", "GSSO_000123",
          "//purl.obolibrary.org/obo/NCIT_C20197", "GSSO_000124",
          "ncit:C20197", "GSSO_000124",
          "//purl.obolibrary.org/obo/NCIT_C124294", "GSSO_009509",
          "ncit:C124294", "GSSO_009509",
          "//purl.obolibrary.org/obo/NCIT_C17998", "GSSO_009515",
          "ncit:C17998", "GSSO_009515");

  // todo also map Undetermined/Unknown to "assigned no gender at birth" ?

  private NCITToGSSOSexMapping() {
    // no instances
  }

  public static String[] toGSSO(String[] ncit) {
    String[] gsso = new String[ncit.length];
    for (int i = 0; i < ncit.length; i++) {
      gsso[i] = MAPPING.containsKey(ncit[i]) ? MAPPING.get(ncit[i]) : ncit[i];
    }
    return gsso;
  }
}
