package org.molgenis.emx2.io;

import static org.molgenis.emx2.io.emx2.Emx2.sanitize;

import org.molgenis.emx2.NameMapper;

public class RemoveSpaceNameMapper implements NameMapper {
  @Override
  public String map(String name) {
    return sanitize(name);
  }
}
