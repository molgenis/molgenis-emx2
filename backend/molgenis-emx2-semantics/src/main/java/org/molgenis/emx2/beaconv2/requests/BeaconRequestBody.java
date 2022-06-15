package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses.HIT;

import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.common.misc.Pagination;

public class BeaconRequestBody {
  String $schema;
  BeaconRequestMeta meta;
  BeaconQuery query;

  public static class BeaconQuery {
    BeaconRequestParameters requestParameters;
    BeaconFilteringTerms[] filters;
    IncludedResultsetResponses includeResultsetResponses = HIT;
    Pagination pagination = new Pagination();
    Granularity requestGranularity;
    boolean testMode;
  }

  public static class BeaconFilteringTerms {
    enum Similarity {
      EXACT,
      HIGH,
      MEDIUM,
      LOW
    }

    public class Filter {
      String id;
      String scope;
    }

    private class OntologyFilter extends Filter {
      boolean includeDescendantTerms;
      Similarity similarity;
    }

    private class AlphanumericFilter extends Filter {
      BeaconFilterOperator operator;
      String value;
    }

    private class CustomFilter extends Filter {}

    Filter[] items;

    public enum BeaconFilterOperator {
      EQ("="),
      LT("<"),
      GT(">"),
      NOT("!"),
      LTE("<="),
      GTE(">=");

      private String value;

      BeaconFilterOperator(String value) {
        this.value = value;
      }

      public String toString() {
        return value;
      }
    }
  }

  public static class BeaconRequestParameters {}
}
