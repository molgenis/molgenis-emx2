package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.rdf.RdfParser.parseFile;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.rdf.CustomAssertions;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfParser;
import org.molgenis.emx2.rdf.generators.*;
import org.molgenis.emx2.rdf.shacl.ShaclSet;
import org.molgenis.emx2.rdf.writers.RdfModelWriter;
import org.molgenis.emx2.rdf.writers.RdfOutputStreamWriter;
import org.molgenis.emx2.rdf.writers.RdfStreamWriter;
import org.molgenis.emx2.rdf.writers.ShaclResultWriter;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class PetStoreTest extends RdfTestLoaders {
  private static final String SCHEMA_NAME = PetStoreTest.class.getSimpleName();
  private static final String SCHEMA_NAME_SECOND = SCHEMA_NAME + "_SECOND";

  static Schema petStoreTest;
  static Schema petStoreSecondTest;

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.dropSchemaIfExists(SCHEMA_NAME_SECOND);
    DataModels.Profile.PET_STORE.getImportTask(database, SCHEMA_NAME, "", true).run();
    DataModels.Profile.PET_STORE.getImportTask(database, SCHEMA_NAME_SECOND, "", true).run();
    petStoreTest = database.getSchema(SCHEMA_NAME);
    petStoreSecondTest = database.getSchema(SCHEMA_NAME_SECOND);
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
    database.dropSchemaIfExists(SCHEMA_NAME_SECOND);
  }

  @Test
  void testPetStoreRdfEmx2SchemaModel() throws IOException, NoSuchMethodException {
    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/emx2/schema.ttl",
        RdfModelWriter.class,
        Emx2RdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class),
        petStoreTest);
  }

  @Test
  void testPetStoreRdfEmx2SchemaStream() throws IOException, NoSuchMethodException {
    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/emx2/schema.ttl",
        RdfStreamWriter.class,
        Emx2RdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class),
        petStoreTest);
  }

  @Test
  void testPetStoreRdfSemanticSchema() throws IOException, NoSuchMethodException {
    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/semantic/schema.ttl",
        RdfStreamWriter.class,
        SemanticRdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class),
        petStoreTest);
  }

  @Test
  void testPetStoreRdfSemanticOntology() throws IOException, NoSuchMethodException {
    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/semantic/ontology_tag.ttl",
        RdfStreamWriter.class,
        SemanticRdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class),
        petStoreTest.getTable("Tag"));
  }

  @Test
  void testPetStoreRdfSemanticTable() throws IOException, NoSuchMethodException {
    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/semantic/table_user.ttl",
        RdfStreamWriter.class,
        SemanticRdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class),
        petStoreTest.getTable("User"));
  }

  @Test
  void testPetStoreRdfSemanticRow() throws IOException, NoSuchMethodException {
    Table table = petStoreTest.getTable("Pet");

    compareToValidationFile(
        "rdf_files/rdf_api/pet_store/semantic/row_fire_ant.ttl",
        RdfStreamWriter.class,
        SemanticRdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class, PrimaryKey.class),
        table,
        PrimaryKey.fromEncodedString(table, "name=fire%20ant"));
  }

  @Test
  void testPetStoreShaclSimpleEmx2() throws IOException, NoSuchMethodException {
    // ShaclSet prepends "_shacl" to simulate folder where data would be reachable in production.
    ShaclSet shaclSet =
        new ShaclSet("test", null, null, null, new String[] {"pet_store_simple/shacl.ttl"});

    compareToValidationFile(
        "shacl_files/pet_store/simple/output_emx2.ttl",
        ShaclResultWriter.class,
        Arrays.asList(OutputStream.class, RDFFormat.class, ShaclSet.class),
        Arrays.asList(null, RDFFormat.TURTLE, shaclSet),
        Emx2RdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class),
        petStoreTest);
  }

  @Test
  void testPetStoreShaclSimpleSemantic() throws IOException, NoSuchMethodException {
    // ShaclSet prepends "_shacl" to simulate folder where data would be reachable in production.
    ShaclSet shaclSet =
        new ShaclSet("test", null, null, null, new String[] {"pet_store_simple/shacl.ttl"});

    compareToValidationFile(
        "shacl_files/pet_store/simple/output_semantic.ttl",
        ShaclResultWriter.class,
        Arrays.asList(OutputStream.class, RDFFormat.class, ShaclSet.class),
        Arrays.asList(null, RDFFormat.TURTLE, shaclSet),
        SemanticRdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Schema.class),
        petStoreTest);
  }

  private void compareToValidationFile(
      String validationFilePath,
      Class<? extends RdfOutputStreamWriter> rdfWriterClass,
      Class<? extends RdfGenerator> generatorClass,
      Method method,
      Object... methodArgs)
      throws IOException {
    compareToValidationFile(
        validationFilePath,
        rdfWriterClass,
        Arrays.asList(OutputStream.class, RDFFormat.class),
        Arrays.asList(null, RDFFormat.TURTLE),
        generatorClass,
        method,
        methodArgs);
  }

  private void compareToValidationFile(
      String validationFilePath,
      Class<? extends RdfOutputStreamWriter> rdfWriterClass,
      List<Class> writerArgClasses,
      List<Object> writerArgs,
      Class<? extends RdfGenerator> generatorClass,
      Method method,
      Object... methodArgs)
      throws IOException {
    InMemoryRDFHandler expected = new InMemoryRDFHandler(true);
    parseFile(expected, validationFilePath);

    InMemoryRDFHandler actual = new InMemoryRDFHandler(true);
    RdfParser.parseRdf(
        actual, rdfWriterClass, writerArgClasses, writerArgs, generatorClass, method, methodArgs);

    CustomAssertions.equals(expected, actual);
  }

  @Test
  void testDraftRowsAreExcludedEmx2Generator() throws IOException {
    testDraftRowExcluded(parseSchemaRdf(RdfApiGeneratorFactory.EMX2, petStoreTest));
  }

  @Test
  void testDraftRowsAreExcludedSemanticGenerator() throws IOException {
    testDraftRowExcluded(parseSchemaRdf(RdfApiGeneratorFactory.SEMANTIC, petStoreTest));
  }

  private void testDraftRowExcluded(InMemoryRDFHandler handler) {
    IRI nonDraftRowSubject = Values.iri(getApi(petStoreTest) + "Pet/name=pooky");
    IRI draftRowSubject = Values.iri(getApi(petStoreTest) + "Pet/name=yakul");

    assertAll(
        () -> assertNotNull(handler.resources.get(nonDraftRowSubject)),
        () -> assertNull(handler.resources.get(draftRowSubject)));
  }

  @Test
  void testThatRDFOnlyIncludesRequestedSchema() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);

    assertFalse(handler.resources.keySet().isEmpty());

    for (Resource resource : handler.resources.keySet()) {
      assertFalse(
          resource.toString().contains(SCHEMA_NAME_SECOND),
          "No resources from the second pet store schema should be included.");
    }
  }
}
