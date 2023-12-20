package org.molgenis.emx2.beans;

import org.molgenis.emx2.Order;
import org.molgenis.emx2.Sort;

public class SortBean implements Sort {

  private String path;
  private Order order;

  public SortBean(Order order, String path) {
    this.path = path;
    this.order = order;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public Order getOrder() {
    return order;
  }

  public String toString() {
    return String.format("%s %s", getPath(), getOrder());
  }
}
