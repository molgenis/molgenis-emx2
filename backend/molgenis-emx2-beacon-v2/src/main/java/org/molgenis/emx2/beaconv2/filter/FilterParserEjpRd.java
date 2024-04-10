package org.molgenis.emx2.beaconv2.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.Concept;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.common.misc.NCITToGSSOSexMapping;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.Filter;

public class FilterParserEjpRd implements FilterParser {

  private final BeaconQuery beaconQuery;
  private final List<Filter> unsupportedFilters = new ArrayList<>();
  private final List<Filter> graphQlFilters = new ArrayList<>();
  private final List<Filter> postFetchFilters = new ArrayList<>();

  public FilterParserEjpRd(BeaconQuery beaconQuery) {
    this.beaconQuery = beaconQuery;
  }

  @Override
  public FilterParserEjpRd parse() {
    parseRegularFilters();
    return this;
  }

  @Override
  public List<Filter> getUnsupportedFilters() {
    return this.unsupportedFilters;
  }

  @Override
  public List<String> getWarnings() {
    return unsupportedFilters.stream()
        .map(filter -> filter.getId().toString())
        .collect(Collectors.toList());
  }

  @Override
  public boolean hasWarnings() {
    return !getUnsupportedFilters().isEmpty();
  }

  @Override
  public List<Filter> getPostFetchFilters() {
    return postFetchFilters;
  }

  @Override
  public List<String> getGraphQlFilters() {
    List<String> filters =
        graphQlFilters.stream().map(Filter::getGraphQlFilter).collect(Collectors.toList());

    String urlPathFilter = getUrlPathFilter(beaconQuery);
    if (urlPathFilter != null) filters.add(urlPathFilter);

    return filters;
  }

  private void parseRegularFilters() {
    EntryType entryType = beaconQuery.getEntryType();
    List<Concept> permittedConcepts = entryType.getPermittedSearchConcepts();
    for (Filter filter : beaconQuery.getFilters()) {
      filter.setFilterType(FilterType.UNDEFINED);
      if (isIdSearch(filter)) {
        createOntologyFilters(filter);
      } else if (isValid(filter)) {
        String id = filter.getIds()[0];
        try {
          Concept searchConcept = Concept.findById(id);
          if (!permittedConcepts.contains(searchConcept)
              || !searchConcept.isPermittedValue(filter.getValues()))
            throw new MolgenisException(
                "Invalid filter arguments for entry type: " + entryType.getName());
          filter.setConcept(searchConcept);
          switch (searchConcept) {
            case SEX:
              filter.setValues(NCITToGSSOSexMapping.toGSSO(filter.getValues()));
            case CAUSAL_GENE:
              filter.setFilterType(FilterType.ALPHANUMERICAL);
              graphQlFilters.add(filter);
              break;
            case DISEASE, PHENOTYPE:
              filter.setFilterType(FilterType.ONTOLOGY);
              graphQlFilters.add(filter);
              break;
            case AGE_THIS_YEAR, AGE_OF_ONSET, AGE_AT_DIAG:
              filter.setFilterType(FilterType.NUMERICAL);
              postFetchFilters.add(filter);
              break;
          }
        } catch (MolgenisException e) {
          this.unsupportedFilters.add(filter);
        }
      } else throw new MolgenisException("Invalid filter in query: " + filter);
    }
  }

  private void createOntologyFilters(Filter filter) {
    filter.setFilterType(FilterType.ONTOLOGY);
    filter.setConcept(Concept.DISEASE);
    graphQlFilters.add(filter);
    Filter phenotTypeFilter = new Filter(filter);
    phenotTypeFilter.setConcept(Concept.PHENOTYPE);
    graphQlFilters.add(phenotTypeFilter);
  }

  private boolean isIdSearch(Filter filter) {
    return filter.getOperator() == null && filter.getValues() == null && filter.getIds() != null;
  }

  // todo add more validation
  private boolean isValid(Filter filter) {
    return filter.getIds().length == 1;
  }
}
