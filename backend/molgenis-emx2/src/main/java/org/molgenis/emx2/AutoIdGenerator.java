package org.molgenis.emx2;

@FunctionalInterface
public interface AutoIdGenerator {

  /** Generate an auto-id value for the given column, applying any computed template if present. */
  String generate(Column column);
}
