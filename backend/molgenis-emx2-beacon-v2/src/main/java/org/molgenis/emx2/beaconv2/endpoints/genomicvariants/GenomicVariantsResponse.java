package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.beaconv2.QueryEntryType.getTableFromAllSchemas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Table;
import spark.Request;

/**
 * Depending on request parameters, different filtering is applied: only start: "Sequence Query"
 * start AND end: "Range Query" start[0,1] and end[0,1]: "Bracket Query" with GeneId: "GeneId Query"
 * (similar to Bracket, using gene coordinates)
 *
 * <p>see: https://docs.genomebeacons.org/variant-queries/#beacon-sequence-queries
 */
public class GenomicVariantsResponse {

  // query parameters, ignore from output
  private final String qReferenceName;
  private final Long[] qStart;
  private final Long[] qEnd;
  private final String qReferenceBases;
  private final String qAlternateBases;
  private String qGeneId;

  private JsonNode response;

  public GenomicVariantsResponse(Request request, Database database) throws Exception {

    List<Table> genomicVariantTables = getTableFromAllSchemas(database, "GenomicVariations");

    qReferenceName = request.queryParams("referenceName");
    qStart = parseCoordinatesFromRequest(request, "start");
    qEnd = parseCoordinatesFromRequest(request, "end");
    qGeneId = request.queryParams("geneId");
    qReferenceBases = request.queryParams("referenceBases");
    qAlternateBases = request.queryParams("alternateBases");
    qGeneId = request.queryParams("geneId");

    GenomicQueryType genomicQueryType = getGenomicQueryType();

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode response = mapper.createObjectNode();
    ArrayNode resultSets = mapper.createArrayNode();

    if (genomicQueryType == GenomicQueryType.NO_REQUEST_PARAMS) {
      response.set("resultSets", resultSets);
      this.response = response;
      return;
    }

    // each schema has 0 or 1 'GenomicVariations' table
    // each table match yields 1 GenomicVariantsResultSets
    // each row becomes a GenomicVariantsResultSetsItem

    for (Table table : genomicVariantTables) {
      ArrayNode results =
          GenomicQuery.genomicQuery(
              table,
              genomicQueryType,
              qReferenceName,
              qGeneId,
              qStart,
              qEnd,
              qReferenceBases,
              qAlternateBases);

      ObjectNode resultSet = mapper.createObjectNode();
      resultSet.put("id", table.getSchema().getName());
      resultSet.set("results", results);
      resultSets.add(resultSet);
    }

    response.set("resultSets", resultSets);

    Expression jslt = Parser.compileResource("genomicvariations.jslt");
    this.response = jslt.apply(response);
  }

  public JsonNode getResponse() {
    return response;
  }

  private GenomicQueryType getGenomicQueryType() throws Exception {
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
    return genomicQueryType;
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
