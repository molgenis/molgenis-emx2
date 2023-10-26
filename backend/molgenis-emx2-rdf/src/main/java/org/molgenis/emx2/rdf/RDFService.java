package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.RDFUtils.*;

import com.google.common.net.UrlEscapers;
import java.io.OutputStream;
import java.net.URI;
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
 *   <li><a href="http://semanticscience.org">SIO</a>
 *   <li><a href="https://www.w3.org/TR/vocab-data-cube">RDF Data Cube</a>
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
  public static final String NAMESPACE_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  public static final String NAMESPACE_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
  public static final String NAMESPACE_XSD = "http://www.w3.org/2001/XMLSchema#";
  public static final String NAMESPACE_OWL = "http://www.w3.org/2002/07/owl#";
  public static final String NAMESPACE_SIO = "http://semanticscience.org/resource/";
  public static final String NAMESPACE_QB = "http://purl.org/linked-data/cube#";
  public static final String NAMESPACE_DCTERMS = "http://purl.org/dc/terms/";
  public static final IRI IRI_DATABASE_TABLE =
      Values.iri("http://semanticscience.org/resource/SIO_000754");
  public static final IRI IRI_DATASET = Values.iri("http://purl.org/linked-data/cube#DataSet");
  public static final IRI IRI_CONTROLLED_VOCABULARY =
      Values.iri("http://purl.obolibrary.org/obo/NCIT_C48697");
  /**
   * SIO:001055 = observing (definition: observing is a process of passive interaction in which one
   * entity makes note of attributes of one or more entities)
   */
  public static final IRI IRI_OBSERVING =
      Values.iri("http://semanticscience.org/resource/SIO_001055");
  /** NCIT:C25474 = Data */
  public static final IRI IRI_DATA = Values.iri("http://purl.obolibrary.org/obo/NCIT_C25474");
  /** NCIT:C21270 = Ontology */
  public static final IRI IRI_ONTOLOGY = Values.iri("http://purl.obolibrary.org/obo/NCIT_C21270");
  /** SIO:000757 = database column */
  public static final IRI IRI_DATABASE_COLUMN =
      Values.iri("http://semanticscience.org/resource/SIO_000757");

  public static final String SEMANTICS_ID_URL_STRING =
      "http://semanticscience.org/resource/SIO_000115";
  public static final IRI IRI_OBSERVATION =
      Values.iri("http://purl.org/linked-data/cube#Observation");
  /** NCIT:C95637 = Coded Value Data Type */
  public static final IRI IRI_CODED_VALUE_DATATYPE =
      Values.iri("http://purl.obolibrary.org/obo/NCIT_C95637");
  /** SIO:000750 = database */
  public static final IRI IRI_DATABASE =
      Values.iri("http://semanticscience.org/resource/SIO_000750");

  public static final IRI IRI_MOLGENIS = Values.iri("https://molgenis.org");
  public static final IRI IRI_MEASURE_PROPERTY =
      Values.iri("http://purl.org/linked-data/cube#MeasureProperty");
  /** SIO:001187 = database row */
  public static final IRI IRI_DATABASE_ROW =
      Values.iri("http://semanticscience.org/resource/SIO_001187");

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

  /**
   * Construct an RDF Service.
   *
   * @param baseURL the base URL of the MOLGENIS installation
   * @param rdfAPIPath the path fragment for the RDF service within a Schema
   * @param format the requested RDF document type
   */
  public RDFService(final String baseURL, final String rdfAPIPath, final String format) {
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
      final OutputStream outputStream,
      final Table table,
      final String rowId,
      final String columnName,
      final Schema... schemas) {
    try {
      final ModelBuilder builder = new ModelBuilder();
      builder.setNamespace("rdf", NAMESPACE_RDF);
      builder.setNamespace("rdfs", NAMESPACE_RDFS);
      builder.setNamespace("xsd", NAMESPACE_XSD);
      builder.setNamespace("owl", NAMESPACE_OWL);
      builder.setNamespace("sio", NAMESPACE_SIO);
      builder.setNamespace("qb", NAMESPACE_QB);
      builder.setNamespace("dcterms", NAMESPACE_DCTERMS);
      // Define the schemas at the start of the document.
      for (final Schema schema : schemas) {
        final Namespace ns = getSchemaNamespace(schema);
        builder.setNamespace(ns);
      }

      describeRoot(builder);

      for (final Schema schema : schemas) {
        describeSchema(builder, schema);
        final List<Table> tables = table != null ? Arrays.asList(table) : schema.getTablesSorted();
        for (final Table tableToDescribe : tables) {
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

  /**
   * Describe the MOLGENIS instance as a whole.
   *
   * @param builder the builder for constructing the final RDF document.
   */
  protected void describeRoot(final ModelBuilder builder) {
    builder
        .subject(baseURL)
        .add(RDFS.SUBCLASSOF, IRI_DATABASE)
        .add(RDFS.SUBCLASSOF, OWL.THING)
        .add(RDFS.LABEL, "EMX2")
        .add(DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + baseURL)
        .add(DCTERMS.CREATOR, IRI_MOLGENIS);
  }

  /**
   * Get the namespace for this schema
   *
   * @param schema the schema
   * @return A namespace that defines a local unique prefix for this schema.
   */
  private Namespace getSchemaNamespace(final Schema schema) {
    final String schemaName = UrlEscapers.urlPathSegmentEscaper().escape(schema.getName());
    final String url = baseURL + schemaName + rdfAPIPath;
    final String prefix = TypeUtils.convertToPascalCase(schema.getName());
    return Values.namespace(prefix, url);
  }

  /**
   * Get an IRI for the table. Taking the schema in which the table resides into consideration.
   *
   * @param table the table
   * @return An IRI that is based on the schema namespace.
   */
  private IRI getTableIRI(final Table table) {
    final Namespace ns = getSchemaNamespace(table.getSchema());
    return Values.iri(ns, table.getIdentifier());
  }

  private void describeSchema(final ModelBuilder builder, final Schema schema) {
    // The name from a name space is the IRI.
    final String subject = getSchemaNamespace(schema).getName();
    builder
        .subject(subject)
        .add(RDFS.LABEL, schema.getName())
        .add(DCTERMS.IS_PART_OF, Values.iri(baseURL))
        .add(subject, RDF.TYPE, RDFS.CONTAINER);
    if (schema.getMetadata().getDescription() != null) {
      builder.subject(subject).add(DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    for (final Table table : schema.getTablesSorted()) {
      final IRI object = getTableIRI(table);
      builder.subject(subject).add(LDP_CONTAINS, object);
    }
  }

  private void describeTable(final ModelBuilder builder, final Table table) {
    final IRI subject = getTableIRI(table);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDFS.SUBCLASSOF, IRI_DATASET);
    builder.add(subject, RDFS.SUBCLASSOF, IRI_DATABASE_TABLE);
    builder.add(subject, RDFS.SUBCLASSOF, OWL.THING);
    // Specify the tables that this table inherits from.
    Table parent = table.getInheritedTable();
    while (parent != null) {
      builder.add(subject, RDFS.SUBCLASSOF, getTableIRI(parent));
      parent = parent.getInheritedTable();
    }

    if (table.getMetadata().getSemantics() != null) {
      for (final String tableSemantics : table.getMetadata().getSemantics()) {
        builder.add(subject, RDFS.ISDEFINEDBY, iri(tableSemantics));
      }
    } else if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_CONTROLLED_VOCABULARY);
    } else {
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_OBSERVING);
    }
    builder.add(subject, RDFS.LABEL, table.getName());

    if (table.getMetadata().getDescriptions() != null) {
      for (final var entry : table.getMetadata().getDescriptions().entrySet()) {
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
    for (final Column column : table.getMetadata().getColumns()) {
      // Exclude the system columns like mg_insertedBy
      if (column.isSystemColumn()) {
        continue;
      }
      if (columnName == null || columnName.equals(column.getName())) {
        describeColumn(builder, column);
      }
    }
  }

  private IRI getColumnIRI(final Column column) {
    final TableMetadata table = column.getTable();
    final Schema schema = table.getTable().getSchema();
    final String tableName = UrlEscapers.urlPathSegmentEscaper().escape(table.getTableName());
    final String columnName = UrlEscapers.urlPathSegmentEscaper().escape(column.getName());
    final Namespace ns = getSchemaNamespace(schema);
    return Values.iri(ns, tableName + "/column/" + columnName);
  }

  private void describeColumn(final ModelBuilder builder, final Column column) {
    final IRI subject = getColumnIRI(column);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDFS.SUBCLASSOF, IRI_DATABASE_COLUMN);
    builder.add(subject, RDFS.SUBCLASSOF, IRI_MEASURE_PROPERTY);
    builder.add(subject, RDFS.SUBCLASSOF, OWL.THING);
    if (column.isReference()) {
      builder.add(subject, RDFS.SUBCLASSOF, OWL.OBJECTPROPERTY);
      Table refTable = column.getRefTable().getTable();
      builder.add(subject, RDFS.RANGE, getTableIRI(refTable));
    } else {
      builder.add(subject, RDFS.SUBCLASSOF, OWL.DATATYPEPROPERTY);
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

  private CoreDatatype.XSD columnTypeToXSD(final ColumnType columnType) {
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
   * Write the rows to RDF
   *
   * @param builder the builder to output RDF
   * @param table the table for which to fetch the rows
   * @param rowId optional rowId
   */
  public void rowsToRdf(final ModelBuilder builder, final Table table, final String rowId) {
    final IRI tableIRI = getTableIRI(table);
    for (final Row row : getRows(table, rowId)) {
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

      for (final Column column : table.getMetadata().getColumns()) {
        // Exclude the system columns like mg_insertedBy
        if (column.isSystemColumn()) {
          continue;
        }
        IRI columnIRI = getColumnIRI(column);
        for (final Value value : formatValue(row, column)) {
          builder.add(subject, columnIRI, value);
        }
      }
    }
  }

  private List<Row> getRows(final Table table, final String rowId) {
    Query query = table.query();
    if (rowId != null) {
      if (table.getMetadata().getPrimaryKeyFields().size() > 1) {
        query.where(decodeRowIdToFilter(rowId));
      } else {
        query.where(f(table.getMetadata().getPrimaryKeyColumns().get(0).getName(), EQUALS, rowId));
      }
    }
    // If a table is extended then we get only those rows that are for the base table.
    if (table.getMetadata().getColumnNames().contains("mg_tableclass")) {
      var tableName = table.getSchema().getName() + "." + table.getName();
      query.where(f("mg_tableclass", EQUALS, tableName));
    }
    return query.retrieveRows();
  }

  private Filter decodeRowIdToFilter(final String rowId) {
    try {
      final List<NameValuePair> params =
          URLEncodedUtils.parse(new URI("?" + rowId), StandardCharsets.UTF_8);
      final List<Filter> filters =
          params.stream().map(param -> f(param.getName(), EQUALS, param.getValue())).toList();
      return and(filters);
    } catch (Exception e) {
      throw new MolgenisException("Decode row to filter failed for id " + rowId);
    }
  }

  private IRI getIriForRow(final Row row, final TableMetadata metadata) {
    final String tableName = UrlEscapers.urlPathSegmentEscaper().escape(metadata.getTableName());
    final List<NameValuePair> keyParts = new ArrayList<>();
    for (final Column column : metadata.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String[] values = row.getStringArray(reference.getName());
          for (final String value : values) {
            keyParts.add(new BasicNameValuePair(reference.getName(), value));
          }
        }
      } else {
        keyParts.add(new BasicNameValuePair(column.getName(), row.get(column).toString()));
      }
    }
    final Namespace ns = getSchemaNamespace(metadata.getTable().getSchema());
    if (keyParts.size() == 1) {
      final String value = UrlEscapers.urlPathSegmentEscaper().escape(keyParts.get(0).getValue());
      return Values.iri(ns, tableName + "/" + value);
    } else {
      final String parameters = URLEncodedUtils.format(keyParts, StandardCharsets.UTF_8);
      return Values.iri(ns, tableName + "?" + parameters);
    }
  }

  private List<IRI> getIriValue(final Row row, final Column column) {
    final TableMetadata target = column.getRefTable();
    final String tableName = UrlEscapers.urlPathSegmentEscaper().escape(target.getTableName());
    final Namespace ns = getSchemaNamespace(target.getTable().getSchema());

    final Set<IRI> iris = new HashSet<>();
    final Map<Integer, List<NameValuePair>> items = new HashMap<>();
    for (final Reference reference : column.getReferences()) {
      final String localColumn = reference.getName();
      final String targetColumn = reference.getPath().get(0);
      final String[] values = row.getStringArray(localColumn);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          var keyValuePairs = items.getOrDefault(i, new ArrayList<>());
          keyValuePairs.add(new BasicNameValuePair(targetColumn, values[i]));
          items.put(i, keyValuePairs);
        }
      }
    }

    for (final var item : items.values()) {
      if (item.size() == 1) {
        final String value = UrlEscapers.urlPathSegmentEscaper().escape(item.get(0).getValue());
        iris.add(Values.iri(ns, tableName + "/" + value));
      } else if (item.size() > 1) {
        final String parameters = URLEncodedUtils.format(item, StandardCharsets.UTF_8);
        iris.add(Values.iri(ns, tableName + "?" + parameters));
      }
    }
    return List.copyOf(iris);
  }

  private List<Value> formatValue(final Row row, final Column column) {
    final List<Value> values = new ArrayList<>();
    final ColumnType columnType = column.getColumnType();
    if (columnType.isReference()) {
      values.addAll(getIriValue(row, column));
    } else if (columnType.equals(ColumnType.FILE)) {
      if (row.getString(column.getName() + "_id") != null) {
        final String schemaPath =
            UrlEscapers.urlPathSegmentEscaper().escape(column.getSchemaName());
        final String tablePath = UrlEscapers.urlPathSegmentEscaper().escape(column.getTableName());
        final String columnPath = UrlEscapers.urlPathSegmentEscaper().escape(column.getName());
        values.add(Values.iri(schemaPath + "/api/file/" + tablePath + "/" + columnPath + "/"));
      }
    } else {
      values.addAll(getLiteralValues(row, column));
    }
    return values;
  }

  private List<Value> getLiteralValues(final Row row, final Column column) {
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
