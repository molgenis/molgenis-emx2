package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.fairdatapoint.FAIRDataPointDataset.queryDataset;

import java.io.StringWriter;
import java.util.*;
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
import org.molgenis.emx2.*;
import spark.Request;

public class FAIRDataPointDistribution {

  private String result;

  public String getResult() {
    return result;
  }

  public static Set<String> FORMATS =
      new TreeSet<String>(Set.of("csv", "jsonld", "ttl", "excel", "zip"));

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
      throw new Exception("Format unknown. Use 'jsonld', 'ttl', 'csv', 'excel' or 'zip'.");
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
    builder.add(
        reqURL,
        DCTERMS.ISSUED,
        literal(((String) sourceDataset.get("mg_insertedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(
        reqURL,
        DCTERMS.MODIFIED,
        literal(((String) sourceDataset.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
    builder.add(reqURL, DCTERMS.LICENSE, sourceDataset.get("license"));
    builder.add(reqURL, DCTERMS.ACCESS_RIGHTS, sourceDataset.get("accessRights"));
    builder.add(reqURL, DCTERMS.RIGHTS, sourceDataset.get("rights"));
    builder.add(reqURL, DCTERMS.CONFORMS_TO, iri("http://www.w3.org/ns/dcat#Distribution"));
    // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
