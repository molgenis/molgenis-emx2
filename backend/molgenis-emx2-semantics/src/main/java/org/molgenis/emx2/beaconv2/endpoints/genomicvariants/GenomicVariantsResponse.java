package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;
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

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{GenomicVariations"

                  // todo optional parameter: variantType OR alternateBases OR aminoacidChange
                  // todo optional parameter: variantMinLength
                  // todo optional parameter: variantMaxLength
                  + (genomicQueryType == GENEID
                      ? "(filter:{geneId: {equals:\"" + qGeneId + "\"}})"
                      : "")

                  // todo optional parameter: datasetIds
                  // todo optional parameter: filters
                  // fixme 'like' is greedy but allows case insensitivity...
                  + (genomicQueryType == SEQUENCE
                      ? "(filter: { _and: [ { position__start: {equals:"
                          + qStart[0]
                          + "}}, {position__refseqId: {equals:\""
                          + qReferenceName
                          + "\"}}, {referenceBases: {like: \""
                          + qReferenceBases
                          + "\"}}, {alternateBases: {like: \""
                          + qAlternateBases
                          + "\"}}]}) "
                      : "")

                  // todo optional parameter: variantType OR alternateBases OR aminoacidChange
                  // todo optional parameter: variantMinLength
                  // todo optional parameter: variantMaxLength
                  + (genomicQueryType == RANGE
                      ? "(filter: { _or: [ { _and: [{ position__refseqId: { equals:\""
                          + qReferenceName
                          + "\"}}, { position__start: { between: ["
                          + qStart[0]
                          + ","
                          + qEnd[0]
                          + "]}}]}, { _and: [{ position__refseqId: { equals:\""
                          + qReferenceName
                          + "\"}} ,{ position__end: { between: ["
                          + qStart[0]
                          + ","
                          + qEnd[0]
                          + "]}}]}]})"
                      : "")

                  // todo optional parameter: variantType
                  + (genomicQueryType == BRACKET
                      ? "(filter: { _and: [{position__refseqId: { equals:\""
                          + qReferenceName
                          + "\"}},{position__start: { between: ["
                          + qStart[0]
                          + ","
                          + qStart[1]
                          + "]}},{position__end: { between: ["
                          + qEnd[0]
                          + ","
                          + qEnd[1]
                          + "]}}]})"
                      : "")
                  + "{"
                  + "variantInternalId,"
                  + "variantType,"
                  + "referenceBases,"
                  + "alternateBases,"
                  + "position__assemblyId,"
                  + "position__refseqId,"
                  + "position__start,"
                  + "position__end,"
                  + "geneId,"
                  + "genomicHGVSId,"
                  + "proteinHGVSIds,"
                  + "transcriptHGVSIds,"
                  + "clinicalInterpretations{"
                  + "   category{name,codesystem,code},"
                  + "   clinicalRelevance{name,codesystem,code},"
                  + "   conditionId,"
                  + "   effect{name,codesystem,code}"
                  + "},"
                  + "caseLevelData{"
                  + "   individualId{id},"
                  + "   clinicalInterpretations{"
                  + "      category{name,codesystem,code},"
                  + "      clinicalRelevance{name,codesystem,code},"
                  + "      conditionId,"
                  + "      effect{name,codesystem,code}"
                  + "    }"
                  + "  }"
                  + "}}");
      // todo case insensitive matching needed! (e.g. C -> c/G and c -> c/G)

      Map<String, Object> result = executionResult.toSpecification();
      List<Map<String, Object>> gvarListFromJSON =
          (List<Map<String, Object>>)
              ((HashMap<String, Object>) result.get("data")).get("GenomicVariations");

      List<GenomicVariantsResultSetsItem> genomicVariantsItemList = new ArrayList<>();

      if (gvarListFromJSON != null) {
        for (Map map : gvarListFromJSON) {
          GenomicVariantsResultSetsItem genomicVariantsItem = new GenomicVariantsResultSetsItem();
          genomicVariantsItem.setVariantInternalId((String) map.get("variantInternalId"));
          genomicVariantsItem.setVariantType((String) map.get("variantType"));
          genomicVariantsItem.setReferenceBases((String) map.get("referenceBases"));
          genomicVariantsItem.setAlternateBases((String) map.get("alternateBases"));
          genomicVariantsItem.setGeneId((String) map.get("geneId"));
          genomicVariantsItem.setGenomicHGVSId((String) map.get("genomicHGVSId"));
          if (map.get("proteinHGVSIds") != null) {
            genomicVariantsItem.setProteinHGVSIds(
                ((ArrayList<String>) map.get("proteinHGVSIds")).toArray(new String[0]));
          }
          if (map.get("transcriptHGVSIds") != null) {
            genomicVariantsItem.setTranscriptHGVSIds(
                ((ArrayList<String>) map.get("transcriptHGVSIds")).toArray(new String[0]));
          }
          genomicVariantsItem.setPosition(
              new Position(
                  TypeUtils.toString(map.get("position__assemblyId")),
                  TypeUtils.toString(map.get("position__refseqId")),
                  new Long[] {TypeUtils.toLong(map.get("position__start"))},
                  new Long[] {TypeUtils.toLong(map.get("position__end")).longValue()}));
          VariantLevelData variantLevelData =
              new VariantLevelData(ClinicalInterpretations.get(map.get("clinicalInterpretations")));
          if (variantLevelData != null
              && variantLevelData.getClinicalInterpretations() != null
              && variantLevelData.getClinicalInterpretations().length > 0) {
            genomicVariantsItem.setVariantLevelData(variantLevelData);
          }
          CaseLevelData[] caseLevelData = CaseLevelData.get(map.get("caseLevelData"));
          if (caseLevelData != null) {
            genomicVariantsItem.setCaseLevelData(caseLevelData);
          }
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
