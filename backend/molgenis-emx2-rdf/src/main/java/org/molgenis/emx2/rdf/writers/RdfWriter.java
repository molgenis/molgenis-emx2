package org.molgenis.emx2.rdf.writers;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfMapData;

public abstract class RdfWriter implements Closeable {
  private final RdfMapData rdfMapData;

  protected RdfMapData getRdfMapData() {
    return rdfMapData;
  }

  protected RdfWriter(RdfMapData rdfMapData) {
    this.rdfMapData = rdfMapData;
  }

  protected List<Row> getRows(final Table table, final PrimaryKey primaryKey) {
    Query query = table.query();

    if (primaryKey != null) {
      query.where(primaryKey.getFilter());
    }

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      var tableName = table.getSchema().getName() + "." + table.getName();
      query.where(f("mg_tableclass", EQUALS, tableName));
    }
    return query.retrieveRows();
  }

  protected List<Row> getRows(final Table table) {
    return getRows(table, null);
  }

  /** Each combination of a semantic & an object generates a {@link Triple}. */
  protected List<Triple> createCellTriples(Node subject, Row row, Column column) {
    List<Triple> triples = new ArrayList<>();
    // todo: ensure ColumnTypeRdfMapper returns Node instead of Value
    //        for (Node object : ColumnTypeRdfMapper.retrieveValues(rdfMapData, row, column)) {
    //            for (String semantic : column.getSemantics()) {
    //                Node predicate = NodeFactory.createURI(semantic);
    //                triples.add(Triple.create(subject, predicate, object));
    //            }
    //        }
    return triples;
  }

  /** Describe the MOLGENIS instance as a whole. */
  public abstract void writeRoot();

  /** Writes RDF belonging to the table itself (so NOT the individual rows!) */
  public abstract void writeTable(Table table);

  /**
   * Writes all {@link Table}{@code s} in a {@link Schema).
   */
  public abstract void writeRows(Schema schema, boolean includeOntologies);

  public void writeRows(Schema schema) {
    writeRows(schema, false);
  }

  /** Writes all {@link Row}{@code s} in a {@link Table} */
  public abstract void writeRows(Table table);

  /**
   * Writes an individual {@link Row} from a {@link Table}.
   */
  public abstract void writeRows(Table table, Row row);

  /** Writes a single cell ({@link Column} of {@link Row} belonging to a {@link Table}) */
  public abstract void writeCell(Table table, Row row, Column column);
}
