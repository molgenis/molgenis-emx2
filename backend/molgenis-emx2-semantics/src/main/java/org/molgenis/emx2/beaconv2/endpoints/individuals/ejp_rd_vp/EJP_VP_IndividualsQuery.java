package org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.finalizeFilter;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.findColumnPath;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsFields.*;
import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;
import static org.molgenis.emx2.json.JsonUtil.getWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.ColumnPath;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
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

        if (type.endsWith("NCIT_C25150")) {
          // Age (Age this year)
          String ageFilter =
              "{ _or: [{"
                  + AGE_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + age
                  + "Y\"}}, {"
                  + AGE_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age - 1)
                  + "Y\"}}, {"
                  + AGE_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age + 1)
                  + "Y\"}}]  }";
          filters.add(ageFilter);
        } else if (type.endsWith("EFO_0004847")) {
          // Age at disease manifestation
          String ageFilter =
              "{ _or: [{diseases: {"
                  + AGEOFONSET_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + age
                  + "Y\"}}}, {diseases: {"
                  + AGEOFONSET_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age - 1)
                  + "Y\"}}}, {diseases: {"
                  + AGEOFONSET_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age + 1)
                  + "Y\"}}}]  }";
          filters.add(ageFilter);
        } else {
          // has to be NCIT_C156420, Age at diagnosis
          String ageFilter =
              "{ _or: [{diseases: {"
                  + AGEATDIAGNOSIS_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + age
                  + "Y\"}}}, {diseases: {"
                  + AGEATDIAGNOSIS_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age - 1)
                  + "Y\"}}}, {diseases: {"
                  + AGEATDIAGNOSIS_AGE_ISO8601DURATION
                  + ": {like: \"P"
                  + (age + 1)
                  + "Y\"}}}]  }";
          filters.add(ageFilter);
        }
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
       * Diagnosis of the rare disease (SIO_001003), Phenotype (SIO_010056), and any others: create
       * filter dynamically. Right now only columns of type Ontology are supported.
       */
      else {
        ColumnPath columnPath = findColumnPath("", type, this.tables.get(0));
        if (columnPath != null && columnPath.getColumn().isOntology()) {
          String dynamicFilter = columnPath.getPath() + "ontologyTermURI: {like: \"" + id + "\"";
          filters.add(finalizeFilter(dynamicFilter));
        } else {
          return getWriter().writeValueAsString(new BeaconCountResponse(false, 0));
        }
      }
    }

    // execute query with all filters combined
    List<IndividualsResultSets> resultSetsList =
        queryIndividuals(tables, filters.toArray(new String[0]));

    // each table results in one IndividualsResultSets, add up the result counts
    int totalCount = 0;
    for (IndividualsResultSets individualsResultSets : resultSetsList) {
      totalCount += individualsResultSets.getResultsCount();
    }

    // return the individual counts
    return getWriter()
        .writeValueAsString(new BeaconCountResponse(totalCount > 0 ? true : false, totalCount));
  }
}
