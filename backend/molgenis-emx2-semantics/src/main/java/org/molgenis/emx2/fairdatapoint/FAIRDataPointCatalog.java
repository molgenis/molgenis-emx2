package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.StringWriter;
import java.util.*;
import org.eclipse.rdf4j.model.*;
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

public class FAIRDataPointCatalog {

  // todo: deal with null values
  // todo: double check cardinality
  // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

  private String result;

  public String getResult() {
    return result;
  }

  /**
   * schemaName -> List of Map{key, value} for each FDP_Catalog record
   *
   * @param schemas
   * @param id
   * @return
   * @throws Exception
   */
  public static Map<String, List<Map<String, Object>>> getFDPCatalogRecords(
      String id, Schema... schemas) {
    Map<String, List<Map<String, Object>>> allCatalogsFromJSON = new HashMap<>();
    for (Schema schema : schemas) {
      List<Map<String, Object>> catalogsFromJSON = getFDPCatalogRecords(schema, id);
      if (catalogsFromJSON != null) {
        allCatalogsFromJSON.put(schema.getName(), catalogsFromJSON);
      }
    }
    return allCatalogsFromJSON;
  }

  public static List<Map<String, Object>> getFDPCatalogRecords(Schema schema, String id) {
    GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult executionResult =
        grapql.execute(
            "{FDP__Catalog"
                + (id != null ? "(filter:{id: {equals:\"" + id + "\"}})" : "")
                + "{"
                + "id,"
                + "title,"
                + "hasVersion,"
                + "description,"
                + "publisher,"
                + "language{ontologyTermURI},"
                + "license,"
                + "themeTaxonomy,"
                + "dataset{id},"
                + "mg_insertedOn,"
                + "mg_updatedOn"
                + "}}");
    Map<String, Object> result = executionResult.toSpecification();
    if (result.containsKey("data")) {
      return (List<Map<String, Object>>)
          ((HashMap<String, Object>) result.get("data")).get("FDP__Catalog");
    } else {
      return null;
    }
  }

  public FAIRDataPointCatalog(Request request, Table fdpCatalogTable) throws Exception {

    String id = request.params("id");
    Schema schema = fdpCatalogTable.getSchema();

    List<Map<String, Object>> catalogsFromJSON = getFDPCatalogRecords(schema, id);
    if (catalogsFromJSON == null) {
      throw new Exception("catalogsFromJSON is null");
    }
    if (catalogsFromJSON.size() != 1) {
      throw new Exception(
          "Expected to find exactly 1 catalog but found " + catalogsFromJSON.size());
    }
    Map catalogFromJSON = catalogsFromJSON.get(0);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("foaf", "http://xmlns.com/foaf/0.1/");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixToNamespace.put("fdp-o", "https://w3id.org/fdp/fdp-o#");
    prefixToNamespace.put("lang", "http://lexvo.org/id/iso639-3/");
    prefixToNamespace.put("obo", "http://purl.obolibrary.org/obo/");
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
                .replace("/catalog/" + fdpCatalogTable.getSchema().getName() + "/" + id, ""));
    IRI profile = iri(root + "/catalog/profile");
    IRI appUrl = iri(root.toString().replace("/api/fdp", ""));
    IRI dataset = iri(root + "/dataset");

