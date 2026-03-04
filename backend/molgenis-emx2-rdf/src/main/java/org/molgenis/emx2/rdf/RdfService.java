package org.molgenis.emx2.rdf;

public interface RdfService<T> extends AutoCloseable {
  T getGenerator();

  void close();
}
