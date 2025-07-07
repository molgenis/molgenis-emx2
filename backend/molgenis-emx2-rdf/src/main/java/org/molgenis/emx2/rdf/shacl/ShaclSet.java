package org.molgenis.emx2.rdf.shacl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public record ShaclSet(
    String name, String description, String version, String[] sources, String[] files) {
  private static final ClassLoader classLoader = ShaclSet.class.getClassLoader();

  public InputStream getInputStream(int i) {
    return classLoader.getResourceAsStream(files[i]);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    ShaclSet shaclSet = (ShaclSet) object;
    return Objects.equals(name, shaclSet.name)
        && Objects.equals(description, shaclSet.description)
        && Objects.equals(version, shaclSet.version)
        && Arrays.equals(sources, shaclSet.sources)
        && Arrays.equals(files, shaclSet.files);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(name, description, version);
    result = 31 * result + Arrays.hashCode(sources);
    result = 31 * result + Arrays.hashCode(files);
    return result;
  }

  @Override
  public String toString() {
    return "ShaclSet{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", version='"
        + version
        + '\''
        + ", sources="
        + Arrays.toString(sources)
        + ", files="
        + Arrays.toString(files)
        + '}';
  }
}
