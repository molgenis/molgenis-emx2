package org.molgenis.emx2.rdf.shacl;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShaclSet(String id, String name, String version, String[] sources, String[] files) {
  // YAML stores path based on sets.yaml location while this variable contains any path adjustments
  // relevant for using the classLoader (based on where build.gradle actually places the files).
  public static final String PATH_PREFIX = "_shacl/";

  private static final ClassLoader classLoader = ShaclSet.class.getClassLoader();

  public InputStream getInputStream(int i) {
    return classLoader.getResourceAsStream(PATH_PREFIX + files[i]);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    ShaclSet shaclSet = (ShaclSet) object;
    return Objects.equals(id, shaclSet.id)
        && Objects.equals(name, shaclSet.name)
        && Objects.equals(version, shaclSet.version)
        && Arrays.equals(sources, shaclSet.sources)
        && Arrays.equals(files, shaclSet.files);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(id, name, version);
    result = 31 * result + Arrays.hashCode(sources);
    result = 31 * result + Arrays.hashCode(files);
    return result;
  }

  @Override
  public String toString() {
    return "ShaclSet{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
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
