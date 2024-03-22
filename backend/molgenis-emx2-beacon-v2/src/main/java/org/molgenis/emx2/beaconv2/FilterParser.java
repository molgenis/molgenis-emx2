package org.molgenis.emx2.beaconv2;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.common.misc.NCITToGSSOSexMapping;
import org.molgenis.emx2.beaconv2.endpoints.QueryHelper;
import org.molgenis.emx2.beaconv2.requests.Filter;

public class FilterParser {

  private final List<Filter> unsupportedFilters = new ArrayList<>();
  private List<Filter> graphQlFilters = new ArrayList<>();
  private List<Filter> postFetchFilters = new ArrayList<>();

  public FilterParser() {}

  public FilterParser parse(Filter[] filters) {
    if (filters == null) return this;

    for (Filter filter : filters) {
      filter.setFilterType(FilterType.UNDEFINED);
      if (isOntologyFilter(filter)) {
        filter.setFilterType(FilterType.ONTOLOGY);
        graphQlFilters.add(filter);
      } else if (filter.getIds().length == 1) {
        String id = filter.getIds()[0];
        try {
          Concept concept = Concept.findById(id);
          filter.setConcept(concept);
          switch (concept) {
            case SEX:
              String[] filterTerms = NCITToGSSOSexMapping.toGSSO(filter.getValues());
              filter.setValues(filterTerms);
            case CAUSAL_GENE:
              filter.setFilterType(FilterType.ALPHANUMERICAL);
              graphQlFilters.add(filter);
              break;
            case DISEASE:
            case PHENOTYPE:
              filter.setFilterType(FilterType.ONTOLOGY);
              graphQlFilters.add(filter);
              break;
            case AGE_THIS_YEAR:
            case AGE_OF_ONSET:
            case AGE_AT_DIAG:
              filter.setFilterType(FilterType.NUMERICAL);
              postFetchFilters.add(filter);
              break;
          }
        } catch (MolgenisException e) {
          this.unsupportedFilters.add(filter);
        }
      } else throw new MolgenisException("Invalid filter in query: " + filter);
    }
    return this;
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

  public List<String> getGraphQlFilters() {
    List<String> filters = new ArrayList<>();
    for (Filter filter : graphQlFilters) {
      filters.add(filter.getGraphQlFilter());
    }
    return filters;
  }
}
