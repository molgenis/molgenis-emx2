package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.fairdatapoint.FAIRDataPointDataset.queryDataset;
import static org.molgenis.emx2.semantics.RDFService.extractHost;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.encodedIRI;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.getURI;

import java.io.StringWriter;
import java.net.URI;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;
import spark.Request;

public class FAIRDataPointDistribution {

  private String result;

  public String getResult() {
    return result;
  }

  public static Set<String> FORMATS =
      new TreeSet<>(
          Set.of(
              "csv",
              "jsonld",
              "ttl",
              "excel",
              "zip",
              "rdf-ttl",
              "rdf-n3",
              "rdf-ntriples",
              "rdf-nquads",
              "rdf-xml",
              "rdf-trig",
              "rdf-jsonld",
              "graphql"));

  /**
   * Access a dataset distribution by a combination of schema, table, and format. Example:
   * http://localhost:8080/api/fdp/distribution/rd3/Analyses/jsonld
   *
   * @param request
   * @param database
   * @throws Exception
   */
  public FAIRDataPointDistribution(Request request, Database database) throws Exception {

    String schema = request.params("schema");
    String table = request.params("table");
    String format = request.params("format");

    if (schema == null || table == null || format == null) {
      throw new Exception("You must provide 3 parameters: schema, table, and format");
    }

    format = format.toLowerCase();
    if (!FORMATS.contains(format)) {
      throw new Exception("Format unknown. Use any of: " + FORMATS);
    }

    if (database.getSchema(schema) == null) {
      throw new Exception("Schema unknown.");
    }

    if (database.getSchema(schema).getTable(table) == null) {
      throw new Exception("Table unknown.");
    }

    // don't trust user: check if distribution is really part of a dataset
    Schema schemaObj = database.getSchema(schema);
    List<Map<String, Object>> datasetsFromJSON = queryDataset(schemaObj, "distribution", table);
    if (datasetsFromJSON.size() == 0) {
      throw new Exception(
          "Requested table distribution exists within schema, but is not part of any dataset in the schema and is therefore not retrievable.");
    }
    if (datasetsFromJSON.size() > 1) {
      throw new Exception(
          "Reference in dataset to table distribution is not unique (was secondary key removed?), cannot pinpoint source.");
    }
    Map sourceDataset = datasetsFromJSON.get(0);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");

    // Main model builder
    ModelBuilder builder = new ModelBuilder();
    RDFFormat applicationOntologyFormat = RDFFormat.TURTLE;
    WriterConfig config = new WriterConfig();
    config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
    for (String prefix : prefixToNamespace.keySet()) {
      builder.setNamespace(prefix, prefixToNamespace.get(prefix));
    }

    // reconstruct server:port URL to prevent problems with double encoding of schema/table names
    URI requestURI = getURI(request.url());
    String host = extractHost(requestURI);
    IRI reqURL = iri(request.url()); // escaping/encoding seems OK

    /*
    See https://www.w3.org/TR/vocab-dcat-2/#Class:Distribution
     */
    builder.add(reqURL, RDF.TYPE, DCAT.DISTRIBUTION);
    builder.add(reqURL, DCTERMS.TITLE, "Data distribution for " + reqURL);
    builder.add(
        reqURL,
        DCTERMS.DESCRIPTION,
        "MOLGENIS EMX2 data distribution at "
            + host
            + " for table "
            + table
            + " in schema "
            + schema
            + ", formatted as "
            + format
            + ".");
    if (format.equals("csv")
        || format.equals("jsonld")
        || format.equals("ttl")
        || format.equals("excel")
        || format.equals("zip")) {
      builder.add(
          reqURL,
          DCAT.DOWNLOAD_URL,
          encodedIRI(host + "/" + schema + "/api/" + format + "/" + table));
    } else if (format.equals("graphql")) {
      List<String> columnNames =
          database.getSchema(schema).getTable(table).getMetadata().getColumnNames();
      // GraphQL, e.g. http://localhost:8080/fdh/graphql?query={Analyses{id,etc}}
      builder.add(
          reqURL,
          DCAT.DOWNLOAD_URL,
          encodedIRI(
              host
                  + "/"
                  + schema
                  + "/graphql?query={"
                  + table
                  + "{"
                  + (String.join(",", columnNames))
                  + "}}"));
    } else {
      // all "rdf-" flavours
      builder.add(
          reqURL,
          DCAT.DOWNLOAD_URL,
          encodedIRI(
              host + "/" + schema + "/api/rdf/" + table + "?format=" + format.replace("rdf-", "")));
    }

    builder.add(reqURL, DCAT.MEDIA_TYPE, iri(formatToMediaType(format)));
    builder.add(reqURL, DCTERMS.FORMAT, format);
    builder.add(
        reqURL,
        DCTERMS.ISSUED,
        literal(
            TypeUtils.toString(sourceDataset.get("mg_insertedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(
        reqURL,
        DCTERMS.MODIFIED,
        literal(
            TypeUtils.toString(sourceDataset.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
    if (sourceDataset.get("license") != null) {
      builder.add(reqURL, DCTERMS.LICENSE, sourceDataset.get("license"));
    }
    if (sourceDataset.get("accessRights") != null) {
      builder.add(reqURL, DCTERMS.ACCESS_RIGHTS, sourceDataset.get("accessRights"));
    }
    if (sourceDataset.get("rights") != null) {
      builder.add(reqURL, DCTERMS.RIGHTS, sourceDataset.get("rights"));
    }
    builder.add(reqURL, DCTERMS.CONFORMS_TO, iri("http://www.w3.org/ns/dcat#Distribution"));
    // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }

  /**
   * Convert a format into its corresponding MIME type
   *
   * @param format
   * @return
   * @throws Exception
   */
  public static String formatToMediaType(String format) throws Exception {
    String mediaType;
    switch (format) {
      case "csv":
        mediaType = "https://www.iana.org/assignments/media-types/text/csv";
        break;
      case "jsonld":
      case "rdf-jsonld":
      case "graphql":
        mediaType = "https://www.iana.org/assignments/media-types/application/ld+json";
        break;
      case "ttl":
      case "rdf-ttl":
        mediaType = "https://www.iana.org/assignments/media-types/text/turtle";
        break;
      case "excel":
        mediaType = "https://www.iana.org/assignments/media-types/application/vnd.ms-excel";
        break;
      case "zip":
        mediaType = "https://www.iana.org/assignments/media-types/application/zip";
        break;
      case "rdf-n3":
        mediaType = "https://www.iana.org/assignments/media-types/text/n3";
        break;
      case "rdf-ntriples":
        mediaType = "https://www.iana.org/assignments/media-types/application/n-triples";
        break;
      case "rdf-nquads":
        mediaType = "https://www.iana.org/assignments/media-types/application/n-quads";
        break;
      case "rdf-xml":
        mediaType = "https://www.iana.org/assignments/media-types/application/rdf+xml";
        break;
      case "rdf-trig":
        mediaType = "https://www.iana.org/assignments/media-types/application/trig";
        break;
      default:
        throw new Exception("MIME Type could not be assigned");
    }
    return mediaType;
  }
}
