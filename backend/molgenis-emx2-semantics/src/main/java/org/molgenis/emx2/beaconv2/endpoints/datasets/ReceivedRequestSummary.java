package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.common.Schemas;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReceivedRequestSummary {

  String apiVersion = "v2.0.0-draft.4"; // TODO CommonMeta.apiVersion but not allowed static?
  String requestedGranularity = "record"; // TODO default? get from request?
  Pagination pagination = new Pagination();
  Schemas[] requestedSchemas = Arrays.asList(new Schemas("datasets")).toArray(Schemas[]::new);
}
