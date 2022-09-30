package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapListToOntologyTerms;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.AgeAndAgeGroup;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;

public class QueryIndividuals {

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
    List<IndividualsResultSets> resultSetsList = new ArrayList<>();

    StringBuffer concatFilters = new StringBuffer();
    for (String filter : filters) {
      concatFilters.append(filter + ",");
    }
    concatFilters.deleteCharAt(concatFilters.length() - 1);

    for (Table table : tables) {
      List<IndividualsResultSetsItem> individualsItemList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Individuals"
                  + "(filter: { _and: [ "
                  + concatFilters
                  + " ] }  )"
                  + "{"
                  + "id,"
                  + "sex{name,codesystem,code},"
                  + "age__ageGroup{name,codesystem,code},"
                  + "age__age__iso8601duration,"
                  + "diseaseCausalGenes{name,codesystem,code},"
                  + "ethnicity{name,codesystem,code},"
                  + "geographicOrigin{name,codesystem,code},"
                  + "phenotypicFeatures{"
                  + "   featureType{name,codesystem,code},"
                  + "   modifiers{name,codesystem,code},"
                  + "   severity{name,codesystem,code}},"
                  + "diseases{"
                  + "   diseaseCode{name,codesystem,code},"
                  + "   ageOfOnset__ageGroup{name,codesystem,code},"
                  + "   ageOfOnset__age__iso8601duration,"
                  + "   ageAtDiagnosis__ageGroup{name,codesystem,code},"
                  + "   ageAtDiagnosis__age__iso8601duration,"
                  + "   familyHistory,"
                  + "   severity{name,codesystem,code},"
                  + "   stage{name,codesystem,code}},"
                  + "measures{"
                  + "   assayCode{name,codesystem,code},"
                  + "   date,"
                  + "   measurementVariable,"
                  + "   measurementValue__value,"
                  + "   measurementValue__units{name,codesystem,code},"
                  + "   observationMoment__age__iso8601duration"
                  + "}}}");

      Map<String, Object> result = executionResult.toSpecification();

      List<Map<String, Object>> individualsListFromJSON =
          (List<Map<String, Object>>)
              ((HashMap<String, Object>) result.get("data")).get("Individuals");

      if (individualsListFromJSON != null) {
        for (Map map : individualsListFromJSON) {
          IndividualsResultSetsItem individualsItem = new IndividualsResultSetsItem();
          individualsItem.setId(TypeUtils.toString(map.get("id")));
          individualsItem.setSex(mapToOntologyTerm((Map) map.get("sex")));
          individualsItem.setAge(
              new AgeAndAgeGroup(
                  mapToOntologyTerm((Map) map.get("age__ageGroup")),
                  TypeUtils.toString(map.get("age__age__iso8601duration"))));
          individualsItem.setDiseaseCausalGenes(
              mapListToOntologyTerms((List<Map>) map.get("diseaseCausalGenes")));
          individualsItem.setEthnicity(mapToOntologyTerm((Map) map.get("ethnicity")));
          individualsItem.setGeographicOrigin(mapToOntologyTerm((Map) map.get("geographicOrigin")));
          individualsItem.setPhenotypicFeatures(
              PhenotypicFeatures.get(map.get("phenotypicFeatures")));
          individualsItem.setDiseases(Diseases.get(map.get("diseases")));
          individualsItem.setMeasures(Measures.get(map.get("measures")));
          individualsItemList.add(individualsItem);
        }
      }

      if (individualsItemList.size() > 0) {
        IndividualsResultSets individualsResultSets =
            new IndividualsResultSets(
                table.getSchema().getName(),
                individualsItemList.size(),
                individualsItemList.toArray(
                    new IndividualsResultSetsItem[individualsItemList.size()]));
        resultSetsList.add(individualsResultSets);
      }
    }
    return resultSetsList;
  }
}
