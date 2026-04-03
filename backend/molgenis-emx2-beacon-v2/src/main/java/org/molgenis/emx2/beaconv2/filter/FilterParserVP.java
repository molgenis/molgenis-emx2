package org.molgenis.emx2.beaconv2.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.common.misc.NCITToGSSOSexMapping;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;

public class FilterParserVP implements FilterParser {

  final BeaconQuery beaconQuery;
  final List<Filter> unsupportedFilters = new ArrayList<>();
  final List<Filter> graphQlFilters = new ArrayList<>();

  public FilterParserVP(BeaconQuery beaconQuery) {
    this.beaconQuery = beaconQuery;
  }

  @Override
  public FilterParser parse() {
    EntryType entryType = beaconQuery.getEntryType();
    List<FilterConceptVP> permittedConcepts = entryType.getPermittedFilters();
    for (Filter filter : beaconQuery.getFilters()) {
      if (!isValidFormat(filter)) {
        throw new MolgenisException("Invalid filter in query: " + filter);
      }

      filter.setFilterType(FilterType.UNDEFINED);
      if (isIdSearch(filter)) {
        createOntologyFiltersFromIds(filter);
      } else {
        String id = filter.getIds()[0];
        try {
          FilterConceptVP searchConcept = FilterConceptVP.findById(id);
          if (!permittedConcepts.contains(searchConcept)
              || !searchConcept.isPermittedValue(filter.getValues()))
            throw new MolgenisException(
                "Invalid filter arguments for entry type: " + entryType.getName());
          processConcept(filter, searchConcept);
        } catch (MolgenisException e) { // VP spec want invalid filters to be ignored
          this.unsupportedFilters.add(filter);
        }
      }
    }
    return this;
  }

  @Override
  public List<Filter> getUnsupportedFilters() {
    return this.unsupportedFilters;
  }

  @Override
  public List<String> getWarnings() {
    return unsupportedFilters.stream().map(filter -> filter.getId().toString()).toList();
  }

  @Override
  public boolean hasWarnings() {
    return !getUnsupportedFilters().isEmpty();
  }

  @Override
  public List<String> getGraphQlFilters() {
    List<String> filters =
        graphQlFilters.stream().map(Filter::getGraphQlFilter).collect(Collectors.toList());

    String urlPathFilter = getUrlPathFilter(beaconQuery);
    if (urlPathFilter != null) filters.add(urlPathFilter);

    return filters;
  }

  void processConcept(Filter filter, FilterConceptVP searchConcept) {
    filter.setConcept(searchConcept);
    switch (searchConcept) {
      case SEX:
        filter.setValues(NCITToGSSOSexMapping.toGSSO(filter.getValues()));
      case CAUSAL_GENE, BIOSAMPLE_TYPE:
        filter.setFilterType(FilterType.ALPHANUMERICAL);
        graphQlFilters.add(filter);
        break;
      case DISEASE, PHENOTYPE:
        processOntologyFilter(filter);
        break;
      case AGE_THIS_YEAR, AGE_OF_ONSET, AGE_AT_DIAG:
        processAgeFilter(filter);
        break;
    }
  }

  void processOntologyFilter(Filter filter) {
    filter.setFilterType(FilterType.ONTOLOGY);
    graphQlFilters.add(filter);
  }

  void processAgeFilter(Filter filter) {
    filter.setFilterType(FilterType.NUMERICAL);
    int age = (Integer) filter.getValue();
    String iso8106Duration = "\"P" + age + "Y\"";
    String iso8106DurationPlusOne = "\"P" + (age + 1) + "Y\"";
    switch (filter.getOperator()) {
      case ">=":
        filter.setValues(new String[] {iso8106Duration, null});
        break;
      case ">":
        filter.setValues(new String[] {iso8106DurationPlusOne, null});
        break;
      case "<":
        filter.setValues(new String[] {null, iso8106Duration});
        break;
      case "<=":
        filter.setValues(new String[] {null, iso8106DurationPlusOne});
        break;
      case "=":
        filter.setValues(new String[] {iso8106Duration, iso8106DurationPlusOne});
        break;
    }
    graphQlFilters.add(filter);
  }

  void createOntologyFiltersFromIds(Filter filter) {
    filter.setConcept(FilterConceptVP.DISEASE);
    filter.setValues(filter.getIds());
    processOntologyFilter(filter);

    Filter phenotTypeFilter = new Filter(filter);
    phenotTypeFilter.setConcept(FilterConceptVP.PHENOTYPE);
    processOntologyFilter(phenotTypeFilter);
  }

  boolean isIdSearch(Filter filter) {
    return filter.getOperator() == null && filter.getValues() == null && filter.getIds() != null;
  }

  boolean isValidFormat(Filter filter) {
    return filter.getIds().length == 1;
  }
}
