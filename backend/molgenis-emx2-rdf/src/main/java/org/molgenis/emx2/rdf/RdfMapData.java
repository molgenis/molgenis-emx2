package org.molgenis.emx2.rdf;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;

public class RdfMapData {
  private final String baseURL;
  private final OntologyIriMapper ontologyIriMapper;

  public RdfMapData(String baseURL, OntologyIriMapper ontologyIriMapper) {
    this.baseURL = formatBaseURL(baseURL);
    this.ontologyIriMapper = requireNonNull(ontologyIriMapper);
  }

  public String getBaseURL() {
    return baseURL;
  }

  public OntologyIriMapper getOntologyIriMapper() {
    return ontologyIriMapper;
  }
}
