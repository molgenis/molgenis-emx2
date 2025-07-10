package org.molgenis.emx2.rdf.generators;

import java.util.Collection;
import java.util.List;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.BasicIRI;
import org.molgenis.emx2.rdf.RdfMapData;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;
import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
import org.molgenis.emx2.rdf.writers.RdfWriter;

/**
 * Root RDF generator is separate as it does not follow the general logic of a config defining its
 * behavior but follows its own logic.
 */
public class RootRdfGenerator extends RdfGenerator {
  private final Emx2RdfGenerator emx2RdfGenerator;

  public RootRdfGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
    emx2RdfGenerator = new Emx2RdfGenerator(writer, baseURL);
  }

  public void generate(Collection<Schema> schemas) {
    NamespaceMapper namespaces = new NamespaceMapper(getBaseURL(), schemas);
    List<Table> tables =
        schemas.stream()
            .map(Schema::getTablesSorted)
            .flatMap(Collection::stream)
            .filter(i -> i.getMetadata().getTableType() == TableType.DATA)
            .toList();
    RdfMapData rdfMapData = new RdfMapData(getBaseURL(), new OntologyIriMapper(tables));

    generatePrefixes(namespaces.getAllNamespaces());
    schemas.forEach(this::generateCustomRdf);
    describeRoot();
    schemas.forEach(emx2RdfGenerator::describeSchema);
    tables.forEach(i -> emx2RdfGenerator.describeTable(namespaces, i));
    tables.forEach(i -> emx2RdfGenerator.describeColumns(namespaces, i, null));
    tables.forEach(i -> emx2RdfGenerator.processRows(namespaces, rdfMapData, i, null));
  }

  private void describeRoot() {
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
