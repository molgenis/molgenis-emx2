package org.molgenis.emx2.rdf;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;

import java.time.ZoneId;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;

public class RdfMapData {
  private final String baseURL;
  private final OntologyIriMapper ontologyIriMapper;
  private final ZoneId zoneId;

  public RdfMapData(String baseURL, OntologyIriMapper ontologyIriMapper, ZoneId zoneId) {
    this.baseURL = formatBaseURL(baseURL);
    this.ontologyIriMapper = requireNonNull(ontologyIriMapper);
    this.zoneId = requireNonNull(zoneId);
  }

  public String getBaseURL() {
    return baseURL;
  }

  public OntologyIriMapper getOntologyIriMapper() {
    return ontologyIriMapper;
  }

  public ZoneId getZoneId() {
    return zoneId;
  }
}
