package org.molgenis.emx2.rdf.generators;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.RdfUtils.*;

import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.BasicIRI;
import org.molgenis.emx2.rdf.ColumnTypeRdfMapper;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.molgenis.emx2.sql.row.resolvers.ResolveComputedValue;

/** A superclass for any class that contains logic of representing data in RDF. */
public abstract class RdfGenerator {

  private final RdfWriter writer;
  private final String baseURL;

  public RdfGenerator(RdfWriter writer, String baseURL) {
    this.writer = writer;
    this.baseURL = formatBaseURL(baseURL);
  }

  protected RdfWriter getWriter() {
    return writer;
  }

  protected String getBaseURL() {
    return baseURL;
  }

  protected List<Row> getRows(final Table table, final PrimaryKey primaryKey) {
    Query query = table.query();

    if (primaryKey != null) {
      query.where(primaryKey.getFilter());
    }

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      String tableName = table.getSchema().getName() + "." + table.getName();
      query.where(f("mg_tableclass", EQUALS, tableName));
    }

    List<Row> rows = query.retrieveRows();
    List<Column> columns = table.getMetadata().getColumns();
    ResolveComputedValue.apply(columns, rows);
    return rows;
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

  protected void generatePrefixes(Schema schema) {
    getWriter().processNamespace(getSchemaNamespace(getBaseURL(), schema.getMetadata()));
    schema
        .getMetadata()
        .getSemanticPrefixes()
        .getAllNamespaces()
        .forEach(getWriter()::processNamespace);
  }

  protected void generatePrefixes(Collection<Schema> schemas) {
    Set<String> processedPrefixes = new HashSet<>();
    Set<String> processedNames = new HashSet<>();

    // if any Schema uses default namespaces, ensure these take priority!
    schemas.stream()
        .filter(schema -> schema.getMetadata().getSemanticPrefixes().isDefaultNamespaces())
        .findFirst()
        .ifPresent(
            schema ->
                schema
                    .getMetadata()
                    .getSemanticPrefixes()
                    .getAllNamespaces()
                    .forEach(
                        namespace -> {
                          processedPrefixes.add(namespace.getPrefix());
                          processedNames.add(namespace.getName());
                          getWriter().processNamespace(namespace);
                        }));

    // Add namespaces specific for each schema.
    schemas.stream()
        .map(schema -> getSchemaNamespace(getBaseURL(), schema.getMetadata()))
        .forEach(
            namespace -> {
              processedPrefixes.add(namespace.getPrefix());
              processedNames.add(namespace.getName());
              getWriter().processNamespace(namespace);
            });

    // Add custom namespaces.
    schemas.stream()
        .flatMap(schema -> schema.getMetadata().getSemanticPrefixes().getAllNamespaces().stream())
        .filter(
            namespace ->
                !processedPrefixes.contains(namespace.getPrefix())
                    && !processedNames.contains(namespace.getName()))
        .forEach(
            namespaces -> {
              processedPrefixes.add(namespaces.getPrefix());
              processedNames.add(namespaces.getName());
              getWriter().processNamespace(namespaces);
            });
  }

  protected void generateCustomRdf(Schema schema) {
    Model model = getCustomRdf(schema);
    if (model != null) {
      // only adds triples, does not transfer defined namespaces!
      model.forEach(writer::processTriple);
    }
  }

  /**
   * @param fileIri the subject to be used (usually generated through {@link ColumnTypeRdfMapper})
   * @param row belonging to the fileIri (f.e. row used as input for {@link
   *     ColumnTypeRdfMapper#retrieveValues(RdfMapData, Row, Column)})
   * @param column belonging to the fileIri (f.e. column used as input for {@link
   *     ColumnTypeRdfMapper#retrieveValues(RdfMapData, Row, Column)})
   */
  protected void generateFileTriples(IRI fileIri, Row row, Column column) {
    getWriter().processTriple(fileIri, RDF.TYPE, BasicIRI.SIO_FILE);
    Literal fileName = Values.literal(row.getString(column.getName() + "_filename"));
    getWriter().processTriple(fileIri, RDFS.LABEL, fileName);
    getWriter().processTriple(fileIri, DCTERMS.TITLE, fileName);
    getWriter()
        .processTriple(
            fileIri,
            DCTERMS.FORMAT,
            Values.iri(
                "http://www.iana.org/assignments/media-types/"
                    + row.getString(column.getName() + "_mimetype")));
  }

  protected void describeRoot() {
    getWriter().processTriple(Values.iri(getBaseURL()), RDF.TYPE, BasicIRI.SIO_DATABASE);
    getWriter().processTriple(Values.iri(getBaseURL()), RDFS.LABEL, Values.literal("EMX2"));
    getWriter()
        .processTriple(
            Values.iri(getBaseURL()),
            DCTERMS.DESCRIPTION,
            Values.literal("MOLGENIS EMX2 database at " + getBaseURL()));
    getWriter().processTriple(Values.iri(getBaseURL()), DCTERMS.CREATOR, BasicIRI.MOLGENIS);
  }
}
