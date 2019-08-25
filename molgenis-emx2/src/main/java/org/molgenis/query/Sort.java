package org.molgenis.query;

public interface Sort {

  String[] getPath();

  Order getOrder();
}
