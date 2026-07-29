package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.List;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.TableStore;

public class DCATPostProcessor implements PostProcessor {

  private final List<PostProcessor> postProcessors;

  public DCATPostProcessor(SchemaMetadata schema) {
    this.postProcessors =
        List.of(
            // Base id field off of acronym or name
            new CoalesceFieldPostProcessor("Collections", "id", false, "acronym", "name"),
            new CoalesceFieldPostProcessor("Catalogues", "id", false, "acronym", "name"),
            new CoalesceFieldPostProcessor("Organisations", "id", false, "organisation name"),

            // Set type of collections to a hardcoded value
            new StaticFieldPostProcessor(
                "Collections", "type", "http://semanticscience.org/resource/SIO_001067"),
            new StaticFieldPostProcessor(
                "Catalogues", "type", "http://semanticscience.org/resource/SIO_001067"),

            // Resolve semantic uri of ontologies to their designated names
            new OntologyResolver(schema),
            new MissingPkResolver(schema),

            // Drop rows left with an incomplete primary key (e.g. unused Organisations that
            // were never resolved as a reference by another table)
            new MissingPkRowDropper(schema, List.of("Organisations")));
  }

  @Override
  public void process(TableStore tableStore) {
    for (PostProcessor postProcessor : postProcessors) {
      postProcessor.process(tableStore);
    }
  }
}
