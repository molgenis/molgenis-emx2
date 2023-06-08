package org.molgenis.emx2.fairdatapoint;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.fairdatapoint.FAIRDataPointCatalog.extractItemAsIRI;
import static org.molgenis.emx2.fairdatapoint.FormatMimeTypes.FORMATS;
import static org.molgenis.emx2.fairdatapoint.Queries.queryDataset;
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
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.TypeUtils;
import spark.Request;

public class FAIRDataPointDataset {

  // todo: check data types (int, date, hyperlinks etc)
  // todo odrl:Policy object instead of String? see
  // https://www.w3.org/TR/vocab-dcat-2/#Property:distribution_has_policy

  private final Request request;
  private final Table fdpDataseTable;
  private String issued;
  private String modified;

  /** Constructor */
  public FAIRDataPointDataset(Request request, Table fdpDataseTable) {
    this.request = request;
    this.fdpDataseTable = fdpDataseTable;
  }

  /** Used to override issued for JUnit testing */
  public void setIssued(String issued) {
    this.issued = issued;
  }

  /** Used to override modified for JUnit testing */
  public void setModified(String modified) {
    this.modified = modified;
  }

  /** Create and get resulting FDP */
  public String getResult() throws Exception {
    String id = request.params("id");
    Schema schema = fdpDataseTable.getSchema();
    List<Map<String, Object>> datasetsFromJSON = queryDataset(schema, "id", id);
    if (datasetsFromJSON == null) {
      throw new Exception("datasetsFromJSON is null");
    }
    if (datasetsFromJSON.size() != 1) {
      throw new Exception("Bad number of dataset results");
    }
    Map<String, Object> datasetFromJSON = datasetsFromJSON.get(0);

    // All prefixes and namespaces
    Map<String, String> prefixToNamespace = new HashMap<>();
    prefixToNamespace.put("dcterms", "http://purl.org/dc/terms/");
    prefixToNamespace.put("dcat", "http://www.w3.org/ns/dcat#");
    prefixToNamespace.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixToNamespace.put("prov", "http://www.w3.org/ns/prov#");
    prefixToNamespace.put("lang", "http://lexvo.org/id/iso639-3/");
    prefixToNamespace.put("odrl", "http://www.w3.org/ns/odrl/2/");
    prefixToNamespace.put("ldp", "http://www.w3.org/ns/ldp#");

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
    String apiFdp = host + "/api/fdp";
    String apiFdpDistribution = apiFdp + "/distribution";

    IRI reqUrl = iri(request.url()); // escaping/encoding seems OK
    IRI apiFdpDistributionEnc = encodedIRI(apiFdpDistribution);

    builder.add(reqUrl, RDF.TYPE, DCAT.DATASET);
    List<Map> distributions = (List<Map>) datasetFromJSON.get("distribution");
    for (Map distribution : distributions) {
      Map type = (Map) distribution.get("type"); // type is a required field

      // note: both type.name (an ontology) and the distribution name are required fields
      if (type.get("name").equals("Table")) {
        String distributionName = (String) distribution.get("name");
        if (!schema.getTableNames().contains(distributionName)) {
          throw new Exception(
              "Schema does not contain the requested table for distribution. Make sure the value of 'distribution' in your FDP_Dataset matches a table name (from the same schema) you want to publish.");
        }
        for (String format : FORMATS) {
          builder.add(
              reqUrl,
              // not 'Distribution' (class) but 'distribution' (predicate)
              iri("http://www.w3.org/ns/dcat#distribution"),
              encodedIRI(
                  apiFdpDistribution
                      + "/"
                      + schema.getName()
                      + "/"
                      + distribution.get("name")
                      + "/"
                      + format));
          builder.add(
              apiFdpDistributionEnc,
              LDP.CONTAINS,
              encodedIRI(
                  apiFdpDistribution
                      + "/"
                      + schema.getName()
                      + "/"
                      + distribution.get("name")
                      + "/"
                      + format));
        }
      } else {
        List<Map> files = (List<Map>) distribution.get("files");
        if (files == null) {
          throw new Exception("No files specified for distribution of type File");
        }
        for (Map m : files) {
          String format = (String) ((Map) m.get("format")).get("name");
          // must manually encode forward slash in edge cases like "nbrf/pir"
          format = format.replace("/", "%2F").toLowerCase(Locale.ROOT);
          // we don't use distribution.get("name") because it may contain multiple files
          // files are in the RemoteFiles table and identifier is unique
          builder.add(
              reqUrl,
              // not 'Distribution' (class) but 'distribution' (predicate)
              iri("http://www.w3.org/ns/dcat#distribution"),
              encodedIRI(
                  apiFdpDistribution
                      + "/"
                      + schema.getName()
                      + "/"
                      + m.get("identifier")
                      + "/"
                      + format));

          builder.add(
              apiFdpDistributionEnc,
              LDP.CONTAINS,
              encodedIRI(
                  apiFdpDistribution
                      + "/"
                      + schema.getName()
                      + "/"
                      + m.get("identifier")
                      + "/"
                      + format));
        }
      }
    }

    if (datasetFromJSON.get("accrualPeriodicity") != null) {
      builder.add(reqUrl, DCTERMS.ACCRUAL_PERIODICITY, datasetFromJSON.get("accrualPeriodicity"));
    }
    if (datasetFromJSON.get("spatial") != null) {
      ArrayList<IRI> spatials =
          extractItemAsIRI((List<LinkedHashMap>) datasetFromJSON.get("spatial"), "ontologyTermURI");
      for (IRI spatial : spatials) {
        builder.add(reqUrl, DCTERMS.SPATIAL, spatial);
      }
    }

    if (datasetFromJSON.get("spatialResolutionInMeters") != null) {
      builder.add(
          reqUrl,
          DCAT.SPATIAL_RESOLUTION_IN_METERS,
          literal((double) datasetFromJSON.get("spatialResolutionInMeters")));
    }
    if (datasetFromJSON.get("temporal") != null) {
      builder.add(reqUrl, DCTERMS.TEMPORAL, datasetFromJSON.get("temporal"));
    }
    if (datasetFromJSON.get("temporalResolution") != null) {
      builder.add(reqUrl, DCAT.TEMPORAL_RESOLUTION, datasetFromJSON.get("temporalResolution"));
    }
    if (datasetFromJSON.get("wasGeneratedBy") != null) {
      builder.add(reqUrl, PROV.WAS_GENERATED_BY, datasetFromJSON.get("wasGeneratedBy"));
    }
    if (datasetFromJSON.get("accessRights") != null) {
      builder.add(reqUrl, DCTERMS.ACCESS_RIGHTS, datasetFromJSON.get("accessRights"));
    }
    if (datasetFromJSON.get("contactPoint") != null) {
      builder.add(reqUrl, DCAT.CONTACT_POINT, datasetFromJSON.get("contactPoint"));
    }
    if (datasetFromJSON.get("creator") != null) {
      builder.add(reqUrl, DCTERMS.CREATOR, datasetFromJSON.get("creator"));
    }
    if (datasetFromJSON.get("description") != null) {
      builder.add(reqUrl, DCTERMS.DESCRIPTION, datasetFromJSON.get("description"));
    }
    if (datasetFromJSON.get("description") != null) {
      builder.add(reqUrl, ODRL2.HAS_POLICY, datasetFromJSON.get("description"));
    }
    if (datasetFromJSON.get("id") != null) {
      builder.add(reqUrl, DCTERMS.IDENTIFIER, datasetFromJSON.get("id"));
    }
    if (datasetFromJSON.get("isReferencedBy") != null) {
      builder.add(reqUrl, DCTERMS.IS_REFERENCED_BY, datasetFromJSON.get("isReferencedBy"));
    }
    if (datasetFromJSON.get("keyword") != null) {
      for (String keyword : (List<String>) datasetFromJSON.get("keyword")) {
        builder.add(reqUrl, DCAT.KEYWORD, keyword);
      }
    }

    if (datasetFromJSON.get("landingPage") != null) {
      builder.add(reqUrl, DCAT.LANDING_PAGE, datasetFromJSON.get("landingPage"));
    }
    if (datasetFromJSON.get("license") != null) {
      builder.add(reqUrl, DCTERMS.LICENSE, datasetFromJSON.get("license"));
    }
    if (datasetFromJSON.get("language") != null) {
      ArrayList<IRI> languages =
          extractItemAsIRI(
              (List<LinkedHashMap>) datasetFromJSON.get("language"), "ontologyTermURI");
      for (IRI language : languages) {
        builder.add(reqUrl, DCTERMS.LANGUAGE, language);
      }
    }
    if (datasetFromJSON.get("relation") != null) {
      builder.add(reqUrl, DCTERMS.RELATION, datasetFromJSON.get("relation"));
    }
    if (datasetFromJSON.get("rights") != null) {
      builder.add(reqUrl, DCTERMS.RIGHTS, datasetFromJSON.get("rights"));
    }
    if (datasetFromJSON.get("qualifiedRelation") != null) {
      builder.add(reqUrl, DCAT.QUALIFIED_RELATION, datasetFromJSON.get("qualifiedRelation"));
    }
    if (datasetFromJSON.get("publisher") != null) {
      builder.add(reqUrl, DCTERMS.PUBLISHER, datasetFromJSON.get("publisher"));
    }
    if (this.issued == null) {
      builder.add(
          reqUrl,
          DCTERMS.ISSUED,
          literal(
              TypeUtils.toString(datasetFromJSON.get("mg_insertedOn")).substring(0, 19),
              XSD.DATETIME));
    } else {
      builder.add(reqUrl, DCTERMS.ISSUED, literal(this.issued, XSD.DATETIME));
    }

    if (datasetFromJSON.get("theme") != null) {
      for (IRI themeIRI : hyperlinkArrayToIRIList((List<String>) datasetFromJSON.get("theme"))) {
        builder.add(reqUrl, DCAT.THEME, themeIRI);
      }
    }
    if (datasetFromJSON.get("title") != null) {
      builder.add(reqUrl, DCTERMS.TITLE, datasetFromJSON.get("title"));
    }
    if (datasetFromJSON.get("type") != null) {
      builder.add(reqUrl, DCTERMS.TYPE, datasetFromJSON.get("type"));
    }
    if (this.modified == null) {
      builder.add(
          reqUrl,
          DCTERMS.MODIFIED,
          literal(
              TypeUtils.toString(datasetFromJSON.get("mg_updatedOn")).substring(0, 19),
              XSD.DATETIME));
    } else {
      builder.add(reqUrl, DCTERMS.MODIFIED, literal(this.modified, XSD.DATETIME));
    }

    if (datasetFromJSON.get("qualifiedAttribution") != null) {
      builder.add(reqUrl, PROV.QUALIFIED_ATTRIBUTION, datasetFromJSON.get("qualifiedAttribution"));
    }
    builder.add(apiFdpDistributionEnc, RDF.TYPE, LDP.DIRECT_CONTAINER);
    builder.add(apiFdpDistributionEnc, DCTERMS.TITLE, "Distributions");
    builder.add(apiFdpDistributionEnc, LDP.MEMBERSHIP_RESOURCE, reqUrl);
    builder.add(apiFdpDistributionEnc, LDP.HAS_MEMBER_RELATION, DCAT.DISTRIBUTION);

    // Write model
    Model model = builder.build();
    StringWriter stringWriter = new StringWriter();
    Rio.write(model, stringWriter, applicationOntologyFormat, config);
    return stringWriter.toString();
  }

  /**
   * Convert EMX2 hyperlink_array to a list of IRIs
   *
   * @param object
   * @return
   */
  public static ArrayList<IRI> hyperlinkArrayToIRIList(List<String> object) {
    ArrayList<IRI> values = new ArrayList<>();
    for (String hyperlink : object) {
      IRI iri = iri(hyperlink);
      values.add(iri);
    }
    return values;
  }
}
