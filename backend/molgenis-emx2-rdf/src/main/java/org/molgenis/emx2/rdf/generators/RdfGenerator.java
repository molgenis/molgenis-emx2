package org.molgenis.emx2.rdf.generators;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;
import static org.molgenis.emx2.rdf.RdfUtils.getCustomRdf;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.writers.RdfWriter;

/** A superclass for any class that contains logic of representing data in RDF. */
public abstract class RdfGenerator {
  private final RdfWriter writer;
  private final String baseURL;

  protected RdfWriter getWriter() {
    return writer;
  }

  protected String getBaseURL() {
    return baseURL;
  }

  public RdfGenerator(RdfWriter writer, String baseURL) {
    this.writer = writer;
    this.baseURL = formatBaseURL(baseURL);
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

  /**
   * Retrieve selected {@link Table} from {@link Schema} including any recursively inherited tables
   * up to the root table (as long as these tables are still part of the same scheme as the {@code
   * tableFilter}).
   */
  protected Set<Table> tablesToDescribe(Schema schema, Table tableFilter) {
    Set<Table> tablesToDescribe = new HashSet<>();
    for (Table currentTable : schema.getTablesSorted()) {
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

  protected void generatePrefixes(Collection<Namespace> namespaces) {
    namespaces.forEach(getWriter()::processNamespace);
  }

  protected void generateCustomRdf(Schema schema) {
    Model model = getCustomRdf(schema);
    if (model != null) {
      // only adds triples, does not transfer defined namespaces!
      model.forEach(writer::processTriple);
    }
  }
}
