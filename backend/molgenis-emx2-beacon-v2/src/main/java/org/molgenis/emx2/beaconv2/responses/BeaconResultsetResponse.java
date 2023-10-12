package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.common.misc.Info;

public class BeaconResultsetResponse {
  private BeaconResponseMeta meta;
  private BeaconSummaryResponseSection responseSummary;
  private BeaconResultsets response;
  private Info info;
  private Handover[] beaconHandovers;

  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconResultsets.json
  public static class BeaconResultsets {
    private String $schema;
    private ResultsetInstance[] resultSets;

    public class ResultsetInstance {
      // id of the resultset
      private String id;

      // Entry type of resultSet. It SHOULD MATCH an entry type declared as collection in the Beacon
      // configuration.
      private String setType;

      private boolean exists;

      // Number of results in this Resultset.
      private int resultsCount;

      // List of handovers that apply to this resultset, not to the whole Beacon or to a result in
      // particular.
      private Handover[] resultsHandovers;

      //
      private Map<String, String>[] results;
    }
  }
}
