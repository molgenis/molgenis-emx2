package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.RDFUtils.*;

import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

// TODO check null value handling
// TODO check value types
// TODO make sure no classes are used as predicates and vice versa
// TODO: ontology tables need semantics to denote "what are these rows instances of?" (typeOf in FG)
// TODO: units for values?

/**
 * Nomenclature used from:
 *
 * <ul>
 *   <li>SIO (http://semanticscience.org)
 *   <li>RDF Data Cube (https://www.w3.org/TR/vocab-data-cube)
 *   <li>OWL, RDF, RDFS
 * </ul>
 */
public class RDFService {
  private static final Map<String, RDFFormat> RDF_FILE_FORMATS =
      Map.of(
          "ttl",
          RDFFormat.TURTLE,
          "n3",
          RDFFormat.N3,
          "ntriples",
          RDFFormat.NTRIPLES,
          "nquads",
          RDFFormat.NQUADS,
          "xml",
          RDFFormat.RDFXML,
          "trig",
          RDFFormat.TRIG,
          "jsonld",
          RDFFormat.JSONLD);
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  public static final IRI LDP_CONTAINS = Values.iri("http://www.w3.org/ns/ldp#contains");
  public static final String ROOT_NAMESPACE = "emx";
  public static final String NAMESPACE_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  public static final String NAMESPACE_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
  public static final String NAMESPACE_XSD = "http://www.w3.org/2001/XMLSchema#";
  public static final String NAMESPACE_OWL = "http://www.w3.org/2002/07/owl#";
  public static final String NAMESPACE_SIO = "http://semanticscience.org/resource/";
  public static final String NAMESPACE_QB = "http://purl.org/linked-data/cube#";
  public static final String NAMESPACE_DCTERMS = "http://purl.org/dc/terms/";
  public static final IRI IRI_DATABASE_TABLE =
      iri("http://semanticscience.org/resource/SIO_000754");
  public static final IRI IRI_DATASET = iri("http://purl.org/linked-data/cube#DataSet");
  public static final IRI IRI_CONTROLLED_VOCABULARY =
      iri("http://purl.obolibrary.org/obo/NCIT_C48697");
  /**
   * SIO:001055 = observing (definition: observing is a process of passive interaction in which one
   * entity makes note of attributes of one or more entities)
   */
  public static final IRI IRI_OBSERVING = iri("http://semanticscience.org/resource/SIO_001055");
  /** NCIT:C25474 = Data */
  public static final IRI IRI_DATA = iri("http://purl.obolibrary.org/obo/NCIT_C25474");
  /** NCIT:C21270 = Ontology */
  public static final IRI IRI_ONTOLOGY = iri("http://purl.obolibrary.org/obo/NCIT_C21270");
  /** SIO:000757 = database column */
  public static final IRI IRI_DATABASE_COLUMN =
      iri("http://semanticscience.org/resource/SIO_000757");

  public static final String SEMANTICS_ID_URL_STRING =
      "http://semanticscience.org/resource/SIO_000115";
  public static final IRI IRI_OBSERVATION = iri("http://purl.org/linked-data/cube#Observation");
  /** NCIT:C95637 = Coded Value Data Type */
  public static final IRI IRI_CODED_VALUE_DATATYPE =
      iri("http://purl.obolibrary.org/obo/NCIT_C95637");
  /** SIO:000750 = database */
  public static final IRI IRI_DATABASE = iri("http://semanticscience.org/resource/SIO_000750");

  public static final IRI IRI_MOLGENIS = iri("https://molgenis.org");
  public static final IRI IRI_MEASURE_PROPERTY =
      iri("http://purl.org/linked-data/cube#MeasureProperty");
  /** SIO:001187 = database row */
  public static final IRI IRI_DATABASE_ROW = iri("http://semanticscience.org/resource/SIO_001187");

  public static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final WriterConfig config;
  private final RDFFormat rdfFormat;
  /**
   * The baseURL is the URL at which MOLGENIS is deployed, include protocol and port (if deviating
   * from the protocol default port). This is used because we need to be able to refer to different
   * schemas.
   */
  private final String baseURL;
  /** The rdfAPIPath is the relative path for the RDF api within a schema. */
  private final String rdfAPIPath;

