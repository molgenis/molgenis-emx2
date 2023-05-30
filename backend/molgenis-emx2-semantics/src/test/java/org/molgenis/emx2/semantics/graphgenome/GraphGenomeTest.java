package org.molgenis.emx2.semantics.graphgenome;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.datamodels.FAIRDataHubLoader.createSchema;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;
import spark.Response;

public class GraphGenomeTest {

  static Database database;
  static List<Table> genomicVariationsTables;
  static final String GRAPH_GENOME_API_LOCATION = "/api/graphgenome";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema graphGenomeSchema = database.dropCreateSchema("graphgenometest");
    createSchema(graphGenomeSchema, "fairdatahub/beaconv2/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("graphgenome", graphGenomeSchema, false);
    genomicVariationsTables = List.of(graphGenomeSchema.getTable("GenomicVariations"));
  }

  @Test
  public void TestForGene() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/graphgenome?gene=TERC&assembly=GRCh37&ucscgenome=hg19");
    when(request.queryParams("gene")).thenReturn("TERC");
    when(request.queryParams("assembly")).thenReturn("GRCh37");
    when(request.queryParams("ucscgenome")).thenReturn("hg19");
    OutputStream outputStream = new ByteArrayOutputStream();
    new GraphGenome()
        .graphGenomeAsRDF(
            outputStream,
            request,
            response,
            GRAPH_GENOME_API_LOCATION,
            genomicVariationsTables,
            true);
    String result = outputStream.toString();
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/graphgenome/TERC> a <http://purl.obolibrary.org/obo/NCIT_C16612>"));
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/graphgenome/TERC/node0/REF/NNNNNNNNN> a <http://purl.obolibrary.org/obo/NCIT_C164388>"));
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/graphgenome/TERC/node1/ALT/T> a <http://ensembl.org/glossary/ENSGLOSSARY_0000187>"));
    assertTrue(
        result.contains(
            "<http://purl.obolibrary.org/obo/RO_0002530> <http://localhost:8080/api/graphgenome/TERC/node0/REF/NNNNNNNNN>"));
    assertTrue(
        result.contains(
            "<http://snomed.info/id/363713009> <http://localhost:8080/api/graphgenome/TERC/node1/ALT/T/clinical_interpretation0>"));
    assertTrue(
        result.contains(
            "dcterms:replaces <http://localhost:8080/api/graphgenome/TERC/node2/REF/C>"));
    assertTrue(
        result.contains(
            "<http://purl.obolibrary.org/obo/HP_0045088> \"Likely benign\", <http://purl.obolibrary.org/obo/NCIT_C168801>"));
  }
}
