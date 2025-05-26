package org.molgenis.emx2.rdf.writers;

import java.io.Closeable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public abstract class RdfWriter implements Closeable {
  public abstract void processNamespace(Namespace namespace);

  abstract void processTriple(Statement statement);

  abstract void processTriple(Resource subject, IRI predicate, Value object);
}
