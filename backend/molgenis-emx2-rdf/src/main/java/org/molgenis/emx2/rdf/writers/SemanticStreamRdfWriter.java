package org.molgenis.emx2.rdf.writers;

import static org.molgenis.emx2.rdf.IriGenerator.rowIRI;

import java.io.OutputStream;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.RdfMapData;

public class SemanticStreamRdfWriter extends RdfStreamWriter {
  public SemanticStreamRdfWriter(RdfMapData rdfMapData, OutputStream out, Lang lang) {
    super(rdfMapData, out, lang);
  }

  @Override
  public void write(Database database) {
    database.getSchemaNames().stream().map(database::getSchema).forEach(this::write);
  }

  @Override
  public void write(Schema schema) {
    schema.getTablesSorted().stream().filter(table -> table.getMetadata().getTableType() != TableType.ONTOLOGIES).forEach(this::write);
  }

  @Override
  public void write(Table table) {
    switch (table.getMetadata().getTableType()) {
      case DATA -> getRows(table).forEach(row -> write(table, row));
        // todo: implement ontology behavior
      case ONTOLOGIES -> throw new NotImplementedException();
    }
  }

  @Override
  public void write(Table table, Row row) {
    final Node subject =
        NodeFactory.createURI(rowIRI(getRdfMapData().getBaseURL(), table, row).stringValue());
    table.getMetadata().getColumns().forEach(column -> writeCellTriples(subject, row, column));
  }

  @Override
  public void write(Table table, Row row, Column column) {
    final Node subject =
        NodeFactory.createURI(rowIRI(getRdfMapData().getBaseURL(), table, row).stringValue());
    writeCellTriples(subject, row, column);
  }

  private void writeCellTriples(Node subject, Row row, Column column) {
    createCellTriples(subject, row, column).forEach(this::writeTriple);
  }
}
