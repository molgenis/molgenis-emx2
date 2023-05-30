package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.*;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.BRACKET;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;

public class GenomicQuery {

  public static final String GENOMIC_VARIATIONS_TABLE_NAME = "GenomicVariations";

  public static List<GenomicVariantsResultSets> genomicQuery(
      Table table,
      GenomicQueryType genomicQueryType,
      String qReferenceName,
      String qGeneId,
      Long[] qStart,
      Long[] qEnd,
      String qReferenceBases,
      String qAlternateBases) {
    List<GenomicVariantsResultSets> resultSetsList = new ArrayList<>();
    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
    ExecutionResult executionResult =
        grapql.execute(
            "{"
                + GENOMIC_VARIATIONS_TABLE_NAME
                + ""

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
                    ? "(filter: { _and: [ { position_start: {equals:"
                        + qStart[0]
                        + "}}, {position_refseqId: {equals:\""
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
                    ? "(filter: { _or: [ { _and: [{ position_refseqId: { equals:\""
                        + qReferenceName
                        + "\"}}, { position_start: { between: ["
                        + qStart[0]
                        + ","
                        + qEnd[0]
                        + "]}}]}, { _and: [{ position_refseqId: { equals:\""
                        + qReferenceName
                        + "\"}} ,{ position_end: { between: ["
                        + qStart[0]
                        + ","
                        + qEnd[0]
                        + "]}}]}]})"
                    : "")

                // todo optional parameter: variantType
                + (genomicQueryType == BRACKET
                    ? "(filter: { _and: [{position_refseqId: { equals:\""
                        + qReferenceName
                        + "\"}},{position_start: { between: ["
                        + qStart[0]
                        + ","
                        + qStart[1]
                        + "]}},{position_end: { between: ["
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
                + "position_assemblyId,"
                + "position_refseqId,"
                + "position_start,"
                + "position_end,"
                + "geneId,"
                + "genomicHGVSId,"
                + "proteinHGVSIds,"
                + "transcriptHGVSIds,"
                + "clinicalInterpretations{"
                + "   category{name,codesystem,code,ontologyTermURI},"
                + "   clinicalRelevance{name,codesystem,code,ontologyTermURI},"
                + "   conditionId,"
                + "   effect{name,codesystem,code,ontologyTermURI}"
                + "},"
                + "caseLevelData{"
                + "   individualId{id},"
                + "   clinicalInterpretations{"
                + "      category{name,codesystem,code,ontologyTermURI},"
                + "      clinicalRelevance{name,codesystem,code,ontologyTermURI},"
                + "      conditionId,"
                + "      effect{name,codesystem,code,ontologyTermURI}"
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
                TypeUtils.toString(map.get("position_assemblyId")),
                TypeUtils.toString(map.get("position_refseqId")),
                (map.get("position_start") != null)
                    ? new Long[] {TypeUtils.toLong(map.get("position_start"))}
                    : null,
                (map.get("position_end") != null)
                    ? new Long[] {TypeUtils.toLong(map.get("position_end"))}
                    : null));
        VariantLevelData variantLevelData =
            new VariantLevelData(ClinicalInterpretations.get(map.get("clinicalInterpretations")));
        if (variantLevelData.getClinicalInterpretations() != null
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
    return resultSetsList;
  }
}
