package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.rdf.RDFService.*;

import java.io.StringWriter;
import java.net.URI;
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
import org.molgenis.emx2.Version;
import spark.Request;

public class FAIRDataPoint {

  // todo: double check cardinality

  /**
   * Constructor
   *
   * @param request
   * @param schemas
   * @throws Exception
   */
  public FAIRDataPoint(Request request, Schema... schemas) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date currentDateTime = new Date(System.currentTimeMillis());
    this.issued = formatter.format(currentDateTime);
    this.modified = formatter.format(currentDateTime);
    this.version = Version.getVersion();
    this.request = request;
    this.schemas = schemas;
  }

  private String version;
  private String issued;
  private String modified;
  private Request request;
  private Schema[] schemas;

  /**
   * Used to override version for JUnit testing
   *
   * @param version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Used to override issued for JUnit testing
   *
   * @param issued
   */
  public void setIssued(String issued) {
    this.issued = issued;
  }

  /**
   * Used to override modified for JUnit testing
   *
   * @param modified
   */
  public void setModified(String modified) {
    this.modified = modified;
  }

  /**
   * Create and get resulting FDP
   *
   * @return
   * @throws Exception
   */
  public String getResult() throws Exception {
    // get all Catalog records from all of the supplied tables
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

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    String requestURL = request.url();
    URI requestURI = getURI(requestURL);
    String host = extractHost(requestURI);
    String apiFdp = host + "/api/fdp";
    String apiFdpCatalog = apiFdp + "/catalog";
    String apiFdpProfile = apiFdp + "/catalog";
    String apiFdpIdentifier = apiFdp + "#identifier";

    IRI apiFdpEnc = encodedIRI(apiFdp);
    IRI apiFdpCatalogEnc = encodedIRI(apiFdpCatalog);
    IRI apiFdpProfileEnc = encodedIRI(apiFdpProfile);
    IRI apiFdpIdentifierEnc = encodedIRI(apiFdpIdentifier);

    /*
    Required by FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(apiFdpEnc, RDF.TYPE, iri("https://w3id.org/fdp/fdp-o#MetadataService"));
    builder.add(
        apiFdpEnc, DCTERMS.TITLE, "FAIR Data Point hosted by MOLGENIS-EMX2 at " + apiFdpEnc);
    BNode publisher = vf.createBNode();
    builder.add(apiFdpEnc, DCTERMS.PUBLISHER, publisher);
    builder.add(publisher, RDF.TYPE, FOAF.AGENT);
    builder.add(publisher, FOAF.NAME, "MOLGENIS-EMX2 FAIR Data Point API");
    builder.add(apiFdpEnc, DCTERMS.LICENSE, iri("https://www.gnu.org/licenses/lgpl-3.0.rdf"));
    builder.add(apiFdpEnc, DCTERMS.CONFORMS_TO, apiFdpProfileEnc);
    builder.add(apiFdpEnc, DCAT.ENDPOINT_URL, apiFdpEnc);
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#metadataIdentifier"), apiFdpIdentifierEnc);
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#metadataIssued"), literal(issued, XSD.DATETIME));
    builder.add(
        apiFdpEnc,
        iri("https://w3id.org/fdp/fdp-o#metadataModified"),
        literal(modified, XSD.DATETIME));
    builder.add(
        apiFdpEnc,
        iri("https://w3id.org/fdp/fdp-o#conformsToFdpSpec"),
        iri("https://specs.fairdatapoint.org/v1.0"));
    if (allCatalogFromJSON.size() != 0) {
      builder.add(apiFdpCatalogEnc, RDF.TYPE, LDP.DIRECT_CONTAINER);
      builder.add(apiFdpCatalogEnc, DCTERMS.TITLE, "Catalogs");
      builder.add(
          apiFdpCatalogEnc,
          iri("http://www.w3.org/ns/ldp#hasMemberRelation"),
          iri("http://www.re3data.org/schema/3-0#dataCatalog"));
      builder.add(
          apiFdpCatalogEnc,
          iri("http://www.w3.org/ns/ldp#hasMemberRelation"),
          iri("https://w3id.org/fdp/fdp-o#metadataCatalog"));
      builder.add(apiFdpCatalogEnc, iri("http://www.w3.org/ns/ldp#membershipResource"), apiFdpEnc);
      for (String schemaName : allCatalogFromJSON.keySet()) {
        for (Map<String, Object> map : allCatalogFromJSON.get(schemaName)) {
          IRI catalogIriEnc = encodedIRI(apiFdpCatalog + "/" + schemaName + "/" + map.get("id"));
          builder.add(apiFdpCatalogEnc, iri("http://www.w3.org/ns/ldp#contains"), catalogIriEnc);
          builder.add(apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#metadataCatalog"), catalogIriEnc);
        }
      }
    }

    /*
    Optional in FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(
        apiFdpEnc,
        DCTERMS.DESCRIPTION,
        "FAIR Data Point hosted by MOLGENIS-EMX2 at "
            + apiFdpEnc
            + ". This implementation follows the FAIR Data Point Working Draft, 23 August 2021 at https://specs.fairdatapoint.org/.");
    builder.add(apiFdpEnc, DCTERMS.LANGUAGE, iri("http://lexvo.org/id/iso639-3/eng"));
    BNode rights = vf.createBNode();
    builder.add(apiFdpEnc, DCTERMS.RIGHTS, rights);
    builder.add(rights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(rights, DCTERMS.DESCRIPTION, "Rights are provided on a per-dataset basis.");
    BNode accessRights = vf.createBNode();
    builder.add(apiFdpEnc, DCTERMS.ACCESS_RIGHTS, accessRights);
    builder.add(accessRights, RDF.TYPE, DCTERMS.RIGHTS_STATEMENT);
    builder.add(
        accessRights, DCTERMS.DESCRIPTION, "Access rights are provided on a per-dataset basis.");
    BNode vcard = vf.createBNode();
    builder.add(apiFdpEnc, DCAT.CONTACT_POINT, vcard);
    builder.add(vcard, RDF.TYPE, VCARD4.KIND);
    builder.add(vcard, VCARD4.INDIVIDUAL, "MOLGENIS support desk");
    builder.add(vcard, VCARD4.HAS_EMAIL, "molgenis-support@umcg.nl");
    builder.add(vcard, VCARD4.HAS_URL, "https://molgenis.org/");
    builder.add(apiFdpEnc, DCAT.ENDPOINT_DESCRIPTION, encodedIRI(host + "/api/openapi"));
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#startDate"), literal(issued, XSD.DATETIME));
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#endDate"), literal(modified, XSD.DATETIME));
    builder.add(
        apiFdpEnc,
        iri("https://w3id.org/fdp/fdp-o#uiLanguage"),
        iri("http://lexvo.org/id/iso639-3/eng"));
    builder.add(apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#hasSoftwareVersion"), this.version);
    builder.add(
        apiFdpEnc,
        iri("https://w3id.org/fdp/fdp-o#fdpSoftwareVersion"),
        iri("http://purl.obolibrary.org/obo/NCIT_C48660"));

    /*
    Not part of FDP specification but good practice (https://specs.fairdatapoint.org/)
     */
    builder.add(apiFdpEnc, RDF.TYPE, DCAT.RESOURCE);
    builder.add(apiFdpEnc, RDF.TYPE, DCAT.DATA_SERVICE);
    builder.add(apiFdpEnc, RDF.TYPE, iri("https://w3id.org/fdp/fdp-o#FAIRDataPoint"));
    builder.add(apiFdpEnc, RDFS.LABEL, "FAIR Data Point hosted by MOLGENIS-EMX2 at " + apiFdpEnc);
    builder.add(apiFdpEnc, DCTERMS.HAS_VERSION, this.version);
    builder.add(
        apiFdpEnc,
        iri("http://www.re3data.org/schema/3-0#repositoryIdentifier"),
        apiFdpIdentifierEnc);
    builder.add(apiFdpIdentifierEnc, RDF.TYPE, iri("http://purl.org/spar/datacite/Identifier"));
    builder.add(apiFdpIdentifierEnc, DCTERMS.IDENTIFIER, apiFdpEnc);
    builder.add(apiFdpProfileEnc, RDFS.LABEL, "FAIR Data Point Profile");
    builder.add(apiFdpProfileEnc, RDFS.LABEL, "Repository Profile");

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    return stringWriter.toString();
  }
}
