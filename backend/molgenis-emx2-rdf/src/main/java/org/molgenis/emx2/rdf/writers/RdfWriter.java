package org.molgenis.emx2.rdf.writers;

import java.io.Closeable;
import java.io.OutputStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;

public abstract class RdfWriter implements Closeable {
  private final String baseUrl;
  private final RDFFormat format;
  private final OutputStream outputStream;

  protected String getBaseUrl() {
    return baseUrl;
  }

  protected RDFFormat getFormat() {
    return format;
  }

  protected OutputStream getOutputStream() {
    return outputStream;
  }

  public RdfWriter(String baseUrl, RDFFormat format, OutputStream outputStream) {
    this.baseUrl = requireNonNull(formatBaseURL(baseUrl));
    this.format = requireNonNull(format);
    this.outputStream = requireNonNull(outputStream);
  }

  public abstract void processNamespace(Namespace namespace);

  public abstract void processTriple(Statement statement);

  public abstract void processTriple(Resource subject, IRI predicate, Value object);
}
