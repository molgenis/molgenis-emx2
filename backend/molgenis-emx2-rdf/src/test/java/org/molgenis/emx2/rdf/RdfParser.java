package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.Column.column;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class RdfParser {
  static final String BASE_URL = "http://localhost:8080";
  static final ClassLoader classLoader = RdfParser.class.getClassLoader();

  static void parseRdfRoot(RDFHandler handler, List<Schema> schemas) throws IOException {
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfRootService rdfService =
          new RdfRootService(BASE_URL, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(schemas);
      }
      parseString(handler, outputStream.toString());
    }
  }

  static void parseRdfSchema(RDFHandler handler, Schema schema) throws IOException {
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfSchemaService rdfService =
          new RdfSchemaService(BASE_URL, schema, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(schema);
      }
      parseString(handler, outputStream.toString());
    }
  }

  static void parseTableRdf(RDFHandler handler, Schema schema, String tableName)
      throws IOException {
    Table table = schema.getTable(tableName);

    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfSchemaService rdfService =
          new RdfSchemaService(BASE_URL, schema, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(table);
      }
      parseString(handler, outputStream.toString());
    }
  }

  static void parseRowRdf(RDFHandler handler, Schema schema, String tableName, String rowId)
      throws IOException {
    Table table = schema.getTable(tableName);
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, rowId);

    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfSchemaService rdfService =
          new RdfSchemaService(BASE_URL, schema, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(table, primaryKey);
      }
      parseString(handler, outputStream.toString());
    }
  }

  static void parseColumnRdf(RDFHandler handler, Schema schema, String tableName, String columnName)
      throws IOException {
    Table table = schema.getTable(tableName);
    Column column = column(columnName);

    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfSchemaService rdfService =
          new RdfSchemaService(BASE_URL, schema, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(table, column);
      }
      parseString(handler, outputStream.toString());
    }
  }

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
