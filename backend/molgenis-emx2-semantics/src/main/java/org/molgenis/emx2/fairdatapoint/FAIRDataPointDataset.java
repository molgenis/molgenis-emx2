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

  // todo: deal with null values
  // todo: double check cardinality
  // todo: check data types (int, date, hyperlinks etc)

  /*
  todo
  automatic fields: identifier

  FDP_Dataset,,distribution,string [=table name, expanded by Dataset endpoint to a full list of possible dcat:Distributions]


   */

  // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

  private String result;

  public String getResult() {
    return result;
  }

  public FAIRDataPointDataset(Request request, Table fdpDataseTable) throws Exception {

    String id = request.params("id");
    Schema schema = fdpDataseTable.getSchema();

    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult executionResult =
        grapql.execute(
            "{FDP__Dataset"
                + "(filter:{id: {equals:\""
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
    List<Map<String, Object>> datasetsFromJSON =
        (List<Map<String, Object>>)
            ((HashMap<String, Object>) result.get("data")).get("FDP__Dataset");
    if (datasetsFromJSON != null && datasetsFromJSON.size() != 1) {
      throw new Exception("Bad number of dataset results");
    }
    Map datasetFromJSON = datasetsFromJSON.get(0);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("foaf", "http://xmlns.com/foaf/0.1/");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixToNamespace.put("ldp", "http://www.w3.org/ns/ldp#");
    prefixToNamespace.put("fdp-o", "https://w3id.org/fdp/fdp-o#");
    prefixToNamespace.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

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
    System.out.println("request.uri() = " + request.uri()); // e.g. /api/fdp/dataset/rd3/datasetId01
    System.out.println("request.contextPath() = " + request.contextPath());

    // todo this can fail with repeated slashes? deal with/escape whitespaces?
    IRI root =
        iri(
            reqUrl
                .toString()
                .replace("/dataset/" + fdpDataseTable.getSchema().getName() + "/" + id, ""));
    IRI profile = iri(root + "/dataset/profile");
    IRI appUrl = iri(root.toString().replace("/api/fdp", ""));

    builder.add(reqUrl, RDF.TYPE, DCAT.DATASET);

    String distribution = (String) datasetFromJSON.get("distribution");
    for (String format : FORMATS) {
      builder.add(
          reqUrl,
          DCAT.DISTRIBUTION, // todo table name
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
    builder.add(reqUrl, DCAT.KEYWORD, datasetFromJSON.get("keyword"));
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
    builder.add(reqUrl, DCTERMS.ISSUED, datasetFromJSON.get("mg_insertedOn"));
    builder.add(reqUrl, DCAT.THEME, datasetFromJSON.get("theme"));
    builder.add(reqUrl, DCTERMS.TITLE, datasetFromJSON.get("title"));
    builder.add(reqUrl, DCTERMS.TYPE, datasetFromJSON.get("type"));
    builder.add(reqUrl, DCTERMS.MODIFIED, datasetFromJSON.get("mg_updatedOn"));
    builder.add(reqUrl, PROV.QUALIFIED_ATTRIBUTION, datasetFromJSON.get("qualifiedAttribution"));

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
