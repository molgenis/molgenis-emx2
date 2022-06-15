package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.common.misc.Info;

public class BeaconResultsetResponse {
  BeaconResponseMeta meta;
  BeaconSummaryResponseSection responseSummary;
  BeaconResultsets response;
  Info info;
  Handover[] beaconHandovers;

  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconResultsets.json
  public static class BeaconResultsets {
    String $schema;
    ResultsetInstance[] resultSets;

    public class ResultsetInstance {
      // id of the resultset
      String id;

      // Entry type of resultSet. It SHOULD MATCH an entry type declared as collection in the Beacon
      // configuration.
      String setType;

      boolean exists;

      // Number of results in this Resultset.
      int resultsCount;

      // List of handovers that apply to this resultset, not to the whole Beacon or to a result in
      // particular.
      Handover[] resultsHandovers;

      //
      Map<String, String>[] results;
    }
  }
}
