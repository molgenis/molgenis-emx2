package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
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
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
                + "language,"
                + "relation,"
                + "rights,"
                + "qualifiedRelation,"
                + "publisher,"
                + "issued,"
                + "theme,"
                + "title,"
                + "type,"
                + "modified,"
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

    // todo implement the other properties from https://www.w3.org/TR/vocab-dcat-2/#Class:Dataset

    ArrayList<IRI> spatials =
        extractItemAsIRI((List<Map>) datasetFromJSON.get("spatial"), "ontologyTermURI");
    for (IRI spatial : spatials) {
      builder.add(reqUrl, DCTERMS.SPATIAL, spatial);
    }

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
