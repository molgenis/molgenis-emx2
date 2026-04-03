package org.molgenis.emx2.utils.generator;

import java.util.UUID;

public class IdGeneratorImpl implements IdGenerator {
  @Override
  public String generateId() {
    return UUID.randomUUID().toString();
  }
}
