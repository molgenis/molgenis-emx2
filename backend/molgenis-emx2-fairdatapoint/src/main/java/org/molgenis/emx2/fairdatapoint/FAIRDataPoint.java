package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.rdf.RDFUtils.*;

import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.molgenis.emx2.Version;
import spark.Request;

public class FAIRDataPoint {

  private static final String FDP_ROOT_METADATA = "FAIR Data Point root metadata";

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
    this.version = "MOLGENIS EMX2 " + Version.getVersion();
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
    String apiFdpProfile = apiFdp + "/profile";
    String apiFdpIdentifier = apiFdp + "#identifier";

    IRI apiFdpEnc = encodedIRI(apiFdp);
    IRI apiFdpCatalogEnc = encodedIRI(apiFdpCatalog);
    IRI apiFdpProfileEnc = encodedIRI(apiFdpProfile);
    IRI apiFdpIdentifierEnc = encodedIRI(apiFdpIdentifier);

    /*
    Required by FDP specification (https://specs.fairdatapoint.org/)
     */
    builder.add(apiFdpEnc, RDF.TYPE, iri("https://w3id.org/fdp/fdp-o#MetadataService"));
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
    builder.add(apiFdpEnc, DCAT.ENDPOINT_DESCRIPTION, encodedIRI(host + "/api/openapi"));
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#startDate"), literal(issued, XSD.DATETIME));
    builder.add(
        apiFdpEnc, iri("https://w3id.org/fdp/fdp-o#endDate"), literal(modified, XSD.DATETIME));
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
    stringWriter.append(getFDPRootMetadata(apiFdpEnc.toString()));
    return stringWriter.toString();
  }

  private String getFDPRootMetadata(String apiFdpEnc) {
    for (Schema schema : schemas) {
      if (schema.hasSetting(FDP_ROOT_METADATA)) {
        return schema.getSettingValue(FDP_ROOT_METADATA);
      }
    }
    Schema schema = addFDPRootMetadataIfMissing(apiFdpEnc);
    return schema.getSettingValue(FDP_ROOT_METADATA);
  }

  private Schema addFDPRootMetadataIfMissing(String apiFdpEnc) {
    schemas[0]
        .getMetadata()
        .setSetting(
            FDP_ROOT_METADATA,
            """
            %s
            <%s>
              dcterms:title "FAIR Data Point hosted by MOLGENIS-EMX2";
              dcterms:publisher [ a foaf:Agent;
                  foaf:name "MOLGENIS-EMX2 FAIR Data Point API"
                ];
              dcterms:license <https://www.gnu.org/licenses/lgpl-3.0.rdf>;
              dcterms:description "FAIR Data Point hosted by MOLGENIS-EMX2. This implementation follows the FAIR Data Point Working Draft, 23 August 2021 at https://specs.fairdatapoint.org/.";
              dcterms:language lang:eng;
              dcterms:rights [ a dcterms:RightsStatement;
                  dcterms:description "Rights are provided on a per-dataset basis."
                ];
              dcterms:accessRights [ a dcterms:RightsStatement;
                  dcterms:description "Access rights are provided on a per-dataset basis."
                ];
              dcat:contactPoint [ a vc:Kind;
                  vc:Individual "MOLGENIS support desk";
                  vc:hasEmail "molgenis-support@umcg.nl";
                  vc:hasURL "https://molgenis.org/"
                ];
              fdp-o:uiLanguage lang:eng;
              rdfs:label "FAIR Data Point hosted by MOLGENIS-EMX2" .
              """
                .formatted(System.lineSeparator(), apiFdpEnc));
    return schemas[0];
  }
}
