package org.molgenis.emx2.rdf.generators;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.rdf.RdfMapData;
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
    List<Schema> sortedSchemas =
        schemas.stream().sorted(Comparator.comparing(Schema::getName)).toList();

    List<Table> tables =
        sortedSchemas.stream()
            .map(Schema::getTablesSorted)
            .flatMap(Collection::stream)
            .filter(i -> i.getMetadata().getTableType() == TableType.DATA)
            .toList();
    RdfMapData rdfMapData = new RdfMapData(getBaseURL(), new OntologyIriMapper(tables));

    generatePrefixes(sortedSchemas);
    sortedSchemas.forEach(this::generateCustomRdf);
    describeRoot();
    sortedSchemas.forEach(emx2RdfGenerator::describeSchema);
    tables.forEach(emx2RdfGenerator::describeTable);
    tables.forEach(i -> emx2RdfGenerator.describeColumns(i, null));
    tables.forEach(i -> emx2RdfGenerator.processRows(rdfMapData, i, null));
  }
}
