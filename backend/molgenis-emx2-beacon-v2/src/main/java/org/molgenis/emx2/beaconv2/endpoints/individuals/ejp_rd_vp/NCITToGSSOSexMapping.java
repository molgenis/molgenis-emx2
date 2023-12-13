package org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp;

import java.util.HashMap;

public class NCITToGSSOSexMapping {

  public NCITToGSSOSexMapping() {
    super();
  }

  public HashMap getMapping() {
    HashMap<String, String> mapping = new HashMap<>();

    // NCIT "Female". A person who belongs to the sex that normally produces ova. The term is
    // used to indicate biological sex distinctions, or cultural gender role distinctions, or
    // both.
    mapping.put("//purl.obolibrary.org/obo/NCIT_C16576", "GSSO_000123");
    mapping.put("NCIT_C16576", "GSSO_000123");

    // NCIT "Male". A person who belongs to the sex that normally produces sperm. The term is
    // used to indicate biological sex distinctions, cultural gender role distinctions, or both.
    mapping.put("//purl.obolibrary.org/obo/NCIT_C20197", "GSSO_000124");
    mapping.put("NCIT_C20197", "GSSO_000124");

    // NCIT "Undetermined". A term referring to the lack of definitive criteria for
    // classification of a finding.
    mapping.put("//purl.obolibrary.org/obo/NCIT_C124294", "GSSO_009509");
    mapping.put("NCIT_C124294", "GSSO_009509");

    // NCIT "Unknown". Not known, observed, recorded; or reported as unknown by the data
    // contributor.
    mapping.put("//purl.obolibrary.org/obo/NCIT_C17998", "GSSO_009515");
    mapping.put("NCIT_C17998", "GSSO_009515");

    // todo also map Undetermined/Unknown to "assigned no gender at birth" ?

    return mapping;
  }
}
