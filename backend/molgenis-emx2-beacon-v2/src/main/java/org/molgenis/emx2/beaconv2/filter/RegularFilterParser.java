package org.molgenis.emx2.beaconv2.filter;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;

public class RegularFilterParser extends FilterParserVP {

  public RegularFilterParser(BeaconQuery beaconQuery) {
    super(beaconQuery);
  }

  @Override
  public FilterParser parse() {
    for (Filter filter : beaconQuery.getFilters()) {
      if (!super.isValidFormat(filter)) {
        throw new MolgenisException("Invalid filter in query: " + filter);
      }

      filter.setFilterType(FilterType.UNDEFINED);
      if (isIdSearch(filter)) {
        super.createOntologyFiltersFromIds(filter);
      } else {
        String id = filter.getIds()[0];
        if (FilterConceptVP.hasId(id)) {
          FilterConceptVP searchConcept = FilterConceptVP.findById(id);
          super.processConcept(filter, searchConcept);
        } else {
          graphQlFilters.add(filter);
        }
      }
    }
    return this;
  }
}
