package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.common.Schemas;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReceivedRequestSummary {

  private String apiVersion = "v2.0.0-draft.4"; // TODO CommonMeta.apiVersion but not allowed static?
  private String requestedGranularity = "record"; // TODO default? get from request?
  private Pagination pagination = new Pagination();
  private Schemas[] requestedSchemas = Arrays.asList(new Schemas("datasets")).toArray(Schemas[]::new);
}
