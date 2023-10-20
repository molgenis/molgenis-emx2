package org.molgenis.emx2;

/**
 * use for to create a converter for names, e.g. when wanting to strip spaces or when converting
 * from label to name
 */
public interface NameMapper {
  public String map(String name);
}
