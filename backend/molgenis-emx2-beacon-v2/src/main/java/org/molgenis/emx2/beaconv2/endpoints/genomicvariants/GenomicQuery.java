package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.*;
import static org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicQueryType.BRACKET;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class GenomicQuery {

  public static final String GENOMIC_VARIATIONS_TABLE_NAME = "GenomicVariations";

  public static ArrayNode genomicQuery(
      Table table,
      GenomicQueryType genomicQueryType,
      String qReferenceName,
      String qGeneId,
      Long[] qStart,
      Long[] qEnd,
      String qReferenceBases,
      String qAlternateBases) {

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

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode results =
        (ArrayNode)
            mapper.valueToTree(executionResult.getData()).get(GENOMIC_VARIATIONS_TABLE_NAME);
    return results;
  }
}
