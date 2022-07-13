package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.fairdatapoint.FAIRDataPointCatalog.extractItemAsIRI;
import static org.molgenis.emx2.fairdatapoint.FAIRDataPointDistribution.FORMATS;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import spark.Request;

public class FAIRDataPointDataset {

  // todo: double check cardinality
  // todo: check data types (int, date, hyperlinks etc)
  // todo odrl:Policy object instead of String? see
  // https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

  private String result;

  public String getResult() {
    return result;
  }

  public FAIRDataPointDataset(Request request, Table fdpDataseTable) throws Exception {

    String id = request.params("id");
    Schema schema = fdpDataseTable.getSchema();
    List<Map<String, Object>> datasetsFromJSON = queryDataset(schema, "id", id);
    if (datasetsFromJSON == null) {
      throw new Exception("datasetsFromJSON is null");
    }
    if (datasetsFromJSON.size() != 1) {
      throw new Exception("Bad number of dataset results");
    }
    Map<String, Object> datasetFromJSON = datasetsFromJSON.get(0);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixToNamespace.put("prov", "http://www.w3.org/ns/prov#");
    prefixToNamespace.put("lang", "http://lexvo.org/id/iso639-3/");
    prefixToNamespace.put("odrl", "http://www.w3.org/ns/odrl/2/");
    prefixToNamespace.put("ldp", "http://www.w3.org/ns/ldp#");

    // Main model builder
    ModelBuilder builder = new ModelBuilder();
    RDFFormat applicationOntologyFormat = RDFFormat.TURTLE;
    ValueFactory vf = SimpleValueFactory.getInstance();
    WriterConfig config = new WriterConfig();
    config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
    for (String prefix : prefixToNamespace.keySet()) {
      builder.setNamespace(prefix, prefixToNamespace.get(prefix));
    }

    IRI reqUrl = iri(request.url());

    // todo this can fail with repeated slashes? deal with/escape whitespaces?
    IRI root =
        iri(
            reqUrl
                .toString()
                .replace("/dataset/" + fdpDataseTable.getSchema().getName() + "/" + id, ""));
    IRI distributionIRI = iri(root + "/distribution");

    builder.add(reqUrl, RDF.TYPE, DCAT.DATASET);

    String distribution = (String) datasetFromJSON.get("distribution");
    if (!schema.getTableNames().contains(distribution)) {
      throw new Exception(
          "Schema does not contain the requested table for distribution. Make sure the value of 'distribution' in your FDP_Dataset matches a table name (from the same schema) you want to publish.");
    }
    for (String format : FORMATS) {
      builder.add(
          reqUrl,
          // not 'Distribution' (class) but 'distribution' (predicate)
          iri("http://www.w3.org/ns/dcat#distribution"),
          iri(root + "/distribution/" + schema.getName() + "/" + distribution + "/" + format));
    }

    builder.add(reqUrl, DCTERMS.ACCRUAL_PERIODICITY, datasetFromJSON.get("accrualPeriodicity"));

    ArrayList<IRI> spatials =
        extractItemAsIRI((List<Map>) datasetFromJSON.get("spatial"), "ontologyTermURI");
    for (IRI spatial : spatials) {
      builder.add(reqUrl, DCTERMS.SPATIAL, spatial);
    }

    builder.add(
        reqUrl,
        DCAT.SPATIAL_RESOLUTION_IN_METERS,
        literal((double) datasetFromJSON.get("spatialResolutionInMeters")));

    builder.add(reqUrl, DCTERMS.TEMPORAL, datasetFromJSON.get("temporal"));
    builder.add(reqUrl, DCAT.TEMPORAL_RESOLUTION, datasetFromJSON.get("temporalResolution"));
    builder.add(reqUrl, PROV.WAS_GENERATED_BY, datasetFromJSON.get("wasGeneratedBy"));
    builder.add(reqUrl, DCTERMS.ACCESS_RIGHTS, datasetFromJSON.get("accessRights"));
    builder.add(reqUrl, DCAT.CONTACT_POINT, datasetFromJSON.get("contactPoint"));
    builder.add(reqUrl, DCTERMS.CREATOR, datasetFromJSON.get("creator"));
    builder.add(reqUrl, DCTERMS.DESCRIPTION, datasetFromJSON.get("description"));
    builder.add(reqUrl, ODRL2.HAS_POLICY, datasetFromJSON.get("description"));
    builder.add(reqUrl, DCTERMS.IDENTIFIER, datasetFromJSON.get("id"));
    builder.add(reqUrl, DCTERMS.IS_REFERENCED_BY, datasetFromJSON.get("isReferencedBy"));
    for (String keyword : (List<String>) datasetFromJSON.get("keyword")) {
      builder.add(reqUrl, DCAT.KEYWORD, keyword);
    }
    builder.add(reqUrl, DCAT.LANDING_PAGE, datasetFromJSON.get("landingPage"));
    builder.add(reqUrl, DCTERMS.LICENSE, datasetFromJSON.get("license"));
    ArrayList<IRI> languages =
        extractItemAsIRI((List<Map>) datasetFromJSON.get("language"), "ontologyTermURI");
    for (IRI language : languages) {
      builder.add(reqUrl, DCTERMS.LANGUAGE, language);
    }
    builder.add(reqUrl, DCTERMS.RELATION, datasetFromJSON.get("relation"));
    builder.add(reqUrl, DCTERMS.RIGHTS, datasetFromJSON.get("rights"));
    builder.add(reqUrl, DCAT.QUALIFIED_RELATION, datasetFromJSON.get("qualifiedRelation"));
    builder.add(reqUrl, DCTERMS.PUBLISHER, datasetFromJSON.get("publisher"));
    builder.add(
        reqUrl,
        DCTERMS.ISSUED,
        literal(((String) datasetFromJSON.get("mg_insertedOn")).substring(0, 19), XSD.DATETIME));
    for (IRI themeIRI : hyperlinkArrayToIRIList((List<String>) datasetFromJSON.get("theme"))) {
      builder.add(reqUrl, DCAT.THEME, themeIRI);
    }
    builder.add(reqUrl, DCTERMS.TITLE, datasetFromJSON.get("title"));
    builder.add(reqUrl, DCTERMS.TYPE, datasetFromJSON.get("type"));
    builder.add(
        reqUrl,
        DCTERMS.MODIFIED,
        literal(((String) datasetFromJSON.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(reqUrl, PROV.QUALIFIED_ATTRIBUTION, datasetFromJSON.get("qualifiedAttribution"));

    builder.add(distributionIRI, RDF.TYPE, LDP.DIRECT_CONTAINER);
    builder.add(distributionIRI, DCTERMS.TITLE, "Distributions");
    builder.add(distributionIRI, LDP.MEMBERSHIP_RESOURCE, reqUrl);
    builder.add(distributionIRI, LDP.HAS_MEMBER_RELATION, DCAT.DISTRIBUTION);
    for (String format : FORMATS) {
      builder.add(
          distributionIRI,
          LDP.CONTAINS,
          iri(root + "/distribution/" + schema.getName() + "/" + distribution + "/" + format));
    }

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }

  public static List<Map<String, Object>> queryDataset(Schema schema, String idField, String id) {
    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult executionResult =
        grapql.execute(
            "{FDP__Dataset"
                + "(filter:{"
                + idField
                + ": {equals:\""
                + id
                + "\"}})"
                + "{"
                + "id,"
                + "distribution,"
                + "accrualPeriodicity,"
                + "spatial{ontologyTermURI},"
                + "spatialResolutionInMeters,"
                + "temporal,"
                + "temporalResolution,"
                + "wasGeneratedBy,"
                + "accessRights,"
                + "contactPoint,"
                + "creator,"
                + "description,"
                + "hasPolicy,"
                + "identifier,"
                + "isReferencedBy,"
                + "keyword,"
                + "landingPage,"
                + "license,"
                + "language{ontologyTermURI},"
                + "relation,"
                + "rights,"
                + "qualifiedRelation,"
                + "publisher,"
                + "theme,"
                + "title,"
                + "type,"
                + "qualifiedAttribution,"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}}");
    Map<String, Object> result = executionResult.toSpecification();
    if (result.get("data") == null
        || ((HashMap<String, Object>) result.get("data")).get("FDP__Dataset") == null) {
      return new ArrayList<>();
    }
    return (List<Map<String, Object>>)
        ((HashMap<String, Object>) result.get("data")).get("FDP__Dataset");
  }

  /**
   * Convert EMX2 hyperlink_array to a list of IRIs
   *
   * @param object
   * @return
   */
  public static ArrayList<IRI> hyperlinkArrayToIRIList(List<String> object) {
    ArrayList<IRI> values = new ArrayList<>();
    for (String hyperlink : object) {
      IRI iri = iri(hyperlink);
      values.add(iri);
    }
    return values;
  }
}
