package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.fairdatapoint.FormatMimeTypes.FORMATS;
import static org.molgenis.emx2.fairdatapoint.FormatMimeTypes.formatToMediaType;
import static org.molgenis.emx2.fairdatapoint.Queries.queryDistribution;
import static org.molgenis.emx2.rdf.RDFService.*;
import static org.molgenis.emx2.rdf.RDFUtils.*;

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

  /**
   * Access a dataset distribution by a combination of schema, table, and format. Example:
   * http://localhost:8080/api/fdp/distribution/rd3/Analyses/jsonld
   *
   * @param request
   * @param database
   * @throws Exception
   */
  public FAIRDataPointDistribution(Request request, Database database) throws Exception {

    String schemaParam = request.params("schema");
    String distributionParam = request.params("distribution");
    String formatParam = request.params("format");

    if (schemaParam == null || distributionParam == null || formatParam == null) {
      throw new Exception(
          "You must provide 3 parameters: schema, distribution (or file), and format");
    }

    if (database.getSchema(schemaParam) == null) {
      throw new Exception("Schema unknown.");
    }

    // 'distribution' must either refer to the name of existing and visible 'Distribution' by a
    // file contained therein
    Schema schemaObj = database.getSchema(schemaParam);
    List<Map<String, Object>> distrByName = queryDistribution(schemaObj, "name", distributionParam);
    List<Map<String, Object>> distrByFile =
        queryDistribution(schemaObj, "files:{identifier", distributionParam);

    if (distrByName.size() == 0 && distrByFile.size() == 0) {
      throw new Exception("Distribution or file therein not found");
    } else if (distrByName.size() > 0 && distrByFile.size() > 0) {
      throw new Exception(
          "Cannot resolve distribution because it is ambiguous: file name equal to a table name. Please resolve this issue.");
    }

    List<Map<String, Object>> distributions;
    if (distrByName.size() > 0) {
      distributions = distrByName;
    } else {
      distributions = distrByFile;
    }

    Map type = (Map) distributions.get(0).get("type"); // type and type.name are required
    boolean refersToTable = type.get("name").equals("Table");

    if (refersToTable) {
      formatParam = formatParam.toLowerCase();
      if (!FORMATS.contains(formatParam)) {
        throw new Exception("Format unknown. Use any of: " + FORMATS);
      }
      if (database.getSchema(schemaParam).getTable(distributionParam) == null) {
        throw new Exception("Table unknown.");
      }
    }

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
            + distributionParam
            + " in schema "
            + schemaParam
            + ", formatted as "
            + formatParam
            + ".");

    if (refersToTable) {
      if (formatParam.equals("csv")
          || formatParam.equals("jsonld")
          || formatParam.equals("ttl")
          || formatParam.equals("excel")
          || formatParam.equals("zip")) {
        builder.add(
            reqURL,
            DCAT.DOWNLOAD_URL,
            encodedIRI(host + "/" + schemaParam + "/api/" + formatParam + "/" + distributionParam));
      } else if (formatParam.equals("graphql")) {
        List<String> columnNames =
            database
                .getSchema(schemaParam)
                .getTable(distributionParam)
                .getMetadata()
                .getColumnNames();
        // GraphQL, e.g. http://localhost:8080/fdh/graphql?query={Analyses{id,etc}}
        builder.add(
            reqURL,
            DCAT.DOWNLOAD_URL,
            encodedIRI(
                host
                    + "/"
                    + schemaParam
                    + "/graphql?query={"
                    + distributionParam
                    + "{"
                    + (String.join(",", columnNames))
                    + "}}"));
      } else {
        // all "rdf-" flavours
        builder.add(
            reqURL,
            DCAT.DOWNLOAD_URL,
            encodedIRI(
                host
                    + "/"
                    + schemaParam
                    + "/api/rdf/"
                    + distributionParam
                    + "?format="
                    + formatParam.replace("rdf-", "")));
      }
      builder.add(reqURL, DCAT.MEDIA_TYPE, iri(formatToMediaType(formatParam)));
    } else {
      // file
      // todo
    }

    builder.add(reqURL, DCTERMS.FORMAT, formatParam);

    for (Map distribution : distributions) {
      builder.add(
          reqURL,
          DCTERMS.ISSUED,
          literal(
              TypeUtils.toString(distribution.get("mg_insertedOn")).substring(0, 19),
              XSD.DATETIME));
      builder.add(
          reqURL,
          DCTERMS.MODIFIED,
          literal(
              TypeUtils.toString(distribution.get("mg_updatedOn")).substring(0, 19), XSD.DATETIME));
      List<Map> datasets = (List<Map>) distribution.get("belongsToDataset");
      for (Map dataset : datasets) {
        if (dataset.get("license") != null) {
          builder.add(reqURL, DCTERMS.LICENSE, dataset.get("license"));
        }
        if (dataset.get("accessRights") != null) {
          builder.add(reqURL, DCTERMS.ACCESS_RIGHTS, dataset.get("accessRights"));
        }
        if (dataset.get("rights") != null) {
          builder.add(reqURL, DCTERMS.RIGHTS, dataset.get("rights"));
        }
      }
    }

    builder.add(reqURL, DCTERMS.CONFORMS_TO, iri("http://www.w3.org/ns/dcat#Distribution"));
    // todo odrl:Policy? https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    this.result = stringWriter.toString();
  }
}
