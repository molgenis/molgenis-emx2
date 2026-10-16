package org.molgenis.emx2.beaconv2.filter;

import java.util.List;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconQuery;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestParameters;

public interface FilterParser {

  FilterParser parse();

  List<Filter> getUnsupportedFilters();

  List<String> getWarnings();

  boolean hasWarnings();

  List<String> getGraphQlFilters();

  default String getUrlPathFilter(BeaconQuery beaconQuery) {
    String id = null;
    String entryTypeId = null;
    for (BeaconRequestParameters param : beaconQuery.getRequestParameters()) {
      if ("id".equals(param.getRef())) {
        id = param.getDescription();
      } else if ("entry_type_id".equals(param.getRef())) {
        entryTypeId = param.getDescription();
      }
    }
    if (id != null && entryTypeId != null) {
      EntryType entryType = EntryType.findByName(entryTypeId);
      String graphQlId = entryType.getSingular().toLowerCase();
      return "{ %s: { id: { equals: \"%s\" } } }".formatted(graphQlId, id);
    } else if (id != null) {
      return "{ id: { equals: \"%s\" } }".formatted(id);
    }
    return null;
  }
}
