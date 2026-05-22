package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.molgenis.emx2.TableMetadata;

public interface QueryGenerator {
  SelectQuery generate(TableMetadata tableMetadata);
}
