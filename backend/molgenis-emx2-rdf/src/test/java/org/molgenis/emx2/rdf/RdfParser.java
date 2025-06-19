package org.molgenis.emx2.rdf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;

public abstract class RdfParser {
  static final ClassLoader classLoader = RdfParser.class.getClassLoader();

  static void parseFile(RDFHandler handler, String filePath) throws IOException {
    try (FileReader reader =
        new FileReader(new File(classLoader.getResource(filePath).getFile()))) {
      parseRdf(handler, reader);
    }
  }

  static void parseString(RDFHandler handler, String rdf) throws IOException {
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
