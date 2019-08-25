package org.molgenis.query;

import org.molgenis.query.Order;

public interface Sort {

  String[] getPath();

  Order getOrder();
}
