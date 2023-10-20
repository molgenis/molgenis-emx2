package org.molgenis.emx2.fairdatapoint;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.fairdatapoint.FormatMimeTypes.formatToMediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;
import spark.Request;

@Tag("slow")
public class FAIRDataPointTest {

  static Database database;
  static Schema[] fairDataHubSchemas;
  static Schema fdpSchema;

  @BeforeAll
  @Disabled
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fdpSchema = database.dropCreateSchema("fdpTest");
    Schema fairDataHub_nr1 = database.dropCreateSchema("fairDataHub_nr1");
    Schema fairDataHub_nr2 = database.dropCreateSchema("fairDataHub_nr2");
    FAIRDataHubLoader fairDataHubLoader = new FAIRDataHubLoader();
    fairDataHubLoader.load(fairDataHub_nr1, true);
    fairDataHubLoader.load(fairDataHub_nr2, true);
    fairDataHubSchemas = new Schema[2];
    fairDataHubSchemas[0] = fairDataHub_nr1;
    fairDataHubSchemas[1] = fairDataHub_nr2;
  }

  @Test
  @Disabled
  public void FDPMetadataSchemaService() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/fdp");
    FAIRDataPoint fairDataPoint = new FAIRDataPoint(request, fairDataHubSchemas);
    fairDataPoint.setVersion("setversionforjtest");
    String result = fairDataPoint.getResult();
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/fdp> a fdp-o:MetadataService, dcat:Resource, dcat:DataService,"));
    assertTrue(
        result.contains(
            "dcterms:title \"FAIR Data Point hosted by MOLGENIS-EMX2 at http://localhost:8080/api/fdp\";"));
    assertTrue(result.contains("dcterms:publisher [ a foaf:Agent;"));
    assertTrue(result.contains("foaf:name \"MOLGENIS-EMX2 FAIR Data Point API\""));
    assertTrue(
        result.contains(
            "ldp:contains <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId01>"));

    assertTrue(
        result.contains(
            "<http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId02>, <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/minCatId03>,"));
    assertTrue(
        result.contains("<http://localhost:8080/api/fdp/catalog/fairDataHub_nr2/catalogId01>,"));

    assertTrue(
        result.contains("<http://localhost:8080/api/fdp/catalog/fairDataHub_nr2/catalogId02>,"));

    assertTrue(
        result.contains("<http://localhost:8080/api/fdp/catalog/fairDataHub_nr2/minCatId03>"));
  }

  @Test
  public void FDPCatalog() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId01");
    when(request.params("id")).thenReturn("catalogId01");
    FAIRDataPointCatalog fairDataPointCatalog =
        new FAIRDataPointCatalog(request, fairDataHubSchemas[0].getTable("Catalog"));
    String result = fairDataPointCatalog.getResult();
    assertTrue(
        result.contains(
            "dcat:dataset <http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01>"));
    assertTrue(
        result.contains("<http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId02>;"));
    assertTrue(result.contains("dcterms:rights [ a dcterms:RightsStatement;"));
    assertTrue(
        result.contains("dcterms:description \"Rights are provided on a per-dataset basis.\""));
  }

  @Test
  public void FDPDataset() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01");
    when(request.params("id")).thenReturn("datasetId01");
    FAIRDataPointDataset fairDataPointDataset =
        new FAIRDataPointDataset(request, fairDataHubSchemas[0].getTable("Dataset"));
    fairDataPointDataset.setIssued("2022-09-19T11:57:06");
    fairDataPointDataset.setModified("2022-09-19T11:57:07");
    String result = fairDataPointDataset.getResult();
    assertTrue(
        result.contains(
            "http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01> a dcat:Dataset"));
    assertTrue(
        result.contains(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/graphql"));
    assertTrue(
        result.contains(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-ntriples"));
    assertTrue(result.contains("dcterms:issued \"2022-09-19T11:57:06\"^^xsd:dateTime"));
    assertTrue(result.contains("https://www.iso.org/obp/ui/#iso:code:3166:FR"));
    assertTrue(result.contains("dcat:spatialResolutionInMeters 1.0E1"));
    assertTrue(result.contains("http://edamontology.org/topic_3325"));
    assertTrue(
        result.contains("http://localhost:8080/api/fdp/distribution> a ldp:DirectContainer"));
    assertTrue(result.contains("ldp:contains"));
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/jsonld>"));
  }

  @Test
  public void FDPDistribution() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl");
    when(request.params("schema")).thenReturn("fairDataHub_nr1");
    when(request.params("distribution")).thenReturn("Analyses");
    when(request.params("format")).thenReturn("ttl");
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(
        result.contains(
            "<http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl> a dcat:Distribution;"));

    assertTrue(
        result.contains(
            "dcterms:title \"Data distribution for http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl\";"));

    assertTrue(
        result.contains(
            "dcterms:description \"MOLGENIS EMX2 data distribution at http://localhost:8080 for table Analyses in schema fairDataHub_nr1, formatted as ttl.\";"));
    assertTrue(
        result.contains(
            "dcat:downloadURL <http://localhost:8080/fairDataHub_nr1/api/ttl/Analyses>;"));
    assertTrue(
        result.contains(
            "dcat:mediaType <https://www.iana.org/assignments/media-types/text/turtle>;"));
    assertTrue(result.contains("dcterms:format \"ttl\";"));
  }

  @Test
  public void FDPDistributionMimeTypes() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl");
    when(request.params("schema")).thenReturn("fairDataHub_nr1");
    when(request.params("distribution")).thenReturn("Analyses");
    testFormatToMediaType(request, "csv");
    testFormatToMediaType(request, "jsonld");
    testFormatToMediaType(request, "rdf-jsonld");
    testFormatToMediaType(request, "graphql");
    testFormatToMediaType(request, "ttl");
    testFormatToMediaType(request, "rdf-ttl");
    testFormatToMediaType(request, "excel");
    testFormatToMediaType(request, "zip");
    testFormatToMediaType(request, "rdf-n3");
    testFormatToMediaType(request, "rdf-ntriples");
    testFormatToMediaType(request, "rdf-nquads");
    testFormatToMediaType(request, "rdf-xml");
    testFormatToMediaType(request, "rdf-trig");
  }

  private static void testFormatToMediaType(Request request, String format) throws Exception {
    when(request.params("format")).thenReturn(format);
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(result.contains(formatToMediaType(format)));
  }

  private void runImportProcedure(TableStoreForXlsxFile store, SchemaMetadata cohortSchema) {
    fdpSchema.migrate(cohortSchema);

    StopWatch.print("creation of tables complete, now starting import data");

    for (String tableName : fdpSchema.getTableNames()) {
      if (store.containsTable(tableName))
        fdpSchema.getTable(tableName).update(store.readTable(tableName, null)); // actually upsert
    }
  }
}
