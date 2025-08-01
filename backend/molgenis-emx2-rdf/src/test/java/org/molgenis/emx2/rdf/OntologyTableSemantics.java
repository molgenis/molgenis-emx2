package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.rdf.generators.Emx2RdfGenerator;
import org.molgenis.emx2.rdf.generators.RdfApiGenerator;
import org.molgenis.emx2.rdf.writers.RdfModelWriter;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class OntologyTableSemantics {

  static Database database;
  static Schema petStoreSchema;
  static final String RDF_API_LOCATION = "/api/rdf";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema petStore = database.dropCreateSchema("semanticPetStore");
    PET_STORE.getImportTask(petStore, true).run();
    petStoreSchema = petStore;
  }

  @Test
  void OntologyTableSemanticsTest() throws NoSuchMethodException, IOException {
    Table tagTable = petStoreSchema.getTable("Tag");

    OutputStream outputStream = new ByteArrayOutputStream();
    RDFHandler handler = new TurtleWriter(outputStream);
    RdfParser.parseRdf(
        handler,
        RdfModelWriter.class,
        Arrays.asList(OutputStream.class, RDFFormat.class),
        Arrays.asList(outputStream, RDFFormat.TURTLE),
        Emx2RdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class),
        tagTable);
    outputStream.close();
    String result = outputStream.toString();

    /**
     * Situation before: the 'Tag' ontology table has the default annotation of NCIT:C48697
     * (Controlled Vocabulary) but not the custom annotation added later (reproduceme#Tag)
     */
    assertTrue(
        result.contains("rdfs:subClassOf qb:DataSet, owl:Thing, skos:ConceptScheme;"),
        "Tag should be a subclass of the given classes");
    assertTrue(
        result.contains("SemanticPetStore:Tag a owl:Class;"),
        "Tag should be an instance of owl:Class");
    assertTrue(
        result.contains("rdfs:isDefinedBy obo:NCIT_C48697;"),
        "Tag should be defined by NCIT_C48697");
    assertFalse(
        result.contains("https://w3id.org/reproduceme#Tag>"),
        "Tag should be defined by https://w3id.org/reproduceme#Tag>");

    /** Update the 'Tag' ontology table with new semantics and produce new RDF */
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    ImportProfileTask.class
                        .getClassLoader()
                        .getResourceAsStream("OntologyTableSemanticsTestFile.csv"))));
    petStoreSchema.migrate(metadata);

    outputStream = new ByteArrayOutputStream();
    handler = new TurtleWriter(outputStream);
    RdfParser.parseRdf(
        handler,
        RdfModelWriter.class,
        Arrays.asList(OutputStream.class, RDFFormat.class),
        Arrays.asList(outputStream, RDFFormat.TURTLE),
        Emx2RdfGenerator.class,
        RdfApiGenerator.class.getDeclaredMethod("generate", Table.class),
        tagTable);
    outputStream.close();
    result = outputStream.toString();

    /**
     * Situation after: the 'Tag' ontology table has the Tag annotation from the REPRODUCE-ME
     * ontology, in addition to the 'Controlled Vocabulary' tag (NCIT_C48697)
     */
    assertTrue(
        result.contains("rdfs:subClassOf qb:DataSet, owl:Thing, skos:ConceptScheme;"),
        "Tag should be a subclass of the given classes");
    assertTrue(
        result.contains("SemanticPetStore:Tag a owl:Class;"),
        "Tag should be an instance of owl:Class");
    assertTrue(
        result.contains("rdfs:isDefinedBy obo:NCIT_C48697, <https://w3id.org/reproduceme#Tag>;"));
  }
}
