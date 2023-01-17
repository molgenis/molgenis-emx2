package org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.finalizeFilter;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.findColumnPath;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;
import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static org.molgenis.emx2.semantics.RDFService.extractHost;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.getURI;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.ColumnPath;
import org.molgenis.emx2.beaconv2.endpoints.individuals.Diseases;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSetsItem;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.beaconv2.requests.Filter;
import org.molgenis.emx2.beaconv2.responses.BeaconCountResponse;
import spark.Request;
import spark.Response;

/** Implements https://github.com/ejp-rd-vp/vp-api-specs */
public class EJP_VP_IndividualsQuery {

  private Request request;
  private Response response;
  private List<Table> tables;

  public static String SEX = "NCIT_C28421";
  public static String DISEASE = "NCIT_C2991";
  public static String PHENOTYPE = "SIO_010056";
  public static String CAUSAL_GENE = "data_2295";
  public static String AGE_THIS_YEAR = "NCIT_C83164";
  public static String AGE_OF_ONSET = "NCIT_C124353";
  public static String AGE_AT_DIAG = "NCIT_C156420";

  public EJP_VP_IndividualsQuery(Request request, Response response, List<Table> tables) {
    this.request = request;
    this.response = response;
    this.tables = tables;
  }

  public String getPostResponse() throws Exception {

    if (this.tables == null || this.tables.isEmpty()) {
      throw new Exception(
          "No tables reachable for querying, perhaps permissions are not set correctly?");
    }

    // get host name, need as beaconId in response
    String requestURL = request.url();
    URI requestURI = getURI(requestURL);
    String host = extractHost(requestURI);

    BeaconRequestBody beaconRequestBody =
        new ObjectMapper().readValue(request.body(), BeaconRequestBody.class);

    List<Filter> ageQueries = new ArrayList<>();

    List<String> filters = new ArrayList<>();
    for (Filter filter : beaconRequestBody.getQuery().getFilters()) {

      // Id is the ontology tag of the column we want to query on. Can be just the term
      // ("NCIT_C48697") or prefixed ("obo:NCIT_C48697") or full URL
      // ("http://purl.obolibrary.org/obo/NCIT_C48697"), it does not matter. We strip off anything
      // before the first ':' to make it work.
      String id = filter.getId();
      id = id.indexOf(":") == -1 ? id : id.substring(id.indexOf(":") + 1);

      // operator (=, >, !, etc)
      String operator = filter.getOperator();

      // value is the specific thing to match individuals on, e.g. a particular disease, age of
      // onset, for instance "ordo:ORPHA_79314", "LAMP2",
      // "http://purl.obolibrary.org/obo/NCIT_C16576".
      String value = filter.getValue();
      value = value.indexOf(":") == -1 ? value : value.substring(value.indexOf(":") + 1);

      /**
       * All of the 'age' related queries. AGE_THIS_YEAR = Age this year ("Birth Year" for
       * interoperability reasons), i.e. 'age', AGE_OF_ONSET = Age at disease manifestation, i.e.
       * 'age of onset', AGE_AT_DIAG = Age at diagnosis
       */
      boolean isAgeQuery =
          id.endsWith(AGE_THIS_YEAR) || id.endsWith(AGE_OF_ONSET) || id.endsWith(AGE_AT_DIAG);
      if (isAgeQuery) {
        Filter ageQuery = new Filter(id, operator, value);
        ageQueries.add(ageQuery);
      }

      /** Sex (i.e. GenderAtBirth) but requires a mapping NCIT to GSSO */
      else if (id.endsWith(SEX)) {
        HashMap<String, String> mapping = new NCITToGSSOSexMapping().getMapping();
        String filterTerm = value;
        if (mapping.containsKey(value)) {
          filterTerm = mapping.get(value);
        }
        String genderAtBirthFilter = "{sex: {ontologyTermURI: {like: \"" + filterTerm + "\"}}}";
        filters.add(genderAtBirthFilter);
      }

      /**
       * Causative genes (CAUSAL_GENE), i.e. diseaseCausalGenes. Uses HGNC gene symbol directly
       * ('name') at the moment instead of stable IRI ('ontologyTermURI').
       */
      else if (id.endsWith(CAUSAL_GENE)) {
        String geneFilter = "{diseaseCausalGenes: {name: {equals: \"" + value + "\"}}}";
        filters.add(geneFilter);
      }

      /**
       * Diagnosis of the rare disease (DIAGNOSIS) NOTE: This could have been a dynamic filter, but
       * that matches individuals via the genomic variation refback, throwing off the results
       */
      else if (id.endsWith(DISEASE)) {
        String diseaseFilter =
            "{diseases: { diseaseCode: { ontologyTermURI: {like: \"" + value + "\"}}}}";
        filters.add(diseaseFilter);
      }

      /**
       * Phenotype (PHENOTYPE) NOTE: This could have been a dynamic filter, but that matches
       * individuals via the genomic variation refback, throwing off the results
       */
      else if (id.endsWith(PHENOTYPE)) {
        String phenotypeFilter =
            "{phenotypicFeatures: { featureType: { ontologyTermURI: {like: \"" + value + "\"}}}}";
        filters.add(phenotypeFilter);
      }

      /** Anything else: create filter dynamically. */
      else {
        ColumnPath columnPath = findColumnPath(new ArrayList<>(), id, this.tables.get(0));
        if (columnPath != null && columnPath.getColumn().isOntology()) {
          String dynamicFilter = columnPath + "ontologyTermURI: {like: \"" + value + "\"";
          filters.add(finalizeFilter(dynamicFilter));
        } else {
          return getWriter()
              .writeValueAsString(new BeaconCountResponse(host, beaconRequestBody, false, 0));
        }
      }
    }

    // execute query with all filters combined
    List<IndividualsResultSets> resultSetsList =
        queryIndividuals(tables, filters.toArray(new String[0]));

    // only works because we only do AND queries, so one unmatched filter means no hit
    List<String> removeIndividualIDs = removeIndividualIDs(ageQueries, resultSetsList);

    // use list of 'removed' individuals to rebuild the result set
    List<IndividualsResultSets> filteredResultSetsList = new ArrayList<>();
    for (IndividualsResultSets resultSet : resultSetsList) {
      List<IndividualsResultSetsItem> filteredIndividuals = new ArrayList<>();
      for (IndividualsResultSetsItem individual : resultSet.getResults()) {
        String currentId = resultSet.getId() + "@" + individual.getId();
        if (!removeIndividualIDs.contains(currentId)) {
          filteredIndividuals.add(individual);
        }
      }
      if (filteredIndividuals.size() > 0) {
        IndividualsResultSetsItem[] filteredIndividualsArr =
            filteredIndividuals.toArray(new IndividualsResultSetsItem[0]);
        IndividualsResultSets filteredResultSet =
            new IndividualsResultSets(resultSet.getId(), filteredIndividualsArr);
        filteredResultSetsList.add(filteredResultSet);
      }
    }

    // each table results in one IndividualsResultSets, add up the result counts
    int totalCount = 0;
    for (IndividualsResultSets individualsResultSets : filteredResultSetsList) {
      totalCount += individualsResultSets.getResultsCount();
    }

    // return the individual counts
    return getWriter()
        .writeValueAsString(
            new BeaconCountResponse(
                host, beaconRequestBody, totalCount > 0 ? true : false, totalCount));
  }

