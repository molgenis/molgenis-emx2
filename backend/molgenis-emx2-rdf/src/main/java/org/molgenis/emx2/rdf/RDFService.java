package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.rdf.RdfUtils.getSchemaNamespace;

import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.commons.io.IOUtils;
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
  private static final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  public static final IRI LDP_CONTAINS = Values.iri("http://www.w3.org/ns/ldp#contains");
  public static final IRI IRI_DATABASE_TABLE =
      Values.iri("http://semanticscience.org/resource/SIO_000754");
  public static final IRI IRI_DATASET_CLASS =
      Values.iri("http://purl.org/linked-data/cube#DataSet");
  public static final IRI IRI_DATASET_PREDICATE =
      Values.iri("http://purl.org/linked-data/cube#dataSet");
  public static final IRI IRI_CONTROLLED_VOCABULARY =
      Values.iri("http://purl.obolibrary.org/obo/NCIT_C48697");

  // Advanced setting containing valid Turtle-formatted RDF.
  public static final String SETTING_CUSTOM_RDF = "custom_rdf";

  /**
   * SIO:001055 = observing (definition: observing is a process of passive interaction in which one
   * entity makes note of attributes of one or more entities)
   */
  public static final IRI IRI_OBSERVING =
      Values.iri("http://semanticscience.org/resource/SIO_001055");

  public static final String SEMANTICS_ID_URL_STRING =
      "http://semanticscience.org/resource/SIO_000115";
  public static final IRI IRI_OBSERVATION =
      Values.iri("http://purl.org/linked-data/cube#Observation");

  /** NCIT:C95637 = Coded Value Data Type */
  public static final IRI IRI_CODED_VALUE_DATATYPE =
      Values.iri("http://purl.obolibrary.org/obo/NCIT_C95637");

  /** SIO:SIO_000750 = database */
  public static final IRI IRI_DATABASE =
      Values.iri("http://semanticscience.org/resource/SIO_000750");

  /** SIO:SIO_000396 = file */
  public static final IRI IRI_FILE = Values.iri("http://semanticscience.org/resource/SIO_000396");

  public static final IRI IRI_MOLGENIS = Values.iri("https://molgenis.org");
  public static final String ONTOLOGY_TERM_URI = "ontologyTermURI";

  private final WriterConfig config;
  private final RDFFormat rdfFormat;
  private final ColumnTypeRdfMapper mapper;

  /**
   * The baseURL is the URL at which MOLGENIS is deployed, include protocol and port (if deviating
   * from the protocol default port). This is used because we need to be able to refer to different
   * schemas.
   */
  private final String baseURL;

  /**
   * Construct an RDF Service.
   *
   * @param baseURL the base URL of the MOLGENIS installation
   * @param rdfAPIPath the path fragment for the RDF service within a Schema
   * @param format the requested RDF document type
   */
  public RDFService(final String baseURL, final String rdfAPIPath, final RDFFormat format) {
    // Ensure that the base URL has a trailing "/" so we can use it easily to
    // construct URL paths.
    String baseUrlTrim = baseURL.trim();
    this.baseURL = baseUrlTrim.endsWith("/") ? baseUrlTrim : baseUrlTrim + "/";
    this.mapper = new ColumnTypeRdfMapper(this.baseURL);
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

      // (custom) namespaces & custom rdf
      buildgenericRdf(builder, schemas);

      if (table == null) {
        describeRoot(builder);
      }

      // Collect all tables present in selected schemas.
      Set<Table> allTables = new HashSet<>();

      for (final Schema schema : schemas) {
        if (table == null) {
          describeSchema(builder, schema);
        }
        allTables.addAll(schema.getTablesSorted());
      }

      final Set<Table> tables = table != null ? tablesToDescribe(allTables, table) : allTables;
      for (final Table tableToDescribe : tables) {
        // for full-schema retrieval, don't print the (huge and mostly unused) ontologies
        // of course references to ontologies are still included and are fully retrievable
        if (table == null
            && tableToDescribe.getMetadata().getTableType().equals(TableType.ONTOLOGIES)) {
          continue;
        }
        if (rowId == null) {
          describeTable(builder, tableToDescribe);
          describeColumns(builder, tableToDescribe, columnName);
        }
        // if a column name is provided then only provide column metadata, no row values
        if (columnName == null) {
          rowsToRdf(builder, tableToDescribe, rowId);
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

  /**
   * Processes all schemas for custom namespaces/RDF.
   *
   * @param builder
   * @param schemas
   * @throws IOException
   */
  private void buildgenericRdf(ModelBuilder builder, Schema[] schemas) throws IOException {
    // Defines if all used schemas have a custom_rdf setting.
    boolean allIncludeCustomRdf = true;
    // Define the schemas at the start of the document.
    for (final Schema schema : schemas) {
      final Namespace ns = getSchemaNamespace(baseURL, schema);
      builder.setNamespace(ns);
      // Adds custom RDF to model.
      if (schema.hasSetting(SETTING_CUSTOM_RDF)) {
        addModelToBuilder(
            builder,
            Rio.parse(
                IOUtils.toInputStream(
                    schema.getSettingValue(SETTING_CUSTOM_RDF), StandardCharsets.UTF_8),
                RDFFormat.TURTLE));
      } else {
        allIncludeCustomRdf = false;
      }
    }

    // If any of the used schemas do not have custom_rdf set, adds the default ones.
    if (!allIncludeCustomRdf) {
      DefaultNamespace.streamAll().forEach(builder::setNamespace);
    }
  }

  private void addModelToBuilder(ModelBuilder builder, Model model) {
    model.getNamespaces().forEach(builder::setNamespace); // namespaces
    model.forEach(e -> builder.add(e.getSubject(), e.getPredicate(), e.getObject())); // triples
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
        .add(RDF.TYPE, IRI_DATABASE)
        .add(RDFS.LABEL, "EMX2")
        .add(DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + baseURL)
        .add(DCTERMS.CREATOR, IRI_MOLGENIS);
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
      builder.subject(subject).add(LDP_CONTAINS, object);
    }
  }

  private void describeTable(final ModelBuilder builder, final Table table) {
    final IRI subject = getTableIRI(table);
    builder.add(subject, RDF.TYPE, OWL.CLASS);
    builder.add(subject, RDFS.SUBCLASSOF, IRI_DATASET_CLASS);
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
          builder.add(subject, RDFS.ISDEFINEDBY, iri(tableSemantics));
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
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_OBSERVING);
    }
    // Add 'controlled vocab' and 'concept scheme' for any ONTOLOGIES
    if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      builder.add(subject, RDFS.ISDEFINEDBY, IRI_CONTROLLED_VOCABULARY);
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
      final ModelBuilder builder, final Table table, final String columnName) {
    if (table.getMetadata().getTableType() == TableType.DATA) {
      for (final Column column : table.getMetadata().getNonInheritedColumns()) {
        // Exclude the system columns that refer to specific users
        if (column.isSystemAddUpdateByUserColumn()) {
          continue;
        }
        if (columnName == null || columnName.equals(column.getName())) {
          describeColumn(builder, column);
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

  private void describeColumn(final ModelBuilder builder, final Column column) {
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
        builder.add(subject, RDFS.RANGE, ColumnTypeRdfMapper.getCoreDataType(column));
      }
    }
    builder.add(subject, RDFS.LABEL, column.getName());
    builder.add(subject, RDFS.DOMAIN, getTableIRI(column.getTable().getTable()));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        if (columnSemantics.equals("id")) {
          // todo: need to figure out how to better handle 'id' tagging
          columnSemantics = SEMANTICS_ID_URL_STRING;
        }
        try {
          builder.add(subject, RDFS.ISDEFINEDBY, iri(columnSemantics));
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
  public void rowsToRdf(final ModelBuilder builder, final Table table, final String rowId) {
    final IRI tableIRI = getTableIRI(table);
    final List<Row> rows = getRows(table, rowId);
    switch (table.getMetadata().getTableType()) {
      case ONTOLOGIES ->
          rows.forEach(
              row -> ontologyRowToRdf(builder, table, tableIRI, row, getIriForRow(row, table)));
      case DATA ->
          rows.forEach(
              row -> dataRowToRdf(builder, table, tableIRI, row, rowId, getIriForRow(row, table)));
      default -> throw new MolgenisException("Cannot convert unsupported TableType to RDF");
    }
  }

  private void ontologyRowToRdf(
      final ModelBuilder builder,
      final Table table,
      final IRI tableIRI,
      final Row row,
      final IRI subject) {
    builder.add(subject, RDF.TYPE, IRI_CODED_VALUE_DATATYPE);
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
      builder.add(subject, IRI_CONTROLLED_VOCABULARY, Values.literal(row.getString("codesystem")));
    }
    if (row.getString("definition") != null) {
      builder.add(subject, SKOS.DEFINITION, Values.literal(row.getString("definition")));
    }
    if (row.getString(ONTOLOGY_TERM_URI) != null) {
      builder.add(subject, OWL.SAMEAS, Values.iri(row.getString(ONTOLOGY_TERM_URI)));
    }
    if (row.getString("parent") != null) {
      Set<Value> parents = mapper.retrieveValues(row, table.getMetadata().getColumn("parent"));
      for (var parent : parents) {
        builder.add(subject, RDFS.SUBCLASSOF, parent);
      }
    }
  }

  private void dataRowToRdf(
      final ModelBuilder builder,
      Table table,
      final IRI tableIRI,
      final Row row,
      final String rowId,
      final IRI subject) {
    builder.add(subject, RDF.TYPE, tableIRI);
    builder.add(subject, RDF.TYPE, IRI_OBSERVATION);
    if (table.getMetadata().getSemantics() != null) {
      for (String semantics : table.getMetadata().getSemantics()) {
        builder.add(subject, RDF.TYPE, iri(semantics));
      }
    }
    builder.add(subject, IRI_DATASET_PREDICATE, tableIRI);
    builder.add(subject, RDFS.LABEL, Values.literal(getLabelForRow(row, table.getMetadata())));
    // via rowId might be subclass
    if (rowId != null) {
      // because row IRI point to root tables we need to find actual subclass table to ensure we
      // get all columns
      table = getSubclassTableForRowBasedOnMgTableclass(table, row);
    }
    for (final Column column : table.getMetadata().getColumns()) {
      // Exclude the system columns that refer to specific users
      if (column.isSystemAddUpdateByUserColumn()) {
        continue;
      }
      IRI columnIRI = getColumnIRI(column);
      for (final Value value : mapper.retrieveValues(row, column)) {
        if (column.getSemantics() != null) {
          for (String semantics : column.getSemantics()) {
            builder.add(subject.stringValue(), semantics, value);
            //                builder.add(
            //                    // subject, Values.iri(semantics), value);
            //                    subject, Values.iri(semantics.split(":")[0],
            // semantics.split(":")[1]), value);
          }
        }
        builder.add(subject, columnIRI, value);
        if (column.getColumnType().equals(ColumnType.HYPERLINK)
            || column.getColumnType().equals(ColumnType.HYPERLINK_ARRAY)) {
          var resource = Values.iri(value.stringValue());
          builder.add(resource, RDFS.LABEL, Values.literal(value.stringValue()));
        }
        // Adds file metadata.
        if (column.isFile()) {
          IRI fileSubject = (IRI) value;
          builder.add(fileSubject, RDF.TYPE, IRI_FILE);
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
      }
    }
  }

  private static Table getSubclassTableForRowBasedOnMgTableclass(Table table, Row row) {
    if (row.getString(MG_TABLECLASS) != null) {
      table =
          table
              .getSchema()
              .getDatabase()
              .getSchema(row.getSchemaName())
              .getTable(row.getTableName());
    }
    return table;
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

    List<SelectColumn> selectColumns = new ArrayList<>();
    for (Column c : table.getMetadata().getColumns()) {
      if (c.isFile()) {
        selectColumns.add(s(c.getName(), s("id"), s("filename"), s("mimetype")));
      } else if (c.isRef() || c.isRefArray()) {
        c.getReferences().forEach(i -> selectColumns.add(s(i.getName())));
      } else if (c.isRefback()) {
        selectColumns.add(refBackSelect(c));
      } else {
        selectColumns.add(s(c.getName()));
      }
    }
    SelectColumn[] selectArray = selectColumns.toArray(SelectColumn[]::new);

    if (rowId != null) {
      // first find from root table
      PrimaryKey key = PrimaryKey.makePrimaryKeyFromEncodedKey(rowId);
      List<Row> oneRow = query.select(selectArray).where(key.getFilter()).retrieveRows();
      // if subclass
      if (oneRow.size() == 1 && oneRow.get(0).getString(MG_TABLECLASS) != null) {
        Row row = oneRow.get(0);
        table = getSubclassTableForRowBasedOnMgTableclass(table, row);
        return table.query().select(selectArray).where(key.getFilter()).retrieveRows();
      }
      return oneRow;
    } else {
      if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
        var tableName = table.getSchema().getName() + "." + table.getName();
        query.where(f("mg_tableclass", EQUALS, tableName));
      }
      return query.select(selectArray).retrieveRows();
    }
  }

  private SelectColumn refBackSelect(Column column) {
    List<SelectColumn> subSelects = new ArrayList<>();
    for (Column subColumn : column.getRefTable().getPrimaryKeyColumns()) {
      if (subColumn.isRef() || subColumn.isRefArray()) {
        subSelects.add(refBackSelect(subColumn));
      } else {
        subSelects.add(s(subColumn.getName()));
      }
    }
    return s(column.getName(), subSelects.toArray(SelectColumn[]::new));
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
        keyParts.put(column.getIdentifier(), row.get(column).toString());
      }
    }
    final Namespace ns = getSchemaNamespace(baseURL, metadata.getRootTable().getSchema());
    PrimaryKey key = new PrimaryKey(keyParts);
    return Values.iri(ns, rootTableName + "?" + key.getEncodedValue());
  }
}
