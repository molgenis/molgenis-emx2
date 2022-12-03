package org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.finalizeFilter;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.findColumnPath;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsFields.*;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;
import static org.molgenis.emx2.json.JsonUtil.getWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    BeaconRequestBody beaconRequestBody =
        new ObjectMapper().readValue(request.body(), BeaconRequestBody.class);

    List<AgeQuery> ageQueries = new ArrayList<>();

    List<String> filters = new ArrayList<>();
    for (Filter filter : beaconRequestBody.getQuery().getFilters()) {

      // type is the ontology tag of the column we want to query on
      // either full or partial: "http://purl.obolibrary.org/obo/NCIT_C48697" or "NCIT_C48697"
      String type = filter.getType();

      // id is the specific thing to match individuals on, e.g. a particular disease, age of onset,
      // etc.
      String id = filter.getId();

      // operator (=, >, !, etc) actually not used in this context
      String operator = filter.getOperator();

      /**
       * All of the 'age' related queries. NCIT_C25150 = Age this year, i.e. 'age', EFO_0004847 =
       * Age at disease manifestation, i.e. 'age of onset', NCIT_C156420 = Age at diagnosis
       */
      boolean isAgeQuery =
          type.endsWith("NCIT_C25150") || type.equals("EFO_0004847") || type.equals("NCIT_C156420");
      if (isAgeQuery) {
        int age = Integer.parseInt(id);
        AgeQuery ageQuery = new AgeQuery(type, age, operator);
        ageQueries.add(ageQuery);
      }

      /** Sex (i.e. GenderAtBirth) but requires mapping NCIT to GSSO */
      else if (type.endsWith("NCIT_C28421")) {
        HashMap<String, String> mapping = new HashMap<>();
        // full links? e.g. http://purl.obolibrary.org/obo/NCIT_C16576 ->
        // http://purl.obolibrary.org/obo/GSSO_000123

        // NCIT "Female". A person who belongs to the sex that normally produces ova. The term is
        // used to indicate biological sex distinctions, or cultural gender role distinctions, or
        // both.
        mapping.put("http://purl.obolibrary.org/obo/NCIT_C16576", "GSSO_000123");
        mapping.put("NCIT_C16576", "GSSO_000123");

        // NCIT "Male". A person who belongs to the sex that normally produces sperm. The term is
        // used to indicate biological sex distinctions, cultural gender role distinctions, or both.
        mapping.put("http://purl.obolibrary.org/obo/NCIT_C20197", "GSSO_000124");
        mapping.put("NCIT_C20197", "GSSO_000124");

        // NCIT "Undetermined". A term referring to the lack of definitive criteria for
        // classification of a finding.
        mapping.put("http://purl.obolibrary.org/obo/NCIT_C124294", "GSSO_009509");
        mapping.put("NCIT_C124294", "GSSO_009509");

        // NCIT "Unknown". Not known, observed, recorded; or reported as unknown by the data
        // contributor.
        mapping.put("http://purl.obolibrary.org/obo/NCIT_C17998", "GSSO_009515");
        mapping.put("NCIT_C17998", "GSSO_009515");

        // todo also map Undetermined/Unknown to "assigned no gender at birth" ?

        String filterTerm = id;
        if (mapping.containsKey(id)) {
          filterTerm = mapping.get(id);
        }
        String genderAtBirthFilter = "{sex: {ontologyTermURI: {like: \"" + filterTerm + "\"}}}";
        filters.add(genderAtBirthFilter);
      }

      /**
       * Causative genes, i.e. diseaseCausalGenes. Uses HGNC gene symbol directly ('name') at the
       * moment instead of stable IRI ('ontologyTermURI').
       */
      else if (type.endsWith("NCIT_C16612")) {
        String geneFilter = "{diseaseCausalGenes: {name: {equals: \"" + id + "\"}}}";
        filters.add(geneFilter);
      }

      /**
       * Diagnosis of the rare disease (SIO_001003) NOTE: This could have been a dynamic filter, but
       * that matches individuals via the genomic variation refback, throwing off the results
       */
      else if (type.endsWith("SIO_001003")) {
        String diseaseFilter =
            "{diseases: { diseaseCode: { ontologyTermURI: {like: \"" + id + "\"}}}}";
        filters.add(diseaseFilter);
      }

      /**
       * Phenotype (SIO_010056) NOTE: This could have been a dynamic filter, but that matches
       * individuals via the genomic variation refback, throwing off the results
       */
      else if (type.endsWith("SIO_010056")) {
        String phenotypeFilter =
            "{phenotypicFeatures: { featureType: { ontologyTermURI: {like: \"" + id + "\"}}}}";
        filters.add(phenotypeFilter);
      }

      /** Anything else: create filter dynamically. */
      else {
        ColumnPath columnPath = findColumnPath(new ArrayList<>(), type, this.tables.get(0));
        if (columnPath != null && columnPath.getColumn().isOntology()) {
          String dynamicFilter = columnPath + "ontologyTermURI: {like: \"" + id + "\"";
          filters.add(finalizeFilter(dynamicFilter));
        } else {
          return getWriter().writeValueAsString(new BeaconCountResponse(false, 0));
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
        .writeValueAsString(new BeaconCountResponse(totalCount > 0 ? true : false, totalCount));
  }

  private List<String> removeIndividualIDs(
      List<AgeQuery> ageQueries, List<IndividualsResultSets> resultSetsList) {
    List<String> removeIndividualIDs = new ArrayList<>();
    for (AgeQuery ageQuery : ageQueries) {
      for (IndividualsResultSets resultSet : resultSetsList) {
        for (IndividualsResultSetsItem individual : resultSet.getResults()) {
          List<String> ageStr = new ArrayList<>();
          if (ageQuery.getType().endsWith("NCIT_C25150")) {
            // Age (Age this year)
            if (individual.getAge().getAge().getIso8601duration() != null) {
              ageStr.add(individual.getAge().getAge().getIso8601duration());
            }
          } else if (ageQuery.getType().endsWith("EFO_0004847")) {
            // Age at disease manifestation (i.e. Age of onset)
            if (individual.getDiseases() != null) {
              for (Diseases diseases : individual.getDiseases()) {
                if (diseases.getAgeOfOnset().getAge().getIso8601duration() != null) {
                  ageStr.add(diseases.getAgeOfOnset().getAge().getIso8601duration());
                }
              }
            }
          } else {
            // has to be NCIT_C156420, Age at diagnosis
            if (individual.getDiseases() != null) {
              for (Diseases diseases : individual.getDiseases()) {
                if (diseases.getAgeAtDiagnosis().getAge().getIso8601duration() != null) {
                  ageStr.add(diseases.getAgeAtDiagnosis().getAge().getIso8601duration());
                }
              }
            }
          }
          int[] ageYears = iso8601StringToIntYears(ageStr);
          boolean ageQueryPositiveMatch =
              evalAgeQuery(ageQuery.getValue(), ageYears, ageQuery.getOperator());
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
