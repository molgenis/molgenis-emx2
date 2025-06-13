package org.molgenis.emx2.rdf.writers;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.rdf4j.rio.RDFFormat;

public enum WriterFactory {
  MODEL(RdfModelWriter.class),
  STREAM(RdfStreamWriter.class);

  private final Class<? extends RdfWriter> rdfWriterClass;

  WriterFactory(Class<? extends RdfWriter> rdfWriterClass) {
    this.rdfWriterClass = rdfWriterClass;
  }

  public RdfWriter create(OutputStream out, RDFFormat format) {
    try {
      return rdfWriterClass
          .getConstructor(OutputStream.class, RDFFormat.class)
          .newInstance(out, format);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      // Any exceptions thrown should purely be due to bugs in this specific code.
      throw new RuntimeException("An error occurred while trying to run WriterFactory: " + e);
    }
  }
}
