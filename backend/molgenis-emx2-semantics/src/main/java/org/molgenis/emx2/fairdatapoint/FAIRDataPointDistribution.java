package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.molgenis.emx2.Database;
import spark.Request;

public class FAIRDataPointDistribution {

  private String result;

  public String getResult() {
    return result;
  }

  public static Set<String> FORMATS =
      new HashSet<String>(Set.of("csv", "jsonld", "ttl", "excel", "zip"));

  /**
   * E.g.
   *
   * <p>http://localhost:8080/api/fdp/distribution/rd3/Analyses/jsonld
   *
   * <p>todo prevent tables from being access that are not part of a Dataset via refback!
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
      throw new Exception("Format unknown. Use 'jsonld', 'ttl', 'csv', 'excel' or 'zip'.");
    }

    if (database.getSchema(schema) == null) {
      throw new Exception("Schema unknown.");
    }

    if (database.getSchema(schema).getTable(table) == null) {
      throw new Exception("Table unknown.");
    }

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
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

    schema = schema.replace(" ", "%20");
    table = table.replace(" ", "%20");

    IRI reqURL = iri(request.url());
    // todo check if ok
    String appURL =
        reqURL
            .toString()
            .replace("api/fdp/distribution/" + schema + "/" + table + "/" + format, "");

    /*
    See https://www.w3.org/TR/vocab-dcat-2/#Class:Distribution
     */
    builder.add(reqURL, RDF.TYPE, DCAT.DISTRIBUTION);
    builder.add(reqURL, DCTERMS.TITLE, "Data distribution for " + reqURL);
    builder.add(
        reqURL,
        DCTERMS.DESCRIPTION,
        "MOLGENIS EMX2 data distribution at "
            + appURL
            + " for table "
            + table
            + " in schema "
            + schema
            + ", formatted as "
            + format
            + ".");
    builder.add(reqURL, DCAT.DOWNLOAD_URL, iri(appURL + schema + "/api/" + format + "/" + table));

    switch (format) {
      case "jsonld":
        builder.add(
            reqURL,
            DCAT.MEDIA_TYPE,
            iri("https://www.iana.org/assignments/media-types/application/ld+json"));
        break;
      case "ttl":
        builder.add(
            reqURL,
            DCAT.MEDIA_TYPE,
            iri("https://www.iana.org/assignments/media-types/text/turtle"));
        break;
      case "csv":
        builder.add(
            reqURL, DCAT.MEDIA_TYPE, iri("https://www.iana.org/assignments/media-types/text/csv"));
        break;
      case "excel":
        builder.add(
            reqURL,
            DCAT.MEDIA_TYPE,
            iri("https://www.iana.org/assignments/media-types/application/vnd.ms-excel"));
        break;
      case "zip":
        builder.add(
            reqURL,
            DCAT.MEDIA_TYPE,
            iri("https://www.iana.org/assignments/media-types/application/zip"));
        break;
    }

    builder.add(reqURL, DCTERMS.FORMAT, format);

    // builder.add(fullURL, DCTERMS.ISSUED, ); todo: inherit from Dataset
    // builder.add(fullURL, DCTERMS.MODIFIED, ); todo: inherit from Dataset
    // builder.add(fullURL, DCTERMS.LICENSE, ); todo: inherit from Dataset
    // builder.add(fullURL, DCTERMS.ACCESS_RIGHTS, ); todo: inherit from Dataset
    // builder.add(fullURL, DCTERMS.RIGHTS, ); todo: inherit from Dataset
    builder.add(
        reqURL, DCTERMS.CONFORMS_TO, iri("https://www.w3.org/TR/vocab-dcat-2/#Class:Distribution"));

    // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
