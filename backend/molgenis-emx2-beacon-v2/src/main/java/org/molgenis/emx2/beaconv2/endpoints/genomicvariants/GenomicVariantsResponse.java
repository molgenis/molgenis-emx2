package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Table;
import spark.Request;

/**
 * Depending on request parameters, different filtering is applied: only start: "Sequence Query"
 * start AND end: "Range Query" start[0,1] and end[0,1]: "Bracket Query" with GeneId: "GeneId Query"
 * (similar to Bracket, using gene coordinates)
 *
 * <p>see: https://docs.genomebeacons.org/variant-queries/#beacon-sequence-queries
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude() GenomicVariantsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private final String qReferenceName;
  @JsonIgnore private final Long[] qStart;
  @JsonIgnore private final Long[] qEnd;
  @JsonIgnore private final String qReferenceBases;
  @JsonIgnore private final String qAlternateBases;
  @JsonIgnore private String qGeneId;

  public GenomicVariantsResponse(Request request, List<Table> genomicVariantTables)
      throws Exception {

    List<GenomicVariantsResultSets> resultSetsList = new ArrayList<>();
    qReferenceName = request.queryParams("referenceName");
    qStart = parseCoordinatesFromRequest(request, "start");
    qEnd = parseCoordinatesFromRequest(request, "end");
    qGeneId = request.queryParams("geneId");
    qReferenceBases = request.queryParams("referenceBases");
    qAlternateBases = request.queryParams("alternateBases");
    qGeneId = request.queryParams("geneId");

    GenomicQueryType genomicQueryType;
    if ((qReferenceName != null && qStart != null) && (qGeneId == null)) {
      if (qEnd != null) {
        if (qStart.length == 1 && qEnd.length == 1) {
          genomicQueryType = GenomicQueryType.RANGE;
        } else if (qStart.length == 2 && qEnd.length == 2) {
          genomicQueryType = GenomicQueryType.BRACKET;
        } else {
          throw new Exception(
              "Bad request. Start and end parameters supplied, but both must either be of length 1 (range query) or 2 (bracket query)");
        }
      } else {
        if (qReferenceBases == null || qAlternateBases == null) {
          throw new Exception(
              "Bad request. Sequence query missing referenceBases and/or alternateBases parameters");
        } else {
          genomicQueryType = GenomicQueryType.SEQUENCE;
        }
      }
    } else if (qGeneId != null) {
      genomicQueryType = GenomicQueryType.GENEID;
    } else {
      if (qReferenceName == null
          && qStart == null
          && qEnd == null
          && qReferenceBases == null
          && qAlternateBases == null) {
        genomicQueryType = GenomicQueryType.NO_REQUEST_PARAMS;
      } else {
        throw new Exception(
            "Bad request. Must at least supply: referenceName and start, or geneId");
      }
    }

    if (genomicQueryType == GenomicQueryType.NO_REQUEST_PARAMS) {
      // must return an empty resultSets object
      this.resultSets = resultSetsList.toArray(new GenomicVariantsResultSets[0]);
      return;
    }

    // each schema has 0 or 1 'GenomicVariations' table
    // each table match yields 1 GenomicVariantsResultSets
    // each row becomes a GenomicVariantsResultSetsItem
    for (Table table : genomicVariantTables) {
      resultSetsList.addAll(
          GenomicQuery.genomicQuery(
              table,
              genomicQueryType,
              qReferenceName,
              qGeneId,
              qStart,
              qEnd,
              qReferenceBases,
              qAlternateBases));
    }
    this.resultSets = resultSetsList.toArray(new GenomicVariantsResultSets[0]);
  }

  /** Helper function to extract coordinate long arrays from request */
  private Long[] parseCoordinatesFromRequest(Request request, String param) {
    String value = request.queryParams(param);
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