    /*
    Required by FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(reqUrl, RDF.TYPE, DCAT.CATALOG);
    builder.add(reqUrl, DCTERMS.TITLE, catalogFromJSON.get("title"));
    BNode publisher = vf.createBNode();
    builder.add(reqUrl, DCTERMS.PUBLISHER, publisher);
    builder.add(publisher, RDF.TYPE, FOAF.AGENT);
    builder.add(publisher, FOAF.NAME, catalogFromJSON.get("publisher"));
    builder.add(root, DCTERMS.LICENSE, iri((String) catalogFromJSON.get("license")));
    ArrayList<IRI> datasetIRIs =
        extractDatasetIRIs(catalogFromJSON.get("dataset"), root, schema.getName());
    for (IRI datasetIRI : datasetIRIs) {
      // not 'Dataset' (class) but 'dataset' (predicate)
      builder.add(reqUrl, iri("http://www.w3.org/ns/dcat#dataset"), datasetIRI);
    }
    builder.add(root, DCTERMS.CONFORMS_TO, profile);
    builder.add(root, DCTERMS.IS_PART_OF, root);
    builder.add(root, DCAT.THEME_TAXONOMY, iri((String) catalogFromJSON.get("themeTaxonomy")));
    builder.add(root, iri("https://w3id.org/fdp/fdp-o#metadataIdentifier"), reqUrl);
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#metadataIssued"),
        literal(((String) catalogFromJSON.get("mg_insertedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#metadataModified"),
        literal(((String) catalogFromJSON.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(dataset, RDF.TYPE, LDP.DIRECT_CONTAINER);
    builder.add(dataset, DCTERMS.TITLE, "Datasets");
    builder.add(dataset, LDP.MEMBERSHIP_RESOURCE, reqUrl);
    builder.add(dataset, LDP.HAS_MEMBER_RELATION, DCAT.DATASET);
    for (IRI datasetIRI : datasetIRIs) {
      builder.add(dataset, LDP.CONTAINS, datasetIRI);
    }

    /*
    Optional in FDP specification (https://specs.fairdatapoint.org/)
    */
    if (catalogFromJSON.get("hasVersion") != null)
      builder.add(reqUrl, DCTERMS.HAS_VERSION, catalogFromJSON.get("hasVersion"));

    if (catalogFromJSON.get("description") != null)
      builder.add(reqUrl, DCTERMS.DESCRIPTION, catalogFromJSON.get("description"));

    if (catalogFromJSON.get("language") != null) {
      ArrayList<IRI> languages =
          extractItemAsIRI((List<Map>) catalogFromJSON.get("language"), "ontologyTermURI");
      for (IRI language : languages) {
        builder.add(reqUrl, DCTERMS.LANGUAGE, language);
      }
    }

    builder.add(
        root,
        DCTERMS.ISSUED,
        literal(((String) catalogFromJSON.get("mg_insertedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(
        root,
        DCTERMS.MODIFIED,
        literal(((String) catalogFromJSON.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
    BNode rights = vf.createBNode();
    builder.add(root, DCTERMS.RIGHTS, rights);
    builder.add(rights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(rights, DCTERMS.DESCRIPTION, "Rights are provided on a per-dataset basis.");
    BNode accessRights = vf.createBNode();
    builder.add(root, DCTERMS.ACCESS_RIGHTS, accessRights);
    builder.add(accessRights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(
        accessRights, DCTERMS.DESCRIPTION, "Access rights are provided on a per-dataset basis.");
    builder.add(
        root, FOAF.HOMEPAGE, iri(appUrl + "/" + schema.getName() + "/tables/#/FDP_Catalog"));

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }

  /**
   * Convert a hyperlink_array into a list of IRIs
   *
   * @param hyperlinks
   * @return
   */
  public List<IRI> convertHyperlinkListToIRIs(ArrayList<String> hyperlinks) {
    ArrayList<IRI> values = new ArrayList<>();
    for (String hyperlink : hyperlinks) {
      IRI iri = iri(hyperlink);
      values.add(iri);
    }
    return values;
  }

  /**
   * e.g. for retrieving all ontologyTermURIs as a list of IRIs from a list of OntologyTerms
   *
   * @param object
   * @param item
   * @return
   */
  public static ArrayList<IRI> extractItemAsIRI(List<Map> object, String item) {
    ArrayList<IRI> values = new ArrayList<>();
    for (Map map : object) {
      IRI iri = iri((String) map.get(item));
      values.add(iri);
    }
    return values;
  }

  /**
   * Specific for making a list of IRIs that point to the underlying Datasets
   *
   * @param object
   * @param root
   * @param schema
   * @return
   */
  public ArrayList<IRI> extractDatasetIRIs(Object object, IRI root, String schema) {
    ArrayList<IRI> values = new ArrayList<>();
    for (Map map : ((List<Map>) object)) {
      String id = (String) map.get("id");
      IRI iri = iri(root + "/dataset/" + schema + "/" + id);
      values.add(iri);
    }
    return values;
  }
}
