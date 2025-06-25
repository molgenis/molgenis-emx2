package org.molgenis.emx2.rdf.generators;

import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.getCoreDataType;
import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.retrieveValues;
import static org.molgenis.emx2.rdf.IriGenerator.columnIRI;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;
import static org.molgenis.emx2.rdf.IriGenerator.schemaIRI;
import static org.molgenis.emx2.rdf.IriGenerator.tableIRI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.BasicIRI;
import org.molgenis.emx2.rdf.ColumnTypeRdfMapper;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class Emx2RdfGenerator extends RdfRowsGenerator {
  public Emx2RdfGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  @Override
  public void generate(Schema schema) {
    List<Table> tables = schema.getTablesSorted();
    RdfMapData rdfMapData = new RdfMapData(getBaseURL(), new OntologyIriMapper(tables));
    NamespaceMapper namespaces = new NamespaceMapper(getBaseURL(), schema);

    generatePrefixes(namespaces.getAllNamespaces(schema));
    generateCustomRdf(schema);
    describeRoot();
    describeSchema(schema);
    tables.forEach(i -> describeTable(namespaces, i));
    tables.forEach(i -> describeColumns(namespaces, i, null));
    tables.forEach(i -> processRows(namespaces, rdfMapData, i, null));
  }

  @Override
  public void generate(Table table) {
    Set<Table> tables = tablesToDescribe(table.getSchema(), table);
    RdfMapData rdfMapData = new RdfMapData(getBaseURL(), new OntologyIriMapper(tables));
    NamespaceMapper namespaces = new NamespaceMapper(getBaseURL(), table.getSchema());

    generatePrefixes(namespaces.getAllNamespaces(table.getSchema()));
    generateCustomRdf(table.getSchema());
    describeTable(namespaces, table);
    describeColumns(namespaces, table, null);
    tables.forEach(i -> processRows(namespaces, rdfMapData, i, null));
  }

  @Override
  public void generate(Table table, Column column) {
    NamespaceMapper namespaces = new NamespaceMapper(getBaseURL(), table.getSchema());

    generatePrefixes(namespaces.getAllNamespaces(table.getSchema()));
    describeTable(namespaces, table);
    describeColumns(namespaces, table, column.getName());
  }

  void describeSchema(final Schema schema) {
    final IRI subject = schemaIRI(getBaseURL(), schema);

    getWriter().processTriple(subject, RDFS.LABEL, Values.literal(schema.getName()));
    getWriter().processTriple(subject, DCTERMS.IS_PART_OF, Values.iri(getBaseURL()));
    getWriter().processTriple(subject, RDF.TYPE, RDFS.CONTAINER);
    if (schema.getMetadata().getDescription() != null
        && !schema.getMetadata().getDescription().isEmpty()) {
      getWriter()
          .processTriple(
              subject, DCTERMS.DESCRIPTION, Values.literal(schema.getMetadata().getDescription()));
    }
    for (final Table table : schema.getTablesSorted()) {
      getWriter().processTriple(subject, LDP.CONTAINS, tableIRI(getBaseURL(), table));
    }
  }

  void describeTable(final NamespaceMapper namespaces, final Table table) {
    final IRI subject = tableIRI(getBaseURL(), table);
    getWriter().processTriple(subject, RDF.TYPE, OWL.CLASS);
    getWriter().processTriple(subject, RDFS.SUBCLASSOF, BasicIRI.LD_DATASET_CLASS);
    Table parent = table.getInheritedTable();
    // A table is a subclass of owl:Thing or of it's direct parent
    if (parent == null) {
      getWriter().processTriple(subject, RDFS.SUBCLASSOF, OWL.THING);
    } else {
      getWriter().processTriple(subject, RDFS.SUBCLASSOF, tableIRI(getBaseURL(), parent));
    }
    // Any custom semantics are always added, regardless of table type (DATA/ONTOLOGIES)
    if (table.getMetadata().getSemantics() != null) {
      for (final String tableSemantics : table.getMetadata().getSemantics()) {
        try {
          getWriter()
              .processTriple(
                  subject, RDFS.ISDEFINEDBY, namespaces.map(table.getSchema(), tableSemantics));
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
      getWriter().processTriple(subject, RDFS.ISDEFINEDBY, BasicIRI.SIO_OBSERVING);
    }
    // Add 'controlled vocab' and 'concept scheme' for any ONTOLOGIES
    if (table.getMetadata().getTableType() == TableType.ONTOLOGIES) {
      getWriter().processTriple(subject, RDFS.ISDEFINEDBY, BasicIRI.NCIT_CONTROLLED_VOCABULARY);
      getWriter().processTriple(subject, RDFS.SUBCLASSOF, SKOS.CONCEPT_SCHEME);
    }
    getWriter().processTriple(subject, RDFS.LABEL, Values.literal(table.getName()));

    if (table.getMetadata().getDescriptions() != null) {
      for (final var entry : table.getMetadata().getDescriptions().entrySet()) {
        if (!entry.getValue().isEmpty()) {
          getWriter()
              .processTriple(
                  subject, DCTERMS.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
        }
      }
    }
  }

  void describeColumns(
      final NamespaceMapper namespaces, final Table table, final String columnName) {
    if (table.getMetadata().getTableType() == TableType.DATA) {
      for (final Column column : table.getMetadata().getNonInheritedColumns()) {
        // Exclude the system columns that refer to specific users
        if (column.isSystemAddUpdateByUserColumn()) {
          continue;
        }
        if (columnName == null || columnName.equals(column.getName())) {
          describeColumn(namespaces, column);
        }
      }
    } else {
      // For ontology tables we don't define the columns as predicates.
    }
  }

  private void describeColumn(final NamespaceMapper namespaces, final Column column) {
    final IRI subject = columnIRI(getBaseURL(), column);
    if (column.isReference()) {
      getWriter().processTriple(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      getWriter().processTriple(subject, RDFS.RANGE, tableIRI(getBaseURL(), column.getRefTable()));
    } else {
      var type = column.getColumnType();
      if (type == ColumnType.HYPERLINK || type == ColumnType.HYPERLINK_ARRAY) {
        getWriter().processTriple(subject, RDF.TYPE, OWL.OBJECTPROPERTY);
      } else {
        getWriter().processTriple(subject, RDF.TYPE, OWL.DATATYPEPROPERTY);
        getWriter().processTriple(subject, RDFS.RANGE, getCoreDataType(column).getIri());
      }
    }
    getWriter().processTriple(subject, RDFS.LABEL, Values.literal(column.getName()));
    getWriter().processTriple(subject, RDFS.DOMAIN, tableIRI(getBaseURL(), column.getTable()));
    if (column.getSemantics() != null) {
      for (String columnSemantics : column.getSemantics()) {
        try {
          getWriter()
              .processTriple(
                  subject, RDFS.ISDEFINEDBY, namespaces.map(column.getSchema(), columnSemantics));
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
        getWriter()
            .processTriple(
                subject, DC.DESCRIPTION, Values.literal(entry.getValue(), entry.getKey()));
      }
    }
  }

  @Override
  protected void ontologyRowToRdf(final RdfMapData rdfMapData, final Table table, final Row row) {
    final IRI tableIRI = tableIRI(getBaseURL(), table);
    final IRI subject = rowIRI(getBaseURL(), table, row);

    getWriter().processTriple(subject, RDF.TYPE, BasicIRI.NCIT_CODED_VALUE_DATA_TYPE);
    getWriter().processTriple(subject, RDF.TYPE, OWL.CLASS);
    getWriter().processTriple(subject, RDF.TYPE, SKOS.CONCEPT);
    getWriter().processTriple(subject, RDFS.SUBCLASSOF, tableIRI);
    getWriter().processTriple(subject, SKOS.IN_SCHEME, tableIRI);
    if (row.getString("name") != null) {
      getWriter().processTriple(subject, RDFS.LABEL, Values.literal(row.getString("name")));
      getWriter().processTriple(subject, SKOS.PREF_LABEL, Values.literal(row.getString("name")));
    }
    if (row.getString("label") != null) {
      getWriter().processTriple(subject, RDFS.LABEL, Values.literal(row.getString("label")));
      getWriter().processTriple(subject, SKOS.ALT_LABEL, Values.literal(row.getString("name")));
    }
    if (row.getString("code") != null) {
      getWriter().processTriple(subject, SKOS.NOTATION, Values.literal(row.getString("code")));
    }
    if (row.getString("codesystem") != null) {
      getWriter()
          .processTriple(
              subject,
              BasicIRI.NCIT_CONTROLLED_VOCABULARY,
              Values.literal(row.getString("codesystem")));
    }
    if (row.getString("definition") != null) {
      getWriter()
          .processTriple(subject, SKOS.DEFINITION, Values.literal(row.getString("definition")));
    }
    if (row.getString("ontologyTermURI") != null) {
      getWriter().processTriple(subject, OWL.SAMEAS, Values.iri(row.getString("ontologyTermURI")));
    }
    if (row.getString("parent") != null) {
      Set<Value> parents = retrieveValues(rdfMapData, row, table.getMetadata().getColumn("parent"));
      for (var parent : parents) {
        getWriter().processTriple(subject, RDFS.SUBCLASSOF, parent);
      }
    }
  }

  @Override
  protected void dataRowToRdf(
      final NamespaceMapper namespaces,
      final RdfMapData rdfMapData,
      final Table table,
      final Row row) {
    final IRI tableIRI = tableIRI(getBaseURL(), table);
    final IRI subject = rowIRI(getBaseURL(), table, row);

    getWriter().processTriple(subject, RDF.TYPE, tableIRI);
    getWriter().processTriple(subject, RDF.TYPE, BasicIRI.LD_OBSERVATION);
    getWriter()
        .processTriple(subject, DCAT.ENDPOINT_URL, schemaIRI(getBaseURL(), table.getSchema()));
    getWriter().processTriple(subject, BasicIRI.FDP_METADATAIDENTIFIER, subject);
    if (table.getMetadata().getSemantics() != null) {
      for (String semantics : table.getMetadata().getSemantics()) {
        getWriter().processTriple(subject, RDF.TYPE, namespaces.map(table.getSchema(), semantics));
      }
    }
    getWriter().processTriple(subject, BasicIRI.LD_DATASET_PREDICATE, tableIRI);
    getWriter()
        .processTriple(
            subject, RDFS.LABEL, Values.literal(getLabelForRow(row, table.getMetadata())));
    for (final Column column : table.getMetadata().getColumns()) {
      // Exclude the system columns that refer to specific users
      if (column.isSystemAddUpdateByUserColumn()) {
        continue;
      }
      IRI columnIRI = columnIRI(getBaseURL(), column);

      // Non-default behaviour for non-semantic values to ontology table.
      if (column.getColumnType().equals(ColumnType.ONTOLOGY)
          || column.getColumnType().equals(ColumnType.ONTOLOGY_ARRAY)) {
        retrieveValues(rdfMapData, row, column, ColumnTypeRdfMapper.RdfColumnType.REFERENCE)
            .forEach(value -> getWriter().processTriple(subject, columnIRI, value));
      }

      for (final Value value : retrieveValues(rdfMapData, row, column)) {
        if (column.getSemantics() != null) {
          for (String semantics : column.getSemantics()) {
            getWriter().processTriple(subject, namespaces.map(table.getSchema(), semantics), value);
          }
        }

        switch (column.getColumnType()) {
          case ONTOLOGY, ONTOLOGY_ARRAY -> {} // skipped due to custom behaviour above
          case HYPERLINK, HYPERLINK_ARRAY -> {
            getWriter().processTriple(subject, columnIRI, value);
            var resource = Values.iri(value.stringValue());
            getWriter().processTriple(resource, RDFS.LABEL, Values.literal(value.stringValue()));
          }
          case FILE -> {
            getWriter().processTriple(subject, columnIRI, value);
            generateFileTriples((IRI) value, row, column);
          }
          default -> {
            getWriter().processTriple(subject, columnIRI, value);
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
}
