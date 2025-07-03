package org.molgenis.emx2.rdf.shacl;

import java.io.InputStream;

public record ShaclSet(
    String name, String description, String version, String[] sources, String[] files) {
  private static final ClassLoader classLoader = ShaclSet.class.getClassLoader();

  public InputStream getInputStream(int i) {
    return classLoader.getResourceAsStream(files[i]);
  }
}
