package org.molgenis.emx2.rdf.generators;

import java.util.List;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public abstract class RdfRowsGenerator extends RdfGenerator implements RdfApiGenerator {

  public RdfRowsGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  @Override
  public void generate(Table table, PrimaryKey primaryKey) {
    Set<Table> tables = tablesToDescribe(table.getSchema(), table);
    RdfMapData rdfMapData = new RdfMapData(getBaseURL(), new OntologyIriMapper(tables));
    NamespaceMapper namespaces = new NamespaceMapper(getBaseURL(), table.getSchema());

    generatePrefixes(namespaces.getAllNamespaces(table.getSchema()));
    generateCustomRdf(table.getSchema());
    tables.forEach(i -> processRows(namespaces, rdfMapData, i, primaryKey));
  }

  protected void processRows(
      NamespaceMapper namespaces, RdfMapData rdfMapData, Table table, PrimaryKey primaryKey) {
    List<Row> rows = getRows(table, primaryKey);

    switch (table.getMetadata().getTableType()) {
      case ONTOLOGIES -> rows.forEach(row -> ontologyRowToRdf(rdfMapData, table, row));
      case DATA -> rows.forEach(row -> dataRowToRdf(namespaces, rdfMapData, table, row));
      default -> throw new MolgenisException("Cannot convert unsupported TableType to RDF");
    }
  }

  protected abstract void ontologyRowToRdf(RdfMapData rdfMapData, Table table, Row row);

  protected abstract void dataRowToRdf(
      NamespaceMapper namespaces, RdfMapData rdfMapData, Table table, Row row);
}