  public RDFService(String baseURL, String rdfAPIPath, String format) {
    // Ensure that the base URL has a trailing "/" so we can use it easily to
    // construct URL paths.
    if (baseURL.trim().endsWith("/")) {
      this.baseURL = baseURL.trim();
    } else {
      this.baseURL = baseURL.trim() + "/";
    }
    // Ensure that the stored rdfAPIPath has a leading and trailing "/" so we
    // can use it easily to construct URL paths.
    String temp = rdfAPIPath.trim();
    if (!temp.startsWith("/")) {
      temp = "/" + temp;
    }
    if (!temp.endsWith("/")) {
      temp = temp + "/";
    }
    this.rdfAPIPath = temp;

    if (format == null) {
      this.rdfFormat = RDFFormat.TURTLE;
    } else {
      if (!RDF_FILE_FORMATS.containsKey(format)) {
        throw new MolgenisException("Format unknown. Use any of: " + RDF_FILE_FORMATS.keySet());
      }
      this.rdfFormat = RDF_FILE_FORMATS.get(format);
    }

    this.config = new WriterConfig();
    this.config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
  }

  /**
   * Retrieve EMX2 data described as RDF. Can be used in different ways:
   *
   * <ul>
   *   <li>Call with one or more schemas, table and rowId null: retrieve all data from selected
   *       schemas
   *   <li>Call with a table, schema of that table, rowId null: retrieve all data from selected
   *       table
   *   <li>Call with a table, schema of that table, rowId provided: retrieve all data from selected
   *       row
   * </ul>
   *
   * <p>Each call will result in a full stack of data, containing the following elements:
   *
   * <ul>
   *   <li>Root node with server URL
   *   <li>Schema node(s) linked to its root
   *   <li>Table node(s) linked to its schema
   *   <li>Column node(s) linked to its table
   *   <li>Row node(s) linked to its table with value(s) linked to its column(s)
   * </ul>
   */
  public void describeAsRDF(
      OutputStream outputStream, Table table, String rowId, String columnName, Schema... schemas) {
    try {
      ModelBuilder builder = new ModelBuilder();
      builder.setNamespace("rdf", NAMESPACE_RDF);
      builder.setNamespace("rdfs", NAMESPACE_RDFS);
      builder.setNamespace("xsd", NAMESPACE_XSD);
      builder.setNamespace("owl", NAMESPACE_OWL);
      builder.setNamespace("sio", NAMESPACE_SIO);
      builder.setNamespace("qb", NAMESPACE_QB);
      builder.setNamespace("dcterms", NAMESPACE_DCTERMS);
      for (Schema schema : schemas) {
        Namespace ns = getSchemaNamespace(schema);
        builder.setNamespace(ns);
      }
      //      builder.setNamespace(ROOT_NAMESPACE, baseURL);
      describeRoot(builder);

      for (Schema schema : schemas) {
        describeSchema(builder, schema);
        List<Table> tables = table != null ? Arrays.asList(table) : schema.getTablesSorted();
        for (Table tableToDescribe : tables) {
          describeTable(builder, tableToDescribe);
          describeColumns(builder, tableToDescribe, columnName);
          // if a column name is provided then only provide column metadata, no row values
          if (columnName == null) {
            rowsToRdf(builder, tableToDescribe, rowId);
          }
        }
      }
      Rio.write(builder.build(), outputStream, rdfFormat, config);

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  public WriterConfig getConfig() {
    return config;
  }

  protected String getBaseURL() {
    return baseURL;
  }

  public String getMimeType() {
    return rdfFormat.getDefaultMIMEType();
  }

  public RDFFormat getRdfFormat() {
    return rdfFormat;
  }

  // todo: make non static and private
  protected void describeRoot(ModelBuilder builder) {
    builder
        .subject(baseURL)
        .add(RDF.TYPE, IRI_DATABASE)
        .add(RDFS.LABEL, "EMX2")
        .add(DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + baseURL)
        .add(DCTERMS.CREATOR, IRI_MOLGENIS);
  }

  private String getSchemaLocalName(Schema schema) {
    return TypeUtils.convertToPascalCase(schema.getName()) + rdfAPIPath;
  }

  private Namespace getSchemaNamespace(Schema schema) {
    String url = baseURL + TypeUtils.convertToPascalCase(schema.getName()) + rdfAPIPath;
    return Values.namespace(TypeUtils.convertToPascalCase(schema.getName()), url);
  }

  // TODO TableMetadata ipv Table
  private IRI getTableIRI(Table table) {
    var ns = getSchemaNamespace(table.getSchema());
    return Values.iri(ns, TypeUtils.convertToPascalCase(table.getName()) + "/");
  }

  // todo: make non-static
  private void describeSchema(ModelBuilder builder, Schema schema) {
    //    Namespace root = Values.namespace(ROOT_NAMESPACE, baseURL);
    //    IRI subject = Values.iri(root, getSchemaLocalName(schema));
    //    String subject = ROOT_NAMESPACE + ":" + getSchemaLocalName(schema);
    var subject = getSchemaNamespace(schema).getName();
    builder
        .subject(subject)
        .add(RDFS.LABEL, schema.getName())
        .add(DCTERMS.IS_PART_OF, Values.iri(baseURL))
        .add(subject, RDF.TYPE, RDFS.CONTAINER);
    if (schema.getMetadata().getDescription() != null) {
      builder.subject(subject).add(DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    for (Table table : schema.getTablesSorted()) {
      IRI object = getTableIRI(table);
      builder.subject(subject).add(LDP_CONTAINS, object);
    }
  }

  private void describeTable(ModelBuilder builder, Table table) {
    IRI subject = getTableIRI(table);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDF.TYPE, IRI_DATASET);
    builder.add(subject, RDF.TYPE, IRI_DATABASE_TABLE);
    if (table.getMetadata().getSemantics() != null) {
      for (String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(subject, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_CONTROLLED_VOCABULARY);
    } else {
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_OBSERVING);
    }
    builder.add(subject, RDFS.LABEL, table.getName());
    if (table.getMetadata().getDescriptions() != null) {
      for (var entry : table.getMetadata().getDescriptions().entrySet()) {
        builder.add(subject, DCTERMS.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
      }
    }
    if (table.getMetadata().getTableType() == TableType.DATA) {
      builder.add(subject, RDFS.RANGE, IRI_DATA);
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.RANGE, IRI_ONTOLOGY);
    }
  }

  private void describeColumns(
      final ModelBuilder builder, final Table table, final String columnName) {
    for (Column column : table.getMetadata().getColumns()) {
      if (columnName == null || columnName.equals(column.getName())) {
        describeColumn(builder, column);
      }
    }
  }

  private IRI getColumnIRI(final Column column) {
    var table = column.getTable();
    var schema = table.getTable().getSchema();
    var ns = getSchemaNamespace(schema);
    return Values.iri(
        ns,
        TypeUtils.convertToPascalCase(table.getTableName())
            + "/column/"
            + TypeUtils.convertToCamelCase(column.getName()));
  }

  private void describeColumn(ModelBuilder builder, Column column) {
    IRI subject = getColumnIRI(column);
    builder.add(subject, RDF.TYPE, IRI_DATABASE_COLUMN);
    builder.add(subject, RDF.TYPE, IRI_MEASURE_PROPERTY);
    if (column.isReference()) {
      builder.add(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      Table refTable = column.getRefTable().getTable();
      builder.add(subject, RDFS.RANGE, getTableIRI(refTable));
    } else {
      builder.add(subject, RDF.TYPE, OWL.DATATYPEPROPERTY);
      builder.add(subject, RDFS.RANGE, columnTypeToXSD(column.getColumnType()));
    }
    builder.add(subject, RDFS.LABEL, column.getName());
    builder.add(subject, RDFS.DOMAIN, getTableIRI(column.getTable().getTable()));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = SEMANTICS_ID_URL_STRING;
        }
        builder.add(subject, RDFS.ISDEFINEDBY, iri(columnSemantics));
      }
    }
    if (column.getDescriptions() != null) {
      for (var entry : column.getDescriptions().entrySet()) {
        builder.add(subject, DC.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
      }
    }
  }

  private CoreDatatype.XSD columnTypeToXSD(ColumnType columnType) {
    return switch (columnType) {
      case BOOL, BOOL_ARRAY -> CoreDatatype.XSD.BOOLEAN;
      case DATE, DATE_ARRAY -> CoreDatatype.XSD.DATE;
      case DATETIME, DATETIME_ARRAY -> CoreDatatype.XSD.DATETIME;
      case DECIMAL, DECIMAL_ARRAY -> CoreDatatype.XSD.DECIMAL;
      case EMAIL,
          EMAIL_ARRAY,
          HEADING,
          JSONB,
          JSONB_ARRAY,
          STRING,
          STRING_ARRAY,
          TEXT,
          TEXT_ARRAY,
          UUID,
          UUID_ARRAY,
          AUTO_ID -> CoreDatatype.XSD.STRING;
      case FILE,
          HYPERLINK,
          HYPERLINK_ARRAY,
          ONTOLOGY,
          ONTOLOGY_ARRAY,
          REF,
          REF_ARRAY,
          REFBACK -> CoreDatatype.XSD.ANYURI;
      case INT, INT_ARRAY -> CoreDatatype.XSD.INT;
      case LONG, LONG_ARRAY -> CoreDatatype.XSD.LONG;
      default -> throw new MolgenisException("ColumnType not mapped: " + columnType);
    };
  }

  /**
   * @return
   */
  public void rowsToRdf(ModelBuilder builder, Table table, String rowId) {
    IRI tableIRI = getTableIRI(table);
    for (Row row : getRows(table, rowId)) {
      IRI subject = getIriForRow(row, table.getMetadata());
      builder.add(subject, RDF.TYPE, tableIRI);
      builder.add(subject, RDF.TYPE, IRI_DATABASE_ROW);
      if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
        builder.add(subject, RDF.TYPE, IRI_CODED_VALUE_DATATYPE);
        if (row.getString(ONTOLOGY_TERM_URI) != null) {
          builder.add(subject, RDFS.ISDEFINEDBY, iri(row.getString(ONTOLOGY_TERM_URI)));
        }
      } else {
        builder.add(subject, RDF.TYPE, IRI_OBSERVATION);
      }
      builder.add(subject, IRI_DATASET, tableIRI);

      for (Column column : table.getMetadata().getColumns()) {
        IRI columnIRI = getColumnIRI(column);
        for (Value value : formatValue(row, column)) {
          builder.add(subject, columnIRI, value);
        }
      }
    }
  }

  private List<Row> getRows(Table table, String rowId) {
    Query query = table.query();
    if (rowId != null) {
      if (table.getMetadata().getPrimaryKeyFields().size() > 1) {
        query.where(decodeRowIdToFilter(rowId));
      } else {
        query.where(f(table.getMetadata().getPrimaryKeyColumns().get(0).getName(), EQUALS, rowId));
      }
    }
    return query.retrieveRows(); // we use the default select
  }

  private Filter decodeRowIdToFilter(String rowId) {
    try {
      List<NameValuePair> params =
          URLEncodedUtils.parse(new URI("?" + rowId), StandardCharsets.UTF_8);
      List<Filter> filters = new ArrayList<>();
      params.forEach(param -> filters.add(f(param.getName(), EQUALS, param.getValue())));
      return and(filters);
    } catch (Exception e) {
      throw new MolgenisException("Decode row to filter failed for id " + rowId);
    }
  }

  private List<NameValuePair> getKeyParts(Row row, TableMetadata metadata) {
    List<NameValuePair> keyParts = new ArrayList<>();
    for (Column column : metadata.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (var reference : column.getReferences()) {
          var values = row.getStringArray(reference.getName());
          for (var value : values) {
            keyParts.add(new BasicNameValuePair(reference.getName(), value));
          }
        }
      } else {
        keyParts.add(new BasicNameValuePair(column.getName(), row.get(column).toString()));
      }
    }
    return keyParts;
  }

