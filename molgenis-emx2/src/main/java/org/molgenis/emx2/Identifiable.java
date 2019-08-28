package org.molgenis.emx2;

import java.util.UUID;

public interface Identifiable {
  String MOLGENISID = "molgenisid";

  UUID getMolgenisid();
}
