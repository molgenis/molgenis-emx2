package org.molgenis.emx2.rdf.writers;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

public class RdfModelWriter extends RdfWriter {
  private final ModelBuilder builder = new ModelBuilder();

  public RdfModelWriter(String baseUrl, RDFFormat format, OutputStream outputStream) {
    super(baseUrl, format, outputStream);
  }

  @Override
  public void processNamespace(Namespace namespace) {
    builder.setNamespace(namespace);
  }

  @Override
  public void processTriple(Statement statement) {
    builder.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
  }

  @Override
  public void processTriple(Resource subject, IRI predicate, Value object) {
    builder.add(subject, predicate, object);
  }

  @Override
  public void close() throws IOException {
    Rio.write(builder.build(), getOutputStream(), getFormat());
  }
}