  private IRI getIriForRow(Row row, TableMetadata metadata) {
    var keyParts = getKeyParts(row, metadata);
    var ns = getSchemaNamespace(metadata.getTable().getSchema());
    if (keyParts.size() == 1) {
      var value = URLEncoder.encode(keyParts.get(0).getValue(), StandardCharsets.UTF_8);
      return Values.iri(ns, TypeUtils.convertToPascalCase(metadata.getTableName()) + "/" + value);
    } else {
      var parameters = URLEncodedUtils.format(keyParts, StandardCharsets.UTF_8);
      return Values.iri(
          ns, TypeUtils.convertToPascalCase(metadata.getTableName()) + "/?" + parameters);
    }
  }

  private List<Value> formatValue(Row row, Column column) {
    List<Value> values = new ArrayList<>();
    ColumnType columnType = column.getColumnType();
    if (columnType.isReference()) {
      values.add(getIriForRow(row, column.getTable()));
    } else if (columnType.equals(ColumnType.FILE)) {
      if (row.getString(column.getName() + "_id") != null) {
        values.add(
            encodedIRI(
                TypeUtils.convertToPascalCase(column.getSchemaName())
                    + "/api/file/"
                    + column.getTableName()
                    + "/"
                    + column.getName()
                    + "/"));
      }
    } else {
      values.addAll(getLiteralValues(row, column));
    }
    return values;
  }

  private List<Value> getLiteralValues(Row row, Column column) {
    CoreDatatype.XSD xsdType = columnTypeToXSD(column.getColumnType());
    if (row.getString(column.getName()) == null) {
      return List.of();
    }
    return switch (xsdType) {
      case BOOLEAN -> Arrays.stream(row.getBooleanArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case DATE -> Arrays.stream(row.getDateArray(column.getName()))
          .map(value -> (Value) literal(value.toString(), xsdType))
          .toList();
      case DATETIME -> Arrays.stream(row.getDateTimeArray(column.getName()))
          .map(value -> (Value) literal(dateTimeFormatter.format(value), xsdType))
          .toList();
      case DECIMAL -> Arrays.stream(row.getDecimalArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case STRING -> Arrays.stream(row.getStringArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case ANYURI -> Arrays.stream(row.getStringArray(column.getName()))
          .map(value -> (Value) encodedIRI(value))
          .toList();
      case INT -> Arrays.stream(row.getIntegerArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      case LONG -> Arrays.stream(row.getLongArray(column.getName()))
          .map(value -> (Value) literal(value))
          .toList();
      default -> throw new MolgenisException("XSD type formatting not supported for: " + xsdType);
    };
  }
}
