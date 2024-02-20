package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsFields.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class QueryIndividuals {

  public static final String NAME_CODESYSTEM_CODE = "{name,codesystem,code},";

  /**
   * Construct GraphQL query on Beacon v2 individuals, with optional filters like "{sex:
   * {ontologyTermURI: {like: "http://purl.obolibrary.org/obo/GSSO_000124"}}}", "{ diseases: {
   * diseaseCode: {ontologyTermURI: {like: "Orphanet_1873"}}}}", etc. Beacon supports only AND
   * operator for filters.
   *
   * @param tables
   * @param filters
   * @return
   */
  public static List<IndividualsResultSets> queryIndividuals(
      List<Table> tables, String... filters) {

    StringBuffer concatFilters = new StringBuffer();
    for (String filter : filters) {
      concatFilters.append(filter + ",");
    }
    if (concatFilters.length() > 0) {
      concatFilters.deleteCharAt(concatFilters.length() - 1);
    }

    String graphqlQuery =
        "{Individuals"
            + "(filter: { _and: [ "
            + concatFilters
            + " ] }  )"
            + "{"
            + "id,"
            + "sex{name,codesystem,code},"
            + AGE_AGEGROUP
            + NAME_CODESYSTEM_CODE
            + AGE_AGE_ISO8601DURATION
            + ","
            + "diseaseCausalGenes{name,codesystem,code},"
            + "ethnicity{name,codesystem,code},"
            + "geographicOrigin{name,codesystem,code},"
            + "phenotypicFeatures{"
            + "   featureType{name,codesystem,code},"
            + "   modifiers{name,codesystem,code},"
            + "   severity{name,codesystem,code}},"
            + "diseases{"
            + DISEASECODE
            + NAME_CODESYSTEM_CODE
            + AGEOFONSET_AGEGROUP
            + NAME_CODESYSTEM_CODE
            + AGEOFONSET_AGE_ISO8601DURATION
            + ","
            + AGEATDIAGNOSIS_AGEGROUP
            + NAME_CODESYSTEM_CODE
            + AGEATDIAGNOSIS_AGE_ISO8601DURATION
            + ","
            + FAMILYHISTORY
            + ","
            + SEVERITY
            + NAME_CODESYSTEM_CODE
            + STAGE
            + "{name,codesystem,code}},"
            + "measures{"
            + "   assayCode{name,codesystem,code},"
            + "   date,"
            + "   measurementVariable,"
            + "   measurementValue_value,"
            + "   measurementValue_units{name,codesystem,code},"
            + "   observationMoment_age_iso8601duration"
            + "},"
            + "hasGenomicVariations{"
            + "clinicalInterpretations{"
            + "   category{name,codesystem,code},"
            + "   clinicalRelevance{name,codesystem,code},"
            + "   conditionId,"
            + "   effect{name,codesystem,code}"
            + "},"
            + "}"
            + "}}";

    ObjectMapper mapper = new ObjectMapper();
    Expression jslt = Parser.compileResource("individuals.jslt");

    List<IndividualsResultSets> resultSetsList = new ArrayList<>();
    for (Table table : tables) {
      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult = grapql.execute(graphqlQuery);

      ArrayNode individualsOutput = mapper.createArrayNode();

      JsonNode individuals = mapper.valueToTree(executionResult.getData()).get("Individuals");
      for (JsonNode individual : individuals) individualsOutput.add(jslt.apply(individual));

      if (!individualsOutput.isEmpty()) {
        IndividualsResultSets individualsResultSets =
            new IndividualsResultSets(table.getSchema().getName(), individualsOutput);
        resultSetsList.add(individualsResultSets);
      }
    }
    return resultSetsList;
  }
}
