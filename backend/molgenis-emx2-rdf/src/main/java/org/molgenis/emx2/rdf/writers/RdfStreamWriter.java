package org.molgenis.emx2.rdf.writers;

import java.io.OutputStream;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

public class RdfStreamWriter extends RdfWriter {
  private static final WriterConfig config = new WriterConfig();
  private static final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

  private final RDFWriter writer;

  static {
    config.set(BasicWriterSettings.PRETTY_PRINT, false);
  }

  public RdfStreamWriter(OutputStream outputStream, RDFFormat format) {
    super(outputStream, format);
    writer = Rio.createWriter(getFormat(), getOutputStream());
    writer.startRDF();
    // Failsafe as code is still untested (will enable in separate PR that includes tests)
    throw new NotImplementedException();
  }

  @Override
  public void processNamespace(Namespace namespace) {
    writer.handleNamespace(namespace.getPrefix(), namespace.getName());
  }

  @Override
  public void processTriple(Statement statement) {
    writer.handleStatement(statement);
  }

  @Override
  public void processTriple(Resource subject, IRI predicate, Value object) {
    processTriple(valueFactory.createStatement(subject, predicate, object));
  }

  @Override
  public void close() {
    writer.endRDF();
  }
}
