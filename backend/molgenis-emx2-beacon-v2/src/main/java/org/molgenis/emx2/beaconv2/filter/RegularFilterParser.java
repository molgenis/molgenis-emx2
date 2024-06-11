package org.molgenis.emx2.beaconv2.filter;

import java.util.List;

public class RegularFilterParser implements FilterParser {

  public RegularFilterParser(List<Filter> filters) {
    this.filters = filters;
  }

  private final List<Filter> filters;

  @Override
  public FilterParser parse() {
    for (Filter filter : filters) {}

    return this;
  }

  @Override
  public List<Filter> getUnsupportedFilters() {
    return null;
  }

  @Override
  public List<String> getWarnings() {
    return null;
  }

  @Override
  public boolean hasWarnings() {
    return false;
  }

  @Override
  public List<Filter> getPostFetchFilters() {
    return List.of();
  }

  @Override
  public List<String> getGraphQlFilters() {
    return List.of();
  }
}
