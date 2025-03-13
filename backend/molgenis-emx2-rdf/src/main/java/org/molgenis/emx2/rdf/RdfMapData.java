package org.molgenis.emx2.rdf;

public class RdfMapData {
  private final String baseURL;
  private final OntologyIriMapper ontologyIriMapper;

  public RdfMapData(String baseURL, OntologyIriMapper ontologyIriMapper) {
    String baseUrlTrim = baseURL.trim();
    this.baseURL = baseUrlTrim.endsWith("/") ? baseUrlTrim : baseUrlTrim + "/";
    this.ontologyIriMapper = ontologyIriMapper;
  }

  public String getBaseURL() {
    return baseURL;
  }

  public OntologyIriMapper getOntologyIriMapper() {
    return ontologyIriMapper;
  }
}
