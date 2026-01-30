package org.molgenis.emx2.rdf.generators;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RdfApiGeneratorFactory {
  EMX2(Emx2RdfGenerator.class),
  SEMANTIC(SemanticRdfGenerator.class);

  private static Logger logger = LoggerFactory.getLogger(RdfApiGeneratorFactory.class);

  private final Class<? extends RdfApiGenerator> rdfGenerator;

  RdfApiGeneratorFactory(Class<? extends RdfApiGenerator> rdfGenerator) {
    this.rdfGenerator = rdfGenerator;
  }

  public RdfApiGenerator create(RdfWriter writer, String baseUrl) {
    try {
      return rdfGenerator
          .getConstructor(RdfWriter.class, String.class)
          .newInstance(writer, baseUrl);
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      // Any exceptions thrown should purely be due to bugs in this specific code.
      logger.error(Arrays.toString(e.getStackTrace()));
      throw new RuntimeException(
          "An error occurred while trying to run RdfApiGeneratorFactory: " + e);
    }
  }
}
