package org.molgenis.emx2.beaconv2.responses.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.common.CommonSchemas;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReceivedRequestSummary {

  String apiVersion = "v2.0.0-draft.4"; // TODO CommonMeta.apiVersion but not allowed static?
  String requestedGranularity = "record"; // TODO default? get from request?
  Pagination pagination = new Pagination();
  CommonSchemas[] requestedSchemas =
      Arrays.asList(new CommonSchemas("datasets")).toArray(CommonSchemas[]::new);
}
