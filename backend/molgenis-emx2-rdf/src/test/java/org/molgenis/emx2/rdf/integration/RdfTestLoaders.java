package org.molgenis.emx2.rdf.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.rdf.RdfParser.parseString;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_SEMANTIC_PREFIXES;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfRootService;
import org.molgenis.emx2.rdf.generators.Emx2RdfGenerator;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.OutputStreamWriterFactory;
import org.molgenis.emx2.rdf.writers.RdfOutputStreamWriter;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RdfTestLoaders {
  static final String BASE_URL = "http://localhost:8080";
  static final String RDF_API_LOCATION = "/api/rdf";

  static final Set<Namespace> DEFAULT_NAMESPACES =
      DefaultNamespace.streamAll().collect(Collectors.toSet());

  static Database database;
  static List<Schema> petStoreSchemas;
  static Schema petStore_nr1;
  static Schema petStore_nr2;
  static Schema compositeKeyTest;
  static Schema ontologyTest;
  static Schema ontologyCrossSchemaTest;
  static Schema tableInherTest;
  static Schema tableInherExtTest;
  static Schema fileTest;
  static Schema refBackTest;
  static Schema refLinkTest;
  static Schema semanticTest;

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @AfterAll
  public static void tearDown() {
    //    database.dropSchemaIfExists(petStore_nr1.getName());
    //    database.dropSchemaIfExists(petStore_nr2.getName());
    //    database.dropSchemaIfExists(compositeKeyTest.getName());
    //    database.dropSchemaIfExists(ontologyCrossSchemaTest.getName());
    //    database.dropSchemaIfExists(ontologyTest.getName());
    //    database.dropSchemaIfExists(tableInherExtTest.getName());
    //    database.dropSchemaIfExists(tableInherTest.getName());
    //    database.dropSchemaIfExists(fileTest.getName());
    //    database.dropSchemaIfExists(refBackTest.getName());
    //    database.dropSchemaIfExists(refLinkTest.getName());
    //    database.dropSchemaIfExists(semanticTest.getName());
  }

  static String getApi(String schemaName, boolean trailingSlash) {
    return BASE_URL + "/" + schemaName + RDF_API_LOCATION + (trailingSlash ? "/" : "");
  }

  static String getApi(Schema schema, boolean trailingSlash) {
    return getApi(schema.getName(), trailingSlash);
  }

  /**
   * Actual API has no trailing slash. Use {@link #getApi(Schema, boolean)} with `false` if needed.
   */
  static String getApi(String schemaName) {
    return getApi(schemaName, true);
  }

  static String getApi(Schema schema) {
    return getApi(schema.getName(), true);
  }

  static IRI getIri(String schemaName, String keyString) {
    return Values.iri(getApi(schemaName) + keyString);
  }

  static Triple getTriple(Resource subject, IRI predicate, Value object) {
    return SimpleValueFactory.getInstance().createTriple(subject, predicate, object);
  }

  /**
   * Helper test method to compare namespaces of 2 schemas.
   *
   * @param schemaTestprefix prefix for created schemas ("1" & "2" is added to this for the 2
   *     different schemes)
   * @param expectedNamespaces set containing the expected combined namespaces
   * @param customPrefixes1 custom_rdf setting field for first schema
   * @param customPrefixes2 custom_rdf setting field for first schema (or null if it should not be
   *     set)
   * @throws IOException
   */
  void validateNamespaces(
      String schemaTestprefix,
      Set<Namespace> expectedNamespaces,
      String customPrefixes1,
      String customPrefixes2)
      throws IOException {
    try {
      Schema schema1 = database.dropCreateSchema(schemaTestprefix + "1");
      Schema schema2 = database.dropCreateSchema(schemaTestprefix + "2");
      schema1.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes1);
      if (customPrefixes2 != null) {
        schema2.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes2);
      }

      InMemoryRDFHandler handler = parseRootRdf(List.of(schema1, schema2));
      assertEquals(expectedNamespaces, handler.namespaces);
    } finally {
      database.dropSchemaIfExists(schemaTestprefix + "1");
      database.dropSchemaIfExists(schemaTestprefix + "2");
    }
  }

  InMemoryRDFHandler parseRootRdf(List<Schema> schemas) throws IOException {
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfRootService rdfService =
          new RdfRootService(BASE_URL, RDFFormat.TURTLE, outputStream)) {
        rdfService.getGenerator().generate(schemas);
      }
      parseString(handler, outputStream.toString());
    }
    return handler;
  }

  InMemoryRDFHandler parseSchemaRdf(Schema schema) throws IOException {
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    parseSchemaRdf(RdfApiGeneratorFactory.EMX2, handler, schema);
    return handler;
  }

  InMemoryRDFHandler parseSchemaRdf(RdfApiGeneratorFactory generatorFactory, Schema schema)
      throws IOException {
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    parseSchemaRdf(generatorFactory, handler, schema);
    return handler;
  }

  void parseSchemaRdf(RdfApiGeneratorFactory generatorFactory, RDFHandler handler, Schema schema)
      throws IOException {
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfOutputStreamWriter writer =
          OutputStreamWriterFactory.STREAM.create(outputStream, RDFFormat.TURTLE)) {
        RdfApiGenerator generator = generatorFactory.create(writer, BASE_URL);
        generator.generate(schema);
      }
      parseString(handler, outputStream.toString());
    }
  }

  InMemoryRDFHandler parseTableRdf(Schema schema, String tableName) throws IOException {
    Table table = schema.getTable(tableName);

    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfOutputStreamWriter writer =
          OutputStreamWriterFactory.STREAM.create(outputStream, RDFFormat.TURTLE)) {
        Emx2RdfGenerator generator = new Emx2RdfGenerator(writer, BASE_URL);
        generator.generate(table);
      }
      parseString(handler, outputStream.toString());
    }
    return handler;
  }

  InMemoryRDFHandler parseRowRdf(Schema schema, String tableName, String rowId) throws IOException {
    Table table = schema.getTable(tableName);
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, rowId);

    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfOutputStreamWriter writer =
          OutputStreamWriterFactory.STREAM.create(outputStream, RDFFormat.TURTLE)) {
        Emx2RdfGenerator generator = new Emx2RdfGenerator(writer, BASE_URL);
        generator.generate(table, primaryKey);
      }
      parseString(handler, outputStream.toString());
    }
    return handler;
  }

  InMemoryRDFHandler parseColumnRdf(Schema schema, String tableName, String columnName)
      throws IOException {
    Table table = schema.getTable(tableName);
    Column column = column(columnName);

    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    try (OutputStream outputStream = new ByteArrayOutputStream()) {
      try (RdfOutputStreamWriter writer =
          OutputStreamWriterFactory.STREAM.create(outputStream, RDFFormat.TURTLE)) {
        Emx2RdfGenerator generator = new Emx2RdfGenerator(writer, BASE_URL);
        generator.generate(table, column);
      }
      parseString(handler, outputStream.toString());
    }
    return handler;
  }
}
