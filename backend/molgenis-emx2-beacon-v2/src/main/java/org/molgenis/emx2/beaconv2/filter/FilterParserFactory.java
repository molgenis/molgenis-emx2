package org.molgenis.emx2.beaconv2.filter;

import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class FilterParserFactory {

  public static FilterParser getParserForRequest(BeaconRequestBody requestBody) {
    if (requestBody.getQuery().getEntryType() == EntryType.GENOMIC_VARIANT) {
      return new VariantFilterParser(requestBody.getQuery());
    }
    return new FilterParserEjpRd(requestBody.getQuery());
  }
}
