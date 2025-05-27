package org.molgenis.emx2.rdf.writers;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;

public abstract class RdfWriter implements AutoCloseable {
  private final OutputStream outputStream;
  private final RDFFormat format;

  protected OutputStream getOutputStream() {
    return outputStream;
  }

  protected RDFFormat getFormat() {
    return format;
  }

  public RdfWriter(OutputStream outputStream, RDFFormat format) {
    this.outputStream = requireNonNull(outputStream);
    this.format = requireNonNull(format);
  }

  public abstract void processNamespace(Namespace namespace);

  public abstract void processTriple(Statement statement);

  public abstract void processTriple(Resource subject, IRI predicate, Value object);
}
