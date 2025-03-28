package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.Constants.API_RDF;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.getCoreDataType;
import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.retrieveValues;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;
import static org.molgenis.emx2.rdf.RdfUtils.getCustomPrefixesOrDefault;
import static org.molgenis.emx2.rdf.RdfUtils.getCustomRdf;
import static org.molgenis.emx2.rdf.RdfUtils.getSchemaNamespace;
import static org.molgenis.emx2.rdf.RdfUtils.getSemanticValue;
import static org.molgenis.emx2.utils.URIUtils.encodeIRI;

import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger logger = LoggerFactory.getLogger(RDFService.class);

  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  public static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final WriterConfig config;
  private final RDFFormat rdfFormat;

  /**
   * The baseURL is the URL at which MOLGENIS is deployed, include protocol and port (if not
   * deviating from the protocol, the port should not be included in the output IRI). This is used
   * because we need to be able to refer to different schemas.
   */
  private final String baseURL;

  public RDFService(final URL baseURL, final RDFFormat rdfFormat) {
    this(baseURL.toString(), rdfFormat);
  }

  /**
   * Construct an RDF Service.
   *
   * @param baseURL the base URL of the MOLGENIS installation
   * @param format the requested RDF document type
   */
  public RDFService(final String baseURL, final RDFFormat format) {
    // REST API URL (and therefore `dcat:endpointURL`) should not have trailing slash
    this.baseURL = formatBaseURL(baseURL);
    this.rdfFormat = format == null ? RDFFormat.TURTLE : format;

    this.config = new WriterConfig();
    // Rio documentation says that it takes a lot of memory for large datasets and should be set to
    // false. Setting this to off brought down the time to download the CatalogueOntologies to
    // seconds.
    this.config.set(BasicWriterSettings.INLINE_BLANK_NODES, false);
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

      if (table == null) {
        describeRoot(builder);
      }

      // Collect all tables present in selected schemas.
      Set<Table> allTables = new HashSet<>();

      // Namespaces per prefix per schema.
      // While ModelBuilder.add() allows for prefixed name, conflicting schema-specific namespaces
      // could cause issues.
      Map<String, Map<String, Namespace>> namespaces = new HashMap<>();

      for (final Schema schema : schemas) {
        processNamespaces(builder, schema, namespaces);
        processCustomRdf(builder, schema);
        if (table == null) describeSchema(builder, schema);
        allTables.addAll(schema.getTablesSorted());
      }

      // Tables to include in output.
      final Set<Table> tables = table != null ? tablesToDescribe(allTables, table) : allTables;

      if (logger.isDebugEnabled()) {
        logger.debug(
            "Tables to show: "
                + tables.stream()
                    .map(
                        i -> i.getMetadata().getSchemaName() + "." + i.getMetadata().getTableName())
                    .toList());
        logger.debug("Namespaces per schema: " + namespaces.toString());
      }

      final RdfMapData rdfMapData = new RdfMapData(baseURL, new OntologyIriMapper(tables));

      for (final Table tableToDescribe : tables) {
        // for full-schema retrieval, don't print the (huge and mostly unused) ontologies
        // of course references to ontologies are still included and are fully retrievable
        if (table == null
            && tableToDescribe.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
          continue;
        }
        if (rowId == null) {
          describeTable(builder, namespaces, tableToDescribe);
          describeColumns(builder, namespaces, tableToDescribe, columnName);
        }
        // if a column name is provided then only provide column metadata, no row values
        if (columnName == null) {
          rowsToRdf(builder, rdfMapData, namespaces, tableToDescribe, rowId);
        }
      }
      Rio.write(builder.build(), outputStream, rdfFormat, config);

    } catch (Exception e) {
      throw new MolgenisException("RDF export failed due to an exception", e);
    }
  }

  private Set<Table> tablesToDescribe(Set<Table> allTables, Table tableFilter) {
    Set<Table> tablesToDescribe = new HashSet<>();
    for (Table currentTable : allTables) {
      processInheritedTable(tableFilter, tablesToDescribe, currentTable);
    }
    return tablesToDescribe;
  }

  private boolean processInheritedTable(
      Table tableFilter, Set<Table> tablesToDescribe, Table currentTable) {
    if (currentTable == null) {
      return false;
    }
    if (currentTable.getSchema().getName().equals(tableFilter.getSchema().getName())
        && currentTable.getName().equals(tableFilter.getName())) {
      tablesToDescribe.add(currentTable);
      return true;
    }
    if (processInheritedTable(tableFilter, tablesToDescribe, currentTable.getInheritedTable())) {
      tablesToDescribe.add(currentTable);
      return true;
    }
    return false;
  }

  private void processNamespaces(
      ModelBuilder builder, Schema schema, Map<String, Map<String, Namespace>> namespaces)
      throws IOException {
    builder.setNamespace(getSchemaNamespace(baseURL, schema));

    Map<String, Namespace> schemaNamespaces = getCustomPrefixesOrDefault(schema);
    schemaNamespaces.values().forEach(builder::setNamespace);
    namespaces.put(schema.getName(), schemaNamespaces);
  }

  private void processCustomRdf(ModelBuilder builder, Schema schema) throws IOException {
    Model model = getCustomRdf(schema);
    if (model != null) {
      // only adds triples, does not transfer defined namespaces!
      model.forEach(e -> builder.add(e.getSubject(), e.getPredicate(), e.getObject()));
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
        .add(RDF.TYPE, BasicIRI.SIO_DATABASE.getIri())
        .add(RDFS.LABEL, "EMX2")
        .add(DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + baseURL)
        .add(DCTERMS.CREATOR, BasicIRI.MOLGENIS.getIri());
  }

  /**
   * Get an IRI for the table. Taking the schema in which the table resides into consideration.
   *
   * @param table the table
   * @return An IRI that is based on the schema namespace.
   */
  private IRI getTableIRI(final Table table) {
    final Namespace ns = getSchemaNamespace(baseURL, table.getSchema());
    return Values.iri(ns, table.getIdentifier());
  }

  private void describeSchema(final ModelBuilder builder, final Schema schema) {
    // The name from a name space is the IRI.
    final String subject = getSchemaNamespace(baseURL, schema).getName();
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
      builder.subject(subject).add(BasicIRI.LDP_CONTAINS.getIri(), object);
    }
  }

  private void describeTable(
      final ModelBuilder builder,
      final Map<String, Map<String, Namespace>> namespaces,
      final Table table) {
    final IRI subject = getTableIRI(table);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDFS.SUBCLASSOF, BasicIRI.LD_DATASET_CLASS.getIri());
    Table parent = table.getInheritedTable();
    // A table is a subclass of owl:Thing or of it's direct parent
    if (parent == null) {
      builder.add(subject, RDFS.SUBCLASSOF, OWL.THING);
    } else {
      builder.add(subject, RDFS.SUBCLASSOF, getTableIRI(parent));
    }
    // Any custom semantics are always added, regardless of table type (DATA/ONTOLOGIES)
    if (table.getMetadata().getSemantics() != null) {
      for (final String tableSemantics : table.getMetadata().getSemantics()) {
        try {
          builder.add(
              subject,
              RDFS.ISDEFINEDBY,
              getSemanticValue(table.getMetadata(), namespaces, tableSemantics));
        } catch (Exception e) {
          throw new MolgenisException(
              "Table annotation '"
                  + tableSemantics
                  + "' for table "
                  + table.getName()
                  + " gives error",
              e);
        }
      }
    }
    // Add 'observing' for any DATA
    if (table.getMetadata().getTableType() == TableType.DATA) {
      builder.add(subject, RDFS.ISDEFINEDBY, BasicIRI.SIO_OBSERVING.getIri());
    }
    // Add 'controlled vocab' and 'concept scheme' for any ONTOLOGIES
    if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.ISDEFINEDBY, BasicIRI.NCIT_CONTROLLED_VOCABULARY.getIri());
      builder.add(subject, RDFS.SUBCLASSOF, SKOS.CONCEPT_SCHEME);
    }
    builder.add(subject, RDFS.LABEL, table.getName());

    if (table.getMetadata().getDescriptions() != null) {
      for (final var entry : table.getMetadata().getDescriptions().entrySet()) {
        builder.add(subject, DCTERMS.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
      }
    }
  }

  private void describeColumns(
      final ModelBuilder builder,
      final Map<String, Map<String, Namespace>> namespaces,
      final Table table,
      final String columnName) {
    if (table.getMetadata().getTableType() == TableType.DATA) {
      for (final Column column : table.getMetadata().getNonInheritedColumns()) {
        // Exclude the system columns that refer to specific users
        if (column.isSystemAddUpdateByUserColumn()) {
          continue;
        }
        if (columnName == null || columnName.equals(column.getName())) {
          describeColumn(builder, namespaces, column);
        }
      }
    } else {
      // For ontology tables we don't define the columns as predicates.
    }
  }

  private IRI getColumnIRI(final Column column) {
    TableMetadata table = column.getTable();
    Schema schema = table.getTable().getSchema();
    final String tableName = UrlEscapers.urlPathSegmentEscaper().escape(table.getIdentifier());
    final String columnName = UrlEscapers.urlPathSegmentEscaper().escape(column.getIdentifier());
    final Namespace ns = getSchemaNamespace(baseURL, schema);
    return Values.iri(ns, tableName + "/column/" + columnName);
  }

  private void describeColumn(
      final ModelBuilder builder,
      final Map<String, Map<String, Namespace>> namespaces,
      final Column column) {
    final IRI subject = getColumnIRI(column);
    if (column.isReference()) {
      builder.add(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      Table refTable = column.getRefTable().getTable();
      builder.add(subject, RDFS.RANGE, getTableIRI(refTable));
    } else {
      var type = column.getColumnType();
      if (type == ColumnType.HYPERLINK || type == ColumnType.HYPERLINK_ARRAY) {
        builder.add(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      } else {
        builder.add(subject, RDF.TYPE, OWL.DATATYPEPROPERTY);
        builder.add(subject, RDFS.RANGE, getCoreDataType(column));
      }
    }
    builder.add(subject, RDFS.LABEL, column.getName());
    builder.add(subject, RDFS.DOMAIN, getTableIRI(column.getTable().getTable()));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = BasicIRI.SIO_IDENTIFIER.getIri().stringValue();
        }
        try {
          builder.add(
              subject,
              RDFS.ISDEFINEDBY,
              getSemanticValue(column.getTable(), namespaces, columnSemantics));
        } catch (Exception e) {
          throw new MolgenisException(
              "Semantic tag '"
                  + columnSemantics
                  + "' for column "
                  + column.getTableName()
                  + "."
                  + column.getName()
                  + " gives error",
              e);
        }
      }
    }
    if (column.getDescriptions() != null) {
      for (var entry : column.getDescriptions().entrySet()) {
        builder.add(subject, DC.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
      }
    }
  }

  /**
   * Write the rows to RDF
   *
   * @param builder the builder to output RDF
   * @param table the table for which to fetch the rows
   * @param rowId optional rowId
   */
  public void rowsToRdf(
      final ModelBuilder builder,
      final RdfMapData rdfMapData,
      final Map<String, Map<String, Namespace>> namespaces,
      final Table table,
      final String rowId) {
    final IRI tableIRI = getTableIRI(table);
    final List<Row> rows = getRows(table, rowId);
    switch (table.getMetadata().getTableType()) {
      case ONTOLOGIES ->
          rows.forEach(
              row ->
                  ontologyRowToRdf(
                      builder, rdfMapData, table, tableIRI, row, getIriForRow(row, table)));
      case DATA ->
          rows.forEach(
              row ->
                  dataRowToRdf(
                      builder,
                      rdfMapData,
                      namespaces,
                      table,
                      tableIRI,
                      row,
                      rowId,
                      getIriForRow(row, table)));
      default -> throw new MolgenisException("Cannot convert unsupported TableType to RDF");
    }
  }

  private void ontologyRowToRdf(
      final ModelBuilder builder,
      final RdfMapData rdfMapData,
      final Table table,
      final IRI tableIRI,
      final Row row,
      final IRI subject) {
    builder.add(subject, RDF.TYPE, BasicIRI.NCIT_CODED_VALUE_DATA_TYPE.getIri());
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDF.TYPE, SKOS.CONCEPT);
    builder.add(subject, RDFS.SUBCLASSOF, tableIRI);
    builder.add(subject, SKOS.IN_SCHEME, tableIRI);
    if (row.getString("name") != null) {
      builder.add(subject, RDFS.LABEL, Values.literal(row.getString("name")));
      builder.add(subject, SKOS.PREF_LABEL, Values.literal(row.getString("name")));
    }
    if (row.getString("label") != null) {
      builder.add(subject, RDFS.LABEL, Values.literal(row.getString("label")));
      builder.add(subject, SKOS.ALT_LABEL, Values.literal(row.getString("name")));
    }
    if (row.getString("code") != null) {
      builder.add(subject, SKOS.NOTATION, Values.literal(row.getString("code")));
    }
    if (row.getString("codesystem") != null) {
      builder.add(
          subject,
          BasicIRI.NCIT_CONTROLLED_VOCABULARY.getIri(),
          Values.literal(row.getString("codesystem")));
    }
    if (row.getString("definition") != null) {
      builder.add(subject, SKOS.DEFINITION, Values.literal(row.getString("definition")));
    }
    if (row.getString(ONTOLOGY_TERM_URI) != null) {
      builder.add(subject, OWL.SAMEAS, Values.iri(row.getString(ONTOLOGY_TERM_URI)));
    }
    if (row.getString("parent") != null) {
      Set<Value> parents = retrieveValues(rdfMapData, row, table.getMetadata().getColumn("parent"));
      for (var parent : parents) {
        builder.add(subject, RDFS.SUBCLASSOF, parent);
      }
    }
  }

  private void dataRowToRdf(
      final ModelBuilder builder,
      final RdfMapData rdfMapData,
      final Map<String, Map<String, Namespace>> namespaces,
      final Table table,
      final IRI tableIRI,
      final Row row,
      final String rowId,
      final IRI subject) {
    builder.add(subject, RDF.TYPE, tableIRI);
    builder.add(subject, RDF.TYPE, BasicIRI.LD_OBSERVATION.getIri());
    builder.add(
        subject,
        BasicIRI.DCAT_ENDPOINTURL.getIri(),
        encodeIRI(baseURL + "/" + table.getSchema().getName() + API_RDF));
    builder.add(subject, BasicIRI.FDP_METADATAIDENTIFIER.getIri(), subject);
    if (table.getMetadata().getSemantics() != null) {
      for (String semantics : table.getMetadata().getSemantics()) {
        builder.add(
            subject, RDF.TYPE, getSemanticValue(table.getMetadata(), namespaces, semantics));
      }
    }
    builder.add(subject, BasicIRI.LD_DATASET_PREDICATE.getIri(), tableIRI);
    builder.add(subject, RDFS.LABEL, Values.literal(getLabelForRow(row, table.getMetadata())));
    for (final Column column : table.getMetadata().getColumns()) {
      // Exclude the system columns that refer to specific users
      if (column.isSystemAddUpdateByUserColumn()) {
        continue;
      }
      IRI columnIRI = getColumnIRI(column);

      // Non-default behaviour for non-semantic values to ontology table.
      if (column.getColumnType().equals(ColumnType.ONTOLOGY)
          || column.getColumnType().equals(ColumnType.ONTOLOGY_ARRAY)) {
        retrieveValues(rdfMapData, row, column, ColumnTypeRdfMapper.RdfColumnType.REFERENCE)
            .forEach(value -> builder.add(subject, columnIRI, value));
      }

      for (final Value value : retrieveValues(rdfMapData, row, column)) {
        if (column.getSemantics() != null) {
          for (String semantics : column.getSemantics()) {
            builder.add(
                subject.stringValue(),
                getSemanticValue(table.getMetadata(), namespaces, semantics),
                value);
          }
        }

        switch (column.getColumnType()) {
          case ONTOLOGY, ONTOLOGY_ARRAY -> {} // skipped due to custom behaviour above
          case HYPERLINK, HYPERLINK_ARRAY -> {
            builder.add(subject, columnIRI, value);
            var resource = Values.iri(value.stringValue());
            builder.add(resource, RDFS.LABEL, Values.literal(value.stringValue()));
          }
          case FILE -> {
            builder.add(subject, columnIRI, value);
            IRI fileSubject = (IRI) value;
            builder.add(fileSubject, RDF.TYPE, BasicIRI.SIO_FILE.getIri());
            builder.add(
                fileSubject,
                DCTERMS.TITLE,
                Values.literal(row.getString(column.getName() + "_filename")));
            builder.add(
                fileSubject,
                DCTERMS.FORMAT,
                Values.iri(
                    "http://www.iana.org/assignments/media-types/"
                        + row.getString(column.getName() + "_mimetype")));
          }
          default -> {
            builder.add(subject, columnIRI, value);
          }
        }
      }
    }
  }

  private String getLabelForRow(final Row row, final TableMetadata metadata) {
    List<String> primaryKeyValues = new ArrayList<>();
    for (Column column : metadata.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String value = row.getString(reference.getName());
          primaryKeyValues.add(value);
        }
      } else {
        primaryKeyValues.add(row.getString(column.getName()));
      }
    }
    return String.join(" ", primaryKeyValues);
  }

  private List<Row> getRows(Table table, final String rowId) {
    Query query = table.query();

    if (rowId != null) {
      PrimaryKey key = PrimaryKey.makePrimaryKeyFromEncodedKey(rowId);
      query.where(key.getFilter());
    }

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      var tableName = table.getSchema().getName() + "." + table.getName();
      query.where(f("mg_tableclass", EQUALS, tableName));
    }
    return query.retrieveRows();
  }

  private IRI getIriForRow(final Row row, final Table table) {
    return getIriForRow(row, table.getMetadata());
  }

  private IRI getIriForRow(final Row row, final TableMetadata metadata) {
    final String rootTableName =
        UrlEscapers.urlPathSegmentEscaper().escape(metadata.getRootTable().getIdentifier());
    final Map<String, String> keyParts = new LinkedHashMap<>();
    for (final Column column : metadata.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (final Reference reference : column.getReferences()) {
          final String[] values = row.getStringArray(reference.getName());
          for (final String value : values) {
            keyParts.put(reference.getName(), value);
          }
        }
      } else {
        keyParts.put(column.getName(), row.get(column).toString());
      }
    }
    final Namespace ns = getSchemaNamespace(baseURL, metadata.getRootTable().getSchema());
    PrimaryKey key = new PrimaryKey(keyParts);
    return encodeIRI(ns.getName() + rootTableName + "?" + key.getEncodedValue());
  }
}
