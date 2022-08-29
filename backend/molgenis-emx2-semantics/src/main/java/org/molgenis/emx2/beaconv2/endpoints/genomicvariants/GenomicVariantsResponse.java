package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.BETWEEN;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.selectColumns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Query;
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

      // todo case insensitive matching needed! (e.g. C -> c/G and c -> c/G)
      // todo not good enough - only 'name' of OntologyTerm inside a reference can be queried this way, we miss code and codesystem! replace by GraphQL
      Query query = table.query();
      selectColumns(table, query);

      switch (genomicQueryType) {
        case SEQUENCE -> query.where(
            and(
                f("position_start", EQUALS, qStart[0]),
                f("position_refseqId", EQUALS, qReferenceName),
                or(
                    f("referenceBases", EQUALS, qReferenceBases.toLowerCase()),
                    f("referenceBases", EQUALS, qReferenceBases.toUpperCase())),
                or(
                    f("alternateBases", EQUALS, qAlternateBases.toLowerCase()),
                    f("alternateBases", EQUALS, qAlternateBases.toUpperCase()))));

          // todo optional parameter: datasetIds
          // todo optional parameter: filters
        case RANGE ->
        // "Range Query"
        query.where(
            or(
                and(
                    f("position_start", BETWEEN, (Object[]) new Long[] {qStart[0], qEnd[0]}),
                    f("position_refseqId", EQUALS, qReferenceName)),
                and(
                    f("position_end", BETWEEN, (Object[]) new Long[] {qStart[0], qEnd[0]}),
                    f("position_refseqId", EQUALS, qReferenceName))));

          // todo optional parameter: variantType OR alternateBases OR aminoacidChange
          // todo optional parameter: variantMinLength
          // todo optional parameter: variantMaxLength
        case BRACKET -> query.where(
            and(
                f("position_start", BETWEEN, (Object[]) new Long[] {qStart[0], qStart[1]}),
                f("position_end", BETWEEN, (Object[]) new Long[] {qEnd[0], qEnd[1]}),
                f("position_refseqId", EQUALS, qReferenceName)));

          // todo optional parameter: variantType

        case GENEID ->
        // "GeneId Query"
        query.where(f("geneId", EQUALS, qGeneId));

          // todo: required parameter 'geneId'
          // todo optional parameter: variantType OR alternateBases OR aminoacidChange
          // todo optional parameter: variantMinLength
          // todo optional parameter: variantMaxLength
      }

      String queryResultJSON = query.retrieveJSON();
      Map<String, Object> queryResult = new ObjectMapper().readValue(queryResultJSON, Map.class);
      List<Map<String, Object>> gvarListFromJSON =
          (List<Map<String, Object>>) queryResult.get("GenomicVariations");

      List<GenomicVariantsResultSetsItem> genomicVariantsItemList = new ArrayList<>();

      if (gvarListFromJSON != null) {
        for (Map map : gvarListFromJSON) {
          GenomicVariantsResultSetsItem genomicVariantsItem = new GenomicVariantsResultSetsItem();
          genomicVariantsItem.setVariantInternalId((String) map.get("variantInternalId"));
          genomicVariantsItem.setVariantType((String) map.get("variantType"));
          genomicVariantsItem.setReferenceBases((String) map.get("referenceBases"));
          genomicVariantsItem.setAlternateBases((String) map.get("alternateBases"));
          genomicVariantsItem.setGeneId((String) map.get("geneId"));
          genomicVariantsItem.setPosition(
              new Position(
                  (String) map.get("position_assemblyId"),
                  (String) map.get("position_refseqId"),
                  // FIXME: should be Long but that results in class java.lang.Integer cannot be
                  // cast to class java.lang.Long (java.lang.Integer and java.lang.Long are in
                  // module java.base of loader 'bootstrap') see
                  // https://github.com/molgenis/molgenis-emx2/issues/1547
                  new Long[] {((Integer) map.get("position_start")).longValue()},
                  new Long[] {((Integer) map.get("position_end")).longValue()}));
          genomicVariantsItem.setVariantLevelData(
              new VariantLevelData(
                  ClinicalInterpretations.get(map.get("clinicalInterpretations"))));
          genomicVariantsItemList.add(genomicVariantsItem);
        }
      }
      if (genomicVariantsItemList.size() > 0) {
        GenomicVariantsResultSets genomicVariantsResultSets =
            new GenomicVariantsResultSets(
                table.getSchema().getName(),
                genomicVariantsItemList.size(),
                genomicVariantsItemList.toArray(new GenomicVariantsResultSetsItem[0]));
        resultSetsList.add(genomicVariantsResultSets);
      }
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
