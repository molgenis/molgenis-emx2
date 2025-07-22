package org.molgenis.emx2.rdf.generators;

import static org.molgenis.emx2.rdf.ColumnTypeRdfMapper.retrieveValues;
import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;

import java.util.List;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public class SemanticRdfGenerator extends RdfRowsGenerator {
  public SemanticRdfGenerator(RdfWriter writer, String baseURL) {
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
    tables.forEach(i -> processRows(namespaces, rdfMapData, i, null));
  }

  @Override
  public void generate(Table table) {
    generate(table, (PrimaryKey) null);
  }

  /**
   * Does nothing as in semantic mode the column IRIs are non-existing, therefore there is nothing
   * to describe.
   */
  @Override
  public void generate(Table table, Column column) {}

  @Override
  protected void ontologyRowToRdf(RdfMapData rdfMapData, Table table, Row row) {
    final IRI subject = rowIRI(getBaseURL(), table, row);

    if (row.getString("name") != null) {
      getWriter().processTriple(subject, RDFS.LABEL, Values.literal(row.getString("name")));
    }
    if (row.getString("definition") != null) {
      getWriter()
          .processTriple(subject, SKOS.DEFINITION, Values.literal(row.getString("definition")));
    }
    if (row.getString("ontologyTermURI") != null) {
      getWriter().processTriple(subject, OWL.SAMEAS, Values.iri(row.getString("ontologyTermURI")));
    }
  }

  @Override
  protected void dataRowToRdf(
      NamespaceMapper namespaces, RdfMapData rdfMapData, Table table, Row row) {
    final IRI subject = rowIRI(getBaseURL(), table, row);

    if (table.getMetadata().getSemantics() != null) {
      for (String semantics : table.getMetadata().getSemantics()) {
        getWriter().processTriple(subject, RDF.TYPE, namespaces.map(table.getSchema(), semantics));
      }
    }

    for (final Column column : table.getMetadata().getColumns()) {
      if (column.getSemantics() != null) {
        for (final Value value : retrieveValues(rdfMapData, row, column)) {
          for (String semantics : column.getSemantics()) {
            getWriter().processTriple(subject, namespaces.map(table.getSchema(), semantics), value);
          }

          if (column.getColumnType().isFile()) {
            generateFileTriples((IRI) value, row, column);
          }
        }
      }
    }

    getWriter()
        .processTriple(subject, DCTERMS.CREATED, Values.literal(row.getDateTime("mg_insertedOn")));
    getWriter()
        .processTriple(subject, DCTERMS.MODIFIED, Values.literal(row.getDateTime("mg_updatedOn")));
  }
}
