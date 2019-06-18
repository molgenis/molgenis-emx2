package org.molgenis;

public interface Sort {
  public enum Order {
    ASC,
    DESC
  }

  String[] getPath();

  Order getOrder();
}
