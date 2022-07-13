package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
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
import spark.Request;

public class FAIRDataPoint {

  // todo: double check cardinality

  private String result;

  public String getResult() {
    return result;
  }

  public FAIRDataPoint(Request request, Schema... schemas) throws Exception {
    // get all FDP_Catalog records from all of the supplied tables
    if (schemas.length == 0) {
      throw new Exception("No data available");
    }

    Map<String, List<Map<String, Object>>> allCatalogFromJSON =
        FAIRDataPointCatalog.getFDPCatalogRecords(null, schemas);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("foaf", "http://xmlns.com/foaf/0.1/");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixToNamespace.put("ldp", "http://www.w3.org/ns/ldp#");
    prefixToNamespace.put("fdp-o", "https://w3id.org/fdp/fdp-o#");
    prefixToNamespace.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    prefixToNamespace.put("vc", "http://www.w3.org/2006/vcard/ns#");
    prefixToNamespace.put("r3d", "http://www.re3data.org/schema/3-0#");
    prefixToNamespace.put("lang", "http://lexvo.org/id/iso639-3/");

    // Main model builder
    ModelBuilder builder = new ModelBuilder();
    RDFFormat applicationOntologyFormat = RDFFormat.TURTLE;
    ValueFactory vf = SimpleValueFactory.getInstance();
    WriterConfig config = new WriterConfig();
    config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
    for (String prefix : prefixToNamespace.keySet()) {
      builder.setNamespace(prefix, prefixToNamespace.get(prefix));
    }

    // todo database version automatic using something like ??
    // GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    // queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(database));

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date currentDateTime = new Date(System.currentTimeMillis());
    IRI root = iri(request.url());
    IRI openAPI = iri(request.url().substring(0, request.url().length() - 3) + "openapi");
    IRI catalog = iri(root + "/catalog");
    IRI profile = iri(root + "/profile");
    IRI identifier = iri(root + "#identifier");
    String version = "MOLGENIS-EMX2 v8.0";

    /*
    Required by FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(root, RDF.TYPE, iri("https://w3id.org/fdp/fdp-o#MetadataService"));
    builder.add(root, DCTERMS.TITLE, "FAIR Data Point hosted by MOLGENIS-EMX2 at " + root);
    BNode publisher = vf.createBNode();
    builder.add(root, DCTERMS.PUBLISHER, publisher);
    builder.add(publisher, RDF.TYPE, FOAF.AGENT);
    builder.add(publisher, FOAF.NAME, "MOLGENIS-EMX2 FAIR Data Point API");
    builder.add(root, DCTERMS.LICENSE, iri("https://www.gnu.org/licenses/lgpl-3.0.rdf"));
    builder.add(root, DCTERMS.CONFORMS_TO, profile);
    builder.add(root, DCAT.ENDPOINT_URL, root);
    builder.add(root, iri("https://w3id.org/fdp/fdp-o#metadataIdentifier"), identifier);
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#metadataIssued"),
        literal(formatter.format(currentDateTime), XSD.DATETIME));
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#metadataModified"),
        literal(formatter.format(currentDateTime), XSD.DATETIME));
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#conformsToFdpSpec"),
        iri("https://specs.fairdatapoint.org/v1.0"));
    if (allCatalogFromJSON.size() != 0) {
      builder.add(catalog, RDF.TYPE, LDP.DIRECT_CONTAINER);
      builder.add(catalog, DCTERMS.TITLE, "Catalogs");
      builder.add(
          catalog,
          iri("http://www.w3.org/ns/ldp#hasMemberRelation"),
          iri("http://www.re3data.org/schema/3-0#dataCatalog"));
      builder.add(
          catalog,
          iri("http://www.w3.org/ns/ldp#hasMemberRelation"),
          iri("https://w3id.org/fdp/fdp-o#metadataCatalog"));
      builder.add(catalog, iri("http://www.w3.org/ns/ldp#membershipResource"), root);
      for (String schemaName : allCatalogFromJSON.keySet()) {
        for (Map<String, Object> map : allCatalogFromJSON.get(schemaName)) {
          IRI catalogIRI = iri(catalog + "/" + schemaName + "/" + map.get("id"));
          builder.add(catalog, iri("http://www.w3.org/ns/ldp#contains"), catalogIRI);
          builder.add(root, iri("https://w3id.org/fdp/fdp-o#metadataCatalog"), catalogIRI);
        }
      }
    }

    /*
    Optional in FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(
        root,
        DCTERMS.DESCRIPTION,
        "FAIR Data Point hosted by MOLGENIS-EMX2 at "
            + root
            + ". This implementation follows the FAIR Data Point Working Draft, 23 August 2021 at https://specs.fairdatapoint.org/.");
    builder.add(root, DCTERMS.LANGUAGE, iri("http://lexvo.org/id/iso639-3/eng"));
    BNode rights = vf.createBNode();
    builder.add(root, DCTERMS.RIGHTS, rights);
    builder.add(rights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(rights, DCTERMS.DESCRIPTION, "Rights are provided on a per-dataset basis.");
    BNode accessRights = vf.createBNode();
    builder.add(root, DCTERMS.ACCESS_RIGHTS, accessRights);
    builder.add(accessRights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(
        accessRights, DCTERMS.DESCRIPTION, "Access rights are provided on a per-dataset basis.");
    BNode vcard = vf.createBNode();
    builder.add(root, DCAT.CONTACT_POINT, vcard);
    builder.add(vcard, RDF.TYPE, VCARD4.KIND);
    builder.add(vcard, VCARD4.INDIVIDUAL, "MOLGENIS support desk");
    builder.add(vcard, VCARD4.HAS_EMAIL, "molgenis-support@umcg.nl");
    builder.add(vcard, VCARD4.HAS_URL, "https://molgenis.org/");
    builder.add(root, DCAT.ENDPOINT_DESCRIPTION, openAPI);
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#startDate"),
        literal(formatter.format(currentDateTime), XSD.DATETIME));
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#endDate"),
        literal(formatter.format(currentDateTime), XSD.DATETIME));
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#uiLanguage"),
        iri("http://lexvo.org/id/iso639-3/eng"));
    builder.add(root, iri("https://w3id.org/fdp/fdp-o#hasSoftwareVersion"), version);
    builder.add(
        root,
        iri("https://w3id.org/fdp/fdp-o#fdpSoftwareVersion"),
        iri("http://purl.obolibrary.org/obo/NCIT_C48660"));

    /*
    Not part of FDP specification but good practice (https://specs.fairdatapoint.org/)
     */
    builder.add(root, RDF.TYPE, DCAT.RESOURCE);
    builder.add(root, RDF.TYPE, DCAT.DATA_SERVICE);
    builder.add(root, RDF.TYPE, iri("https://w3id.org/fdp/fdp-o#FAIRDataPoint"));
    builder.add(root, RDFS.LABEL, "FAIR Data Point hosted by MOLGENIS-EMX2 at " + root);
    builder.add(root, DCTERMS.HAS_VERSION, version);
    builder.add(root, iri("http://www.re3data.org/schema/3-0#repositoryIdentifier"), identifier);
    builder.add(identifier, RDF.TYPE, iri("http://purl.org/spar/datacite/Identifier"));
    builder.add(identifier, DCTERMS.IDENTIFIER, root);
    builder.add(profile, RDFS.LABEL, "FAIR Data Point Profile");
    builder.add(profile, RDFS.LABEL, "Repository Profile");

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
