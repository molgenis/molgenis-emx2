package org.molgenis.emx2.rdf.writers;

import org.eclipse.rdf4j.model.*;

public interface RdfWriter extends AutoCloseable {
  void processNamespace(Namespace namespace);

  void processTriple(Statement statement);

  void processTriple(Resource subject, IRI predicate, Value object);
}
