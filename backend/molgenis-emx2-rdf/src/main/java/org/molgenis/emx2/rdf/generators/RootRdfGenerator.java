package org.molgenis.emx2.rdf.generators;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.generators.schema.RdfGenerator;
import org.molgenis.emx2.rdf.writers.RdfWriter;

/**
 * Root RDF generator is separate as it does not follow the general logic of a config defining its
 * behavior but follows its own logic.
 */
public class RootRdfGenerator extends RdfGenerator {
  public RootRdfGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  // todo: implement
  public void generate(Schema... schemas) {}
}
