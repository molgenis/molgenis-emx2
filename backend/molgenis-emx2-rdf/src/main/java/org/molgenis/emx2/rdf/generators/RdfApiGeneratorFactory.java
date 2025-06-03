package org.molgenis.emx2.rdf.generators;

import java.lang.reflect.InvocationTargetException;
import org.molgenis.emx2.rdf.writers.RdfWriter;

public enum RdfApiGeneratorFactory {
  EMX2(Emx2RdfGenerator.class),
  SEMANTIC(SemanticRdfGenerator.class);

  private final Class<? extends RdfApiPaths> rdfGenerator;

  RdfApiGeneratorFactory(Class<? extends RdfApiPaths> rdfGenerator) {
    this.rdfGenerator = rdfGenerator;
  }

  public RdfApiPaths create(RdfWriter writer, String baseUrl) {
    try {
      return rdfGenerator
          .getConstructor(RdfWriter.class, String.class)
          .newInstance(writer, baseUrl);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      // Any exceptions thrown should purely be due to bugs in this specific code.
      throw new RuntimeException(
          "An error occurred while trying to run GenericSchemaRdfGenerator: " + e);
    }
  }
}