  private List<String> removeIndividualIDs(
      List<Filter> ageQueries, List<IndividualsResultSets> resultSetsList) throws Exception {
    List<String> removeIndividualIDs = new ArrayList<>();
    for (Filter ageQuery : ageQueries) {
      for (IndividualsResultSets resultSet : resultSetsList) {
        for (IndividualsResultSetsItem individual : resultSet.getResults()) {
          List<String> ageStr = new ArrayList<>();
          if (ageQuery.getId().endsWith(AGE_THIS_YEAR)) {
            if (individual.getAge().getAge().getIso8601duration() != null) {
              ageStr.add(individual.getAge().getAge().getIso8601duration());
            }
          } else if (ageQuery.getId().endsWith(AGE_OF_ONSET)) {
            if (individual.getDiseases() != null) {
              for (Diseases diseases : individual.getDiseases()) {
                if (diseases.getAgeOfOnset().getAge().getIso8601duration() != null) {
                  ageStr.add(diseases.getAgeOfOnset().getAge().getIso8601duration());
                }
              }
            }
          } else if (ageQuery.getId().endsWith(AGE_AT_DIAG)) {
            if (individual.getDiseases() != null) {
              for (Diseases diseases : individual.getDiseases()) {
                if (diseases.getAgeAtDiagnosis().getAge().getIso8601duration() != null) {
                  ageStr.add(diseases.getAgeAtDiagnosis().getAge().getIso8601duration());
                }
              }
            }
          } else {
            throw new Exception("Bad age query: " + ageQuery);
          }
          int[] ageYears = iso8601StringToIntYears(ageStr);
          int age = Integer.parseInt(ageQuery.getValue());
          boolean ageQueryPositiveMatch = evalAgeQuery(age, ageYears, ageQuery.getOperator());
          if (!ageQueryPositiveMatch) {
            removeIndividualIDs.add(resultSet.getId() + "@" + individual.getId());
          }
        }
      }
    }
    return removeIndividualIDs;
  }

  /**
   * Helper function to apply age queries
   *
   * @param queryValue
   * @param databaseValues
   * @param operator
   * @return
   */
  private boolean evalAgeQuery(int queryValue, int[] databaseValues, String operator) {
    if (operator.equals("=")) {
      for (int i = 0; i < databaseValues.length; i++) {
        if (queryValue == databaseValues[i]
            || queryValue == (databaseValues[i] - 1)
            || queryValue == (databaseValues[i] + 1)) {
          return true;
        }
      }
      return false;
    } else if (operator.contains(">")) {
      for (int i = 0; i < databaseValues.length; i++) {
        if (databaseValues[i] > queryValue) {
          return true;
        }
        if (operator.equals(">=") && databaseValues[i] == queryValue) {
          return true;
        }
      }
      return false;
    } else if (operator.contains("<")) {
      for (int i = 0; i < databaseValues.length; i++) {
        if (databaseValues[i] < queryValue) {
          return true;
        }
        if (operator.equals("<=") && databaseValues[i] == queryValue) {
          return true;
        }
      }
      return false;
    }
    return false;
  }

  /**
   * Helper function to convert ISO8601 duration to number of years as integer
   *
   * @param ageStr
   * @return
   */
  private int[] iso8601StringToIntYears(List<String> ageStr) {
    int[] result = new int[ageStr.size()];
    for (int i = 0; i < ageStr.size(); i++) {
      Period period = Period.parse(ageStr.get(i));
      result[i] = period.getYears();
    }
    return result;
  }
}
