package org.molgenis.beans;

import org.molgenis.Order;
import org.molgenis.Sort;

import java.util.Arrays;

public class SortBean implements Sort {

  private String[] path;
  private Order order;

  public SortBean(Order order, String... path) {
    this.path = path;
    this.order = order;
  }

  @Override
  public String[] getPath() {
    return path;
  }

  @Override
  public Order getOrder() {
    return order;
  }

  public String toString() {
    return String.format("%s %s", Arrays.toString(getPath()), getOrder());
  }
}
