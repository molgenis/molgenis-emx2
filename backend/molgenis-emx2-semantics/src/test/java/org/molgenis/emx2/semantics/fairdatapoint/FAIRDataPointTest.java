package org.molgenis.emx2.semantics.fairdatapoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.fairdatapoint.FAIRDataPoint;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointCatalog;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointDataset;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointDistribution;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class FAIRDataPointTest {

  static Database database;
  static Schema[] fairDataHubSchemas;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema fairDataHub_nr1 = database.dropCreateSchema("fairDataHub_nr1");
    Schema fairDataHub_nr2 = database.dropCreateSchema("fairDataHub_nr2 with a whitespace");
    FAIRDataHubLoader fairDataHubLoader = new FAIRDataHubLoader();
    fairDataHubLoader.load(fairDataHub_nr1, true);
    fairDataHubLoader.load(fairDataHub_nr2, true);
    fairDataHubSchemas = new Schema[2];
    fairDataHubSchemas[0] = fairDataHub_nr1;
    fairDataHubSchemas[1] = fairDataHub_nr2;
  }

  @Test
  public void FDPMetadataSchemaService() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/fdp");
    FAIRDataPoint fairDataPoint = new FAIRDataPoint(request, fairDataHubSchemas);
    fairDataPoint.setVersion("setversionforjtest");
    String result = fairDataPoint.getResult();
    assertTrue(
        result.contains(
            """
            <http://localhost:8080/api/fdp> a fdp-o:MetadataService, dcat:Resource, dcat:DataService,
                fdp-o:FAIRDataPoint;
              dcterms:title "FAIR Data Point hosted by MOLGENIS-EMX2 at http://localhost:8080/api/fdp";
              dcterms:publisher [ a foaf:Agent;
                  foaf:name "MOLGENIS-EMX2 FAIR Data Point API"
                ];"""));
    assertTrue(
        result.contains(
            """
                ldp:contains <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId01>,
                    <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId02>, <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/minCatId03>,
                    <http://localhost:8080/api/fdp/catalog/fairDataHub_nr2%20with%20a%20whitespace/catalogId01>,
                    <http://localhost:8080/api/fdp/catalog/fairDataHub_nr2%20with%20a%20whitespace/catalogId02>,
                    <http://localhost:8080/api/fdp/catalog/fairDataHub_nr2%20with%20a%20whitespace/minCatId03>"""));
    assertEquals(3887, result.length());
  }

  @Test
  public void FDPCatalog() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId01");
    when(request.params("id")).thenReturn("catalogId01");
    FAIRDataPointCatalog fairDataPointCatalog =
        new FAIRDataPointCatalog(request, fairDataHubSchemas[0].getTable("FDP_Catalog"));
    String result = fairDataPointCatalog.getResult();
    assertTrue(
        result.contains(
            "dcat:dataset <http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01>,\n"
                + "    <http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId02>;"));
    assertTrue(
        result.contains(
            """
            dcterms:rights [ a dcterms:RightsStatement;
                  dcterms:description "Rights are provided on a per-dataset basis."
                ];"""));
    assertEquals(2147, result.length());
  }

  @Test
  @Ignore("unstable array order, needs fix or rewrite")
  public void FDPDataset() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01");
    when(request.params("id")).thenReturn("datasetId01");
    FAIRDataPointDataset fairDataPointDataset =
        new FAIRDataPointDataset(request, fairDataHubSchemas[0].getTable("FDP_Dataset"));
    fairDataPointDataset.setIssued("2022-09-19T11:57:06");
    fairDataPointDataset.setModified("2022-09-19T11:57:07");
    String result = fairDataPointDataset.getResult();
    assertEquals(
        """
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix dcterms: <http://purl.org/dc/terms/> .
            @prefix ldp: <http://www.w3.org/ns/ldp#> .
            @prefix dcat: <http://www.w3.org/ns/dcat#> .
            @prefix lang: <http://lexvo.org/id/iso639-3/> .
            @prefix odrl: <http://www.w3.org/ns/odrl/2/> .
            @prefix prov: <http://www.w3.org/ns/prov#> .

            <http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01> a dcat:Dataset;
              dcat:distribution <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/csv>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/excel>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/graphql>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/jsonld>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-jsonld>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-n3>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-nquads>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-ntriples>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-trig>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-ttl>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-xml>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/zip>;
              dcterms:accrualPeriodicity "datasetAccrualPeriodicity01";
              dcterms:spatial <https://www.iso.org/obp/ui/#iso:code:3166:FR>, <https://www.iso.org/obp/ui/#iso:code:3166:ES>;
              dcat:spatialResolutionInMeters 1.0E1;
              dcterms:temporal "datasetTemporal01";
              dcat:temporalResolution "datasetTemporalResolution01";
              prov:wasGeneratedBy "datasetWasGeneratedBy01";
              dcterms:accessRights "datasetAccessRights01";
              dcat:contactPoint "datasetContactPoint01";
              dcterms:creator "datasetCreator01";
              dcterms:description "datasetDescription01";
              odrl:hasPolicy "datasetDescription01";
              dcterms:identifier "datasetId01";
              dcterms:isReferencedBy "datasetIsReferencedBy01";
              dcat:keyword "datasetKeyword01";
              dcat:landingPage "datasetLandingPage01";
              dcterms:license "datasetLicense01";
              dcterms:language lang:eng, lang:nld;
              dcterms:relation "datasetRelation01";
              dcterms:rights "datasetRights01";
              dcat:qualifiedRelation "datasetQualifiedRelation01";
              dcterms:publisher "datasetPublisher01";
              dcterms:issued "2022-09-19T11:57:06"^^xsd:dateTime;
              dcat:theme <http://edamontology.org/topic_3325>, <http://purl.obolibrary.org/obo/NCIT_C16960>;
              dcterms:title "datasetTitle01";
              dcterms:type "datasetType01";
              dcterms:modified "2022-09-19T11:57:07"^^xsd:dateTime;
              prov:qualifiedAttribution "datasetQualifiedAttribution01" .

            <http://localhost:8080/api/fdp/distribution> a ldp:DirectContainer;
              dcterms:title "Distributions";
              ldp:membershipResource <http://localhost:8080/api/fdp/dataset/fairDataHub_nr1/datasetId01>;
              ldp:hasMemberRelation dcat:Distribution;
              ldp:contains <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/csv>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/excel>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/graphql>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/jsonld>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-jsonld>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-n3>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-nquads>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-ntriples>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-trig>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-ttl>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/rdf-xml>, <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl>,
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/zip> .
                """,
        result);
  }

  @Test
  public void FDPDistribution() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl");
    when(request.params("schema")).thenReturn("fairDataHub_nr1");
    when(request.params("table")).thenReturn("Analyses");
    when(request.params("format")).thenReturn("ttl");
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(
        result.contains(
            """
                <http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl> a dcat:Distribution;
                  dcterms:title "Data distribution for http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl";
                  dcterms:description "MOLGENIS EMX2 data distribution at http://localhost:8080 for table Analyses in schema fairDataHub_nr1, formatted as ttl.";
                  dcat:downloadURL <http://localhost:8080/fairDataHub_nr1/api/ttl/Analyses>;
                  dcat:mediaType <https://www.iana.org/assignments/media-types/text/turtle>;
                  dcterms:format "ttl";"""));
    assertEquals(960, result.length());
  }

  @Test
  public void FDPDistributionMimeTypes() throws Exception {
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn("http://localhost:8080/api/fdp/distribution/fairDataHub_nr1/Analyses/ttl");
    when(request.params("schema")).thenReturn("fairDataHub_nr1");
    when(request.params("table")).thenReturn("Analyses");
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
    assertTrue(result.contains(FAIRDataPointDistribution.formatToMediaType(format)));
  }
}
