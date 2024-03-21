package org.molgenis.emx2.beaconv2;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.common.misc.NCITToGSSOSexMapping;
import org.molgenis.emx2.beaconv2.endpoints.QueryHelper;
import org.molgenis.emx2.beaconv2.requests.Filter;

public class FilterParser {

  private final List<Filter> unsupportedFilters = new ArrayList<>();

  public FilterParser() {}

  public List<String> parseFilters(Filter[] filters) {

    List<String> filtersOutput = new ArrayList<>();
    for (Filter filter : filters) {

      if (isOntologyFilter(filter)) {
        FilterType filterType = FilterType.ONTOLOGY;
        filter.setFilterType(filterType);
        filtersOutput.add(filter.getGraphQlFilter());
      } else if (filter.getIds().length == 1) {
        String id = filter.getIds()[0];
        try {
          Concept concept = Concept.findId(id);
          filter.setConcept(concept);
          filter.setFilterType(concept.getFilterType());
          if (concept == Concept.SEX) {
            String[] filterTerms = NCITToGSSOSexMapping.toGSSO(filter.getValues());
            filtersOutput.add(filter.getGraphQlFilter());

            filtersOutput.add(
                valueArrayFilterBuilder("{sex: {ontologyTermURI: {like:", filterTerms));
          }
        } catch (MolgenisException e) {
          this.unsupportedFilters.add(filter);
        }
      } else throw new MolgenisException("Invalid filter in query: " + filter);
    }
    //
    //            boolean isAgeQuery =
    //                id.endsWith(AGE_THIS_YEAR) || id.endsWith(AGE_OF_ONSET) ||
    //       id.endsWith(AGE_AT_DIAG);
    //            if (isAgeQuery) {
    //              ageQueries.add(filter);
    //            } else if (id.endsWith(SEX)) {
    //              Map<String, String> mapping = new NCITToGSSOSexMapping().getMapping();
    //              String[] filterTerms = new String[values.length];
    //              for (int i = 0; i < values.length; i++) {
    //                filterTerms[i] = mapping.containsKey(values[i]) ? mapping.get(values[i]) :
    //       values[i];
    //              }
    //              filtersOutput.add(valueArrayFilterBuilder("{sex: {ontologyTermURI: {like:",
    //       filterTerms));
    //            } else if (id.endsWith(CAUSAL_GENE)) {
    //              filtersOutput.add(valueArrayFilterBuilder("{diseaseCausalGenes: {name:
    // {equals:",
    //       values));
    //            } else if (id.endsWith(DISEASE)) {
    //              filtersOutput.add(
    //                  valueArrayFilterBuilder("{diseases: { diseaseCode: { ontologyTermURI:
    // {like:",
    //       values));
    //            } else if (id.endsWith(PHENOTYPE)) {
    //              filtersOutput.add(
    //                  valueArrayFilterBuilder(
    //                      "{phenotypicFeatures: { featureType: { ontologyTermURI: {like:",
    // values));
    //            }
    //
    //                        /** Anything else: create filter dynamically. */
    //                        else {
    //                            ColumnPath columnPath = findColumnPath(new ArrayList<>(), id,
    //             this.tables.get(0));
    //                            if (columnPath != null && columnPath.getColumn().isOntology()) {
    //                                filters.add(valueArrayFilterBuilder(columnPath +
    // "ontologyTermURI:
    //             {like:", values));
    //                            } else {
    //                                return getWriter()
    //                                        .writeValueAsString(new BeaconCountResponse(host,
    //             beaconRequestBody, false, 0));
    //                            }}}
    //    }

    return filtersOutput;
  }

  public List<Filter> getUnsupportedFilters() {
    return this.unsupportedFilters;
  }

  public List<String> getWarnings() {
    List<String> warnings = new ArrayList<>();
    for (Filter filter : unsupportedFilters) {
      warnings.add(filter.getId().toString());
    }
    return warnings;
  }

  public boolean hasWarnings() {
    return !getUnsupportedFilters().isEmpty();
  }

  private boolean isOntologyFilter(Filter filter) {
    return filter.getOperator() == null && filter.getValues() == null;
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
