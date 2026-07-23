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
            new CoalesceFieldPostProcessor("Collections", "id", "acronym", "name"),
            new CoalesceFieldPostProcessor("Catalogues", "id", "acronym", "name"),

            // Set type of collections to a hardcoded value
            new StaticFieldPostProcessor(
                "Collections", "type", "http://semanticscience.org/resource/SIO_001067"),
            new StaticFieldPostProcessor(
                "Catalogues", "type", "http://semanticscience.org/resource/SIO_001067"),

            // Resolve semantic uri of ontologies to their designated names
            new OntologyResolver(schema),
            new MissingPkResolver(schema, "Organisations", "Contacts", "Collections", "Catalogues"),

            // Drop rows left with an incomplete primary key (e.g. unused Organisations that
            // were never resolved as a reference by another table)
            new MissingPkRowDropper(schema, List.of("Organisations")),

            // Drop _subject_ fields as those aren't in the schema
            new SubjectColumnCleaner());
  }

  @Override
  public void process(TableStore tableStore) {
    for (PostProcessor postProcessor : postProcessors) {
      postProcessor.process(tableStore);
    }
  }
}
