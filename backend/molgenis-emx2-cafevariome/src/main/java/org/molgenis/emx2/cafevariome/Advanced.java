package org.molgenis.emx2.cafevariome;

import java.util.List;
import org.molgenis.emx2.beaconv2.filter.Filter;

public record Advanced(String granularity, List<Filter> requiredFilters) {}
