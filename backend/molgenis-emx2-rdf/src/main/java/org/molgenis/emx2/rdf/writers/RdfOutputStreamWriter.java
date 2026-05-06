package org.molgenis.emx2.rdf.writers;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;

public abstract class RdfOutputStreamWriter implements RdfWriter, AutoCloseable {
  private final OutputStream outputStream;
  private final RDFFormat format;

  public RdfOutputStreamWriter(OutputStream outputStream, RDFFormat format) {
    this.outputStream = requireNonNull(outputStream);
    this.format = requireNonNull(format);
  }

  protected OutputStream getOutputStream() {
    return outputStream;
  }

  protected RDFFormat getFormat() {
    return format;
  }

  @Override
  public abstract void close();
}
