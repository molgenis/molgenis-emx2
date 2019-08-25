package org.molgenis.beans;

import org.molgenis.data.Identifiable;

import java.util.UUID;

public class IdentifiableBean implements Identifiable {
  private UUID molgenisid;

  @Override
  public UUID getMolgenisid() {
    return molgenisid;
  }
}
