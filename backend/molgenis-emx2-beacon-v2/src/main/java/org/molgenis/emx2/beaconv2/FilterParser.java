package org.molgenis.emx2.beaconv2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.endpoints.QueryHelper;
import org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp.NCITToGSSOSexMapping;
import org.molgenis.emx2.beaconv2.requests.Filter;

public class FilterParser {

  public static final String SEX = "NCIT_C28421";
  public static final String DISEASE = "NCIT_C2991";
  public static final String PHENOTYPE = "SIO_010056";
  public static final String CAUSAL_GENE = "data_2295";
  public static final String AGE_THIS_YEAR = "NCIT_C83164";
  public static final String AGE_OF_ONSET = "NCIT_C124353";
  public static final String AGE_AT_DIAG = "NCIT_C156420";

  public FilterParser() {}

  public List<String> parseFilters(Filter[] filters) {
    List<Filter> ageQueries = new ArrayList<>();

    List<String> filtersOutput = new ArrayList<>();
    for (Filter filter : filters) {

      String[] ids = filter.getIds();
      for (int i = 0; i < ids.length; i++) {
        String id = ids[i];
        ids[i] = !id.contains(":") ? id : id.substring(id.indexOf(":") + 1);
      }

      String operator = filter.getOperator();
      String[] values = filter.getValues();

      if (operator == null && values == null) {
        String[] queries =
            new String[] {
              "{diseases: { diseaseCode: { ontologyTermURI: {like:",
              "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:"
            };
        filtersOutput.add(valueArrayFilterBuilder(queries, ids));
        continue;
      }

      // if not ontology filter, assume 1 ID to be present (regular query)
      String id = ids[0];

      // strip away prefixes for values as well
      for (int i = 0; i < values.length; i++) {
        String value = values[i];
        values[i] = !value.contains(":") ? value : value.substring(value.indexOf(":") + 1);
      }

      boolean isAgeQuery =
          id.endsWith(AGE_THIS_YEAR) || id.endsWith(AGE_OF_ONSET) || id.endsWith(AGE_AT_DIAG);
      if (isAgeQuery) {
        ageQueries.add(filter);
      } else if (id.endsWith(SEX)) {
        Map<String, String> mapping = new NCITToGSSOSexMapping().getMapping();
        String[] filterTerms = new String[values.length];
        for (int i = 0; i < values.length; i++) {
          filterTerms[i] = mapping.containsKey(values[i]) ? mapping.get(values[i]) : values[i];
        }
        filtersOutput.add(valueArrayFilterBuilder("{sex: {ontologyTermURI: {like:", filterTerms));
      } else if (id.endsWith(CAUSAL_GENE)) {
        filtersOutput.add(valueArrayFilterBuilder("{diseaseCausalGenes: {name: {equals:", values));
      } else if (id.endsWith(DISEASE)) {
        filtersOutput.add(
            valueArrayFilterBuilder("{diseases: { diseaseCode: { ontologyTermURI: {like:", values));
      } else if (id.endsWith(PHENOTYPE)) {
        filtersOutput.add(
            valueArrayFilterBuilder(
                "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:", values));
      }

      //            /** Anything else: create filter dynamically. */
      //            else {
      //                ColumnPath columnPath = findColumnPath(new ArrayList<>(), id,
      // this.tables.get(0));
      //                if (columnPath != null && columnPath.getColumn().isOntology()) {
      //                    filters.add(valueArrayFilterBuilder(columnPath + "ontologyTermURI:
      // {like:", values));
      //                } else {
      //                    return getWriter()
      //                            .writeValueAsString(new BeaconCountResponse(host,
      // beaconRequestBody, false, 0));
      //                }
      //            }
    }
    return filtersOutput;
  }

  /**
   * Help build OR queries based on value arrays
   *
   * @param queries
   * @param values
   * @return
   */
  private String valueArrayFilterBuilder(String[] queries, String[] values) {
    StringBuilder filter = new StringBuilder();
    filter.append("{ _or: [");
    for (String query : queries) {
      for (String value : values) {
        filter.append(QueryHelper.finalizeFilter(query + " \"" + value + "\"") + ",");
      }
    }
    filter.deleteCharAt(filter.length() - 1);
    filter.append("] }");
    return filter.toString();
  }

  private String valueArrayFilterBuilder(String query, String[] values) {
    return valueArrayFilterBuilder(new String[] {query}, values);
  }
}
