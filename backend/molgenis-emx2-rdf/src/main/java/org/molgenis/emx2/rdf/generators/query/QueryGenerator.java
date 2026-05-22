package org.molgenis.emx2.rdf.generators.query;

import org.molgenis.emx2.TableMetadata;

public interface QueryGenerator {
  String generate(TableMetadata tableMetadata);
}
