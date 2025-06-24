package org.molgenis.emx2.beaconv2.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestParameters;

public class VariantFilterParser implements FilterParser {

  private final BeaconQuery beaconQuery;

  private final String qReferenceName;
  private final Long[] qStart;
  private final Long[] qEnd;
  private final String qReferenceBases;
  private final String qAlternateBases;
  private final String qGeneId;

  private final List<String> graphQlFilters = new ArrayList<>();

  public VariantFilterParser(BeaconQuery query) {
    this.beaconQuery = query;

    qReferenceName = getParam("referenceName");
    qStart = parseCoordinatesFromRequest(getParam("start"));
    qEnd = parseCoordinatesFromRequest(getParam("end"));
    qGeneId = getParam("geneId");
    qReferenceBases = getParam("referenceBases");
    qAlternateBases = getParam("alternateBases");
  }

  @Override
  public FilterParser parse() {
    GenomicQueryType queryType = getGenomicQueryType();
    String graphQlFilter =
        switch (queryType) {
          case SEQUENCE ->
              queryType
                  .getGraphQlQuery()
                  .formatted(qStart[0], qReferenceName, qReferenceBases, qAlternateBases);
          case RANGE -> queryType.getGraphQlQuery().formatted(qReferenceName, qStart[0], qEnd[0]);
          case GENE_ID -> queryType.getGraphQlQuery().formatted(qGeneId);
          case BRACKET ->
              queryType
                  .getGraphQlQuery()
                  .formatted(qReferenceName, qStart[0], qStart[1], qEnd[0], qEnd[1]);
          case NO_PARAMS -> null;
        };
    if (graphQlFilter != null) {
      graphQlFilters.add(graphQlFilter);
    }
    return this;
  }

  @Override
  public List<Filter> getUnsupportedFilters() {
    return List.of();
  }

  @Override
  public List<String> getWarnings() {
    return List.of();
  }

  @Override
  public boolean hasWarnings() {
    return false;
  }

  @Override
  public List<String> getGraphQlFilters() {
    List<String> filters = new ArrayList<>(graphQlFilters);
    String urlPathFilter = getUrlPathFilter(beaconQuery);
    if (urlPathFilter != null) filters.add(urlPathFilter);

    return filters;
  }

  private String getParam(String param) {
    Map<String, BeaconRequestParameters> params = beaconQuery.getRequestParametersMap();
    if (params.containsKey(param)) {
      return params.get(param).getDescription();
    }
    return null;
  }

  private GenomicQueryType getGenomicQueryType() {
    if ((qReferenceName != null && qStart != null) && (qGeneId == null)) {
      if (qEnd != null) {
        if (qStart.length == 1 && qEnd.length == 1) {
          return GenomicQueryType.RANGE;
        } else if (qStart.length == 2 && qEnd.length == 2) {
          return GenomicQueryType.BRACKET;
        } else {
          throw new MolgenisException(
              "Bad request. Start and end parameters supplied, but both must either be of length 1 (range query) or 2 (bracket query)");
        }
      } else {
        if (qReferenceBases == null || qAlternateBases == null) {
          throw new MolgenisException(
              "Bad request. Sequence query missing referenceBases and/or alternateBases parameters");
        } else {
          return GenomicQueryType.SEQUENCE;
        }
      }
    } else if (qGeneId != null) {
      return GenomicQueryType.GENE_ID;
    } else {
      if (qReferenceName == null
          && qStart == null
          && qEnd == null
          && qReferenceBases == null
          && qAlternateBases == null) {
        return GenomicQueryType.NO_PARAMS;
      } else {
        throw new MolgenisException(
            "Bad request. Must at least supply: referenceName and start, or geneId");
      }
    }
  }

  private Long[] parseCoordinatesFromRequest(String value) {
    if (value == null) {
      return null;
    }
    String[] split = value.split(",", -1);
    Long[] result = new Long[split.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = Long.parseLong(split[i]);
    }
    return result;
  }
}
