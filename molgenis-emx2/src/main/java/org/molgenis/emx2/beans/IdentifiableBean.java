package org.molgenis.emx2.beans;

import org.molgenis.emx2.Identifiable;

import java.util.UUID;

public class IdentifiableBean implements Identifiable {
  private UUID molgenisid;

  @Override
  public UUID getMolgenisid() {
    return molgenisid;
  }
}
