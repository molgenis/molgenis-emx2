package org.molgenis.emx2.beaconv2.responses;

import org.molgenis.emx2.beaconv2.common.misc.BeaconInformationalResponseMeta;

public class BeaconFilteringTermsResponse {
  BeaconInformationalResponseMeta meta;
  BeaconFilteringTermsResults response;

  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconFilteringTermsResults.json
  public static class BeaconFilteringTermsResults {
    // Ontology resources defined externally to this beacon implementation
    Resource[] resources;

    // Entities can be filtered using this term
    FilteringTerm[] filteringTerms;

    // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconFilteringTermsResults.json
    public static class FilteringTerm {
      // Either \"custom\", \"alphanumeric\" or ontology/terminology full name.
      String type;

      // The field id in the case of numeric or alphanumeric fields, or the term id in the case of
      // ontology or custom terms. CURIE syntax in the case of an ontology term.
      String id;

      // This would be the \"preferred Label\" in the case of an ontology term.
      String label;
    }

    // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconFilteringTermsResults.json#Resource
    public static class Resource {
      // "OBO ID representing the resource"
      String id;

      // The name of the ontology referred to by the id element
      String type;

      // Uniform Resource Locator of the resource
      String url;

      // The version of the resource or ontology used to make the annotation
      String version;

      // The prefix used in the CURIE of an OntologyClass
      String nameSpacePrefix;

      // The full Internationalized Resource Identifier (IRI) prefix
      String iriPrefix;
    }
  }
}
