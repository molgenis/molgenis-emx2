package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.getCoreDataType;
import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.retrieveValues;
import static org.molgenis.emx2.rdf.IriGenerator.columnIRI;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;
import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;
import static org.molgenis.emx2.rdf.IriGenerator.tableIRI;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;
import static org.molgenis.emx2.rdf.RdfUtils.getCustomRdf;
import static org.molgenis.emx2.rdf.RdfUtils.getSchemaNamespace;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
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
  public static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final WriterConfig config;
  private final RDFFormat rdfFormat;

  /**
   * The baseURL is the URL at which MOLGENIS is deployed, include protocol and port (if not
   * deviating from the protocol, the port should not be included in the output IRI). This is used
   * because we need to be able to refer to different schemas.
   */
  private final String baseURL;

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

      for (final Schema schema : schemas) {
        addSchemaNamespace(builder, schema);
        processCustomRdf(builder, schema);
        if (table == null) describeSchema(builder, schema);
        allTables.addAll(schema.getTablesSorted());
      }

      // Tables to include in output.
      final Set<Table> tables = table != null ? tablesToDescribe(allTables, table) : allTables;

      final RdfMapData rdfMapData =
          new RdfMapData(
              baseURL,
              new NamespaceMapper(Arrays.stream(schemas).toList()),
              new OntologyIriMapper(tables));

      rdfMapData.getNamespaceMapper().getAllNamespaces().forEach(builder::setNamespace);

      if (logger.isDebugEnabled()) {
        logger.debug(
            "Tables to show: "
                + tables.stream()
                    .map(
                        i -> i.getMetadata().getSchemaName() + "." + i.getMetadata().getTableName())
                    .toList());
        logger.debug("Namespaces per schema: " + rdfMapData.getNamespaceMapper());
      }

      for (final Table tableToDescribe : tables) {
        // for full-schema retrieval, don't print the (huge and mostly unused) ontologies
        // of course references to ontologies are still included and are fully retrievable
        if (table == null
            && tableToDescribe.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
          continue;
        }
        if (rowId == null) {
          describeTable(builder, rdfMapData.getNamespaceMapper(), tableToDescribe);
          describeColumns(builder, rdfMapData.getNamespaceMapper(), tableToDescribe, columnName);
        }
        // if a column name is provided then only provide column metadata, no row values
        if (columnName == null) {
          rowsToRdf(builder, rdfMapData, tableToDescribe, rowId);
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

  private void addSchemaNamespace(ModelBuilder builder, Schema schema) throws IOException {
    builder.setNamespace(getSchemaNamespace(baseURL, schema));
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
        .add(RDF.TYPE, BasicIRI.SIO_DATABASE)
        .add(RDFS.LABEL, "EMX2")
        .add(DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + baseURL)
        .add(DCTERMS.CREATOR, BasicIRI.MOLGENIS);
  }

  private void describeSchema(final ModelBuilder builder, final Schema schema) {
    final IRI subject = schemaIRI(baseURL, schema);
    builder
        .subject(subject)
        .add(RDFS.LABEL, schema.getName())
        .add(DCTERMS.IS_PART_OF, Values.iri(baseURL))
        .add(subject, RDF.TYPE, RDFS.CONTAINER);
    if (schema.getMetadata().getDescription() != null) {
      builder.subject(subject).add(DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    for (final Table table : schema.getTablesSorted()) {
      builder.subject(subject).add(LDP.CONTAINS, tableIRI(baseURL, table));
    }
  }

  private void describeTable(
      final ModelBuilder builder, final NamespaceMapper mapper, final Table table) {
    final IRI subject = tableIRI(baseURL, table);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDFS.SUBCLASSOF, BasicIRI.LD_DATASET_CLASS);
    Table parent = table.getInheritedTable();
    // A table is a subclass of owl:Thing or of it's direct parent
    if (parent == null) {
      builder.add(subject, RDFS.SUBCLASSOF, OWL.THING);
    } else {
      builder.add(subject, RDFS.SUBCLASSOF, tableIRI(baseURL, parent));
    }
    // Any custom semantics are always added, regardless of table type (DATA/ONTOLOGIES)
    if (table.getMetadata().getSemantics() != null) {
      for (final String tableSemantics : table.getMetadata().getSemantics()) {
        try {
          builder.add(subject, RDFS.ISDEFINEDBY, mapper.map(table.getSchema(), tableSemantics));
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
      builder.add(subject, RDFS.ISDEFINEDBY, BasicIRI.SIO_OBSERVING);
    }
    // Add 'controlled vocab' and 'concept scheme' for any ONTOLOGIES
    if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.ISDEFINEDBY, BasicIRI.NCIT_CONTROLLED_VOCABULARY);
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
      final NamespaceMapper mapper,
      final Table table,
      final String columnName) {
    if (table.getMetadata().getTableType() == TableType.DATA) {
      for (final Column column : table.getMetadata().getNonInheritedColumns()) {
        // Exclude the system columns that refer to specific users
        if (column.isSystemAddUpdateByUserColumn()) {
          continue;
        }
        if (columnName == null || columnName.equals(column.getName())) {
          describeColumn(builder, mapper, column);
        }
      }
    } else {
      // For ontology tables we don't define the columns as predicates.
    }
  }

  private void describeColumn(
      final ModelBuilder builder, final NamespaceMapper mapper, final Column column) {
    final IRI subject = columnIRI(baseURL, column);
    if (column.isReference()) {
      builder.add(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      builder.add(subject, RDFS.RANGE, tableIRI(baseURL, column.getRefTable()));
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
    builder.add(subject, RDFS.DOMAIN, tableIRI(baseURL, column.getTable()));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = BasicIRI.SIO_IDENTIFIER.stringValue();
        }
        try {
          builder.add(subject, RDFS.ISDEFINEDBY, mapper.map(column.getSchema(), columnSemantics));
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
      final Table table,
      final String rowId) {
    final IRI tableIRI = tableIRI(baseURL, table);
    final List<Row> rows = getRows(table, rowId);
    switch (table.getMetadata().getTableType()) {
      case ONTOLOGIES -> rows.forEach(row -> ontologyRowToRdf(builder, rdfMapData, table, row));
      case DATA -> rows.forEach(row -> dataRowToRdf(builder, rdfMapData, table, row));
      default -> throw new MolgenisException("Cannot convert unsupported TableType to RDF");
    }
  }

  private void ontologyRowToRdf(
      final ModelBuilder builder, final RdfMapData rdfMapData, final Table table, final Row row) {
    final IRI tableIRI = tableIRI(baseURL, table);
    final IRI subject = rowIRI(rdfMapData.getBaseURL(), table, row);

    builder.add(subject, RDF.TYPE, BasicIRI.NCIT_CODED_VALUE_DATA_TYPE);
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
          BasicIRI.NCIT_CONTROLLED_VOCABULARY,
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
      final ModelBuilder builder, final RdfMapData rdfMapData, final Table table, final Row row) {
    final IRI tableIRI = tableIRI(baseURL, table);
    final IRI subject = rowIRI(rdfMapData.getBaseURL(), table, row);

    builder.add(subject, RDF.TYPE, tableIRI);
    builder.add(subject, RDF.TYPE, BasicIRI.LD_OBSERVATION);
    builder.add(subject, DCAT.ENDPOINT_URL, schemaIRI(baseURL, table.getSchema()));
    builder.add(subject, BasicIRI.FDP_METADATAIDENTIFIER, subject);
    if (table.getMetadata().getSemantics() != null) {
      for (String semantics : table.getMetadata().getSemantics()) {
        builder.add(
            subject, RDF.TYPE, rdfMapData.getNamespaceMapper().map(table.getSchema(), semantics));
      }
    }
    builder.add(subject, BasicIRI.LD_DATASET_PREDICATE, tableIRI);
    builder.add(subject, RDFS.LABEL, Values.literal(getLabelForRow(row, table.getMetadata())));
    for (final Column column : table.getMetadata().getColumns()) {
      // Exclude the system columns that refer to specific users
      if (column.isSystemAddUpdateByUserColumn()) {
        continue;
      }
      IRI columnIRI = columnIRI(baseURL, column);

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
                rdfMapData.getNamespaceMapper().map(table.getSchema(), semantics),
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
            builder.add(fileSubject, RDF.TYPE, BasicIRI.SIO_FILE);
            Literal fileName = Values.literal(row.getString(column.getName() + "_filename"));
            builder.add(fileSubject, RDFS.LABEL, fileName);
            builder.add(fileSubject, DCTERMS.TITLE, fileName);
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
      PrimaryKey key = PrimaryKey.fromEncodedString(table, rowId);
      query.where(key.getFilter());
    }

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      var tableName = table.getSchema().getName() + "." + table.getName();
      query.where(f("mg_tableclass", EQUALS, tableName));
    }
    return query.retrieveRows();
  }
}
