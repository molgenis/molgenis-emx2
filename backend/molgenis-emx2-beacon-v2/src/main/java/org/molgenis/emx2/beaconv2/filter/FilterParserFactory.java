package org.molgenis.emx2.beaconv2.filter;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class FilterParserFactory {

  public static FilterParser getParserForRequest(BeaconRequestBody requestBody) {

    BeaconSpec beaconSpec = requestBody.getMeta().getSpecification();
    if (beaconSpec == BeaconSpec.BEACON_VP) {
      return new FilterParserVP(requestBody.getQuery());
    } else if (beaconSpec == BeaconSpec.BEACON_V2) {
      if (requestBody.getQuery().getEntryType() == EntryType.GENOMIC_VARIANT) {
        return new VariantFilterParser(requestBody.getQuery());
      }
      return new RegularFilterParser(requestBody.getQuery());
    } else {
      throw new MolgenisException("Invalid beacon specification: " + beaconSpec);
    }
  }
}
