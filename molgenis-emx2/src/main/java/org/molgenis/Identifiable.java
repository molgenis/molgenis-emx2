package org.molgenis;

import java.util.UUID;

public interface Identifiable {
  String MOLGENISID = "molgenisid";

  UUID getMolgenisid();
}
