package org.molgenis.emx2.beaconv2.common.misc;

import java.util.Map;

public class NCITToGSSOSexMapping {
  static final Map<String, String> MAPPING =
      Map.of(
          "//purl.obolibrary.org/obo/NCIT_C16576", "GSSO_000123",
          "NCIT_C16576", "GSSO_000123",
          "//purl.obolibrary.org/obo/NCIT_C20197", "GSSO_000124",
          "NCIT_C20197", "GSSO_000124",
          "//purl.obolibrary.org/obo/NCIT_C124294", "GSSO_009509",
          "NCIT_C124294", "GSSO_009509",
          "//purl.obolibrary.org/obo/NCIT_C17998", "GSSO_009515",
          "NCIT_C17998", "GSSO_009515");
  // todo also map Undetermined/Unknown to "assigned no gender at birth" ?

  public static String[] toGSSO(String[] NCITT) {
    String[] GSSO = new String[NCITT.length];
    for (int i = 0; i < NCITT.length; i++) {
      GSSO[i] = MAPPING.containsKey(NCITT[i]) ? MAPPING.get(NCITT[i]) : GSSO[i];
    }
    return GSSO;
  }
}
