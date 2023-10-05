package org.molgenis.emx2.io;

import org.molgenis.emx2.NameMapper;

public class DefaultNameMapper implements NameMapper {
  @Override
  public String map(String name) {
    return name;
  }
}
