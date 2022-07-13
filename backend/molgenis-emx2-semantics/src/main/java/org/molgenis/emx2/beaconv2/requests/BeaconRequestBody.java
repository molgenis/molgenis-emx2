package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses.HIT;

import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.common.misc.Pagination;

public class BeaconRequestBody {
  private String $schema;
  private BeaconRequestMeta meta;
  private BeaconQuery query;

  public static class BeaconQuery {
    private BeaconRequestParameters requestParameters;
    private BeaconFilteringTerms[] filters;
    private IncludedResultsetResponses includeResultsetResponses = HIT;
    private Pagination pagination = new Pagination();
    private Granularity requestGranularity;
    private boolean testMode;
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

      @Override
      public String toString() {
        return value;
      }
    }
  }

  public static class BeaconRequestParameters {}
}
