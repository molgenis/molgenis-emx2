package org.molgenis.emx2.query;

public interface Sort {

  String[] getPath();

  Order getOrder();
}
