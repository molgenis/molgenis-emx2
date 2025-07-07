package org.molgenis.emx2.rdf;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.rdf.generators.RdfGenerator;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public abstract class RdfParser {
  static final ClassLoader classLoader = RdfParser.class.getClassLoader();
  public static final String BASE_URL = "http://localhost:8080";

  // Generic functions to load RDF as if processed through the RDF API

  /**
   * @param handler
   * @param writerClass
   * @param writerArgClasses should contain {@link OutputStream} as first item
   * @param writerArgs first item should be null so that {@link OutputStream} can be placed there
   * @param generatorClass
   * @param method
   * @param methodArgs
   * @throws IOException
   */
  public static void parseRdf(
      RDFHandler handler,
      Class<? extends RdfWriter> writerClass,
      List<Class> writerArgClasses,
      List<Object> writerArgs,
      Class<? extends RdfGenerator> generatorClass,
      Method method,
      Object... methodArgs)
      throws IOException {
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      writerArgs.set(0, outputStream);

      try (RdfWriter writer =
          writerClass
              .getConstructor(writerArgClasses.toArray(new Class[0]))
              .newInstance(writerArgs.toArray())) {
        RdfGenerator generator =
            generatorClass
                .getConstructor(RdfWriter.class, String.class)
                .newInstance(writer, BASE_URL);
        method.invoke(generator, methodArgs);
      } catch (InvocationTargetException
          | IllegalAccessException
          | NoSuchMethodException
          | InstantiationException e) {
        throw new RuntimeException(e);
      }
      parseString(handler, outputStream.toString());
    }
  }

  // Generic functions to load RDF from file/String
  public static void parseFile(RDFHandler handler, String filePath) throws IOException {
    try (FileReader reader =
        new FileReader(Objects.requireNonNull(classLoader.getResource(filePath)).getFile())) {
      parseRdf(handler, reader);
    }
  }

  public static void parseString(RDFHandler handler, String rdf) throws IOException {
    try (Reader reader = new StringReader(rdf)) {
      parseRdf(handler, reader);
    }
  }

  private static void parseRdf(RDFHandler handler, Reader reader) throws IOException {
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(handler);
    parser.parse(reader);
  }
}
