package org.molgenis.beans;

import org.molgenis.Identifiable;

import java.util.UUID;

public class IdentifiableBean implements Identifiable {
  private UUID molgenisid;

  @Override
  public UUID getMolgenisid() {
    return molgenisid;
  }
}
